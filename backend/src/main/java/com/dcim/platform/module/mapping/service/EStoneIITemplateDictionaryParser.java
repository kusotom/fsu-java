package com.dcim.platform.module.mapping.service;

import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Component
public class EStoneIITemplateDictionaryParser {

    public static final String NO_MIDPOINT_SOURCE = "EquipmentTemplateeStoneII-IO-无中间点电压.xml";
    public static final String WITH_MIDPOINT_SOURCE = "EquipmentTemplateeStoneII-IO-有中间点电压.xml";
    public static final String DEFAULT_SOURCE = "EquipmentTemplateeStoneII-IO.xml";
    public static final String CANDIDATE_SOURCE = "eStoneII_IO_标准码表.xlsx:DeviceSignalCandidate";
    public static final String DEFAULT_FSU_ID = "51051243812345";

    private static final String NO_MIDPOINT_RESOURCE =
            "classpath:dictionary/estoneii/equipment-template-estoneii-io-no-midpoint.xml";
    private static final String WITH_MIDPOINT_RESOURCE =
            "classpath:dictionary/estoneii/equipment-template-estoneii-io-with-midpoint.xml";
    private static final String DEFAULT_RESOURCE =
            "classpath:dictionary/estoneii/equipment-template-estoneii-io-default.xml";
    private static final String CANDIDATE_RESOURCE =
            "classpath:dictionary/estoneii/device-signal-candidates.csv";

    private final ResourceLoader resourceLoader;

    public EStoneIITemplateDictionaryParser(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    public TemplateDictionaryData parseAll() {
        TemplateFile noMid = parseTemplate(NO_MIDPOINT_RESOURCE, NO_MIDPOINT_SOURCE);
        TemplateFile withMid = parseTemplate(WITH_MIDPOINT_RESOURCE, WITH_MIDPOINT_SOURCE);
        TemplateFile defaultFile = parseTemplate(DEFAULT_RESOURCE, DEFAULT_SOURCE);
        List<CandidateDefinition> candidates = parseCandidates();
        Map<String, CandidateDefinition> candidateBySignal = new HashMap<>();
        for (CandidateDefinition c : candidates) {
            candidateBySignal.putIfAbsent(c.signalId(), c);
        }

        List<SignalDefinition> signals = mergeSignals(noMid, withMid, defaultFile, candidateBySignal);
        List<EventDefinition> events = mergeEvents(noMid, withMid, defaultFile);
        List<ControlDefinition> controls = mergeControls(noMid, withMid, defaultFile);
        return new TemplateDictionaryData(signals, events, controls, candidates);
    }

    public Map<String, String> parseMeanings(String raw) {
        Map<String, String> meanings = new LinkedHashMap<>();
        if (raw == null || raw.isBlank()) return meanings;
        for (String part : raw.split(";")) {
            if (part == null || part.isBlank()) continue;
            int idx = part.indexOf(':');
            if (idx <= 0) continue;
            meanings.put(part.substring(0, idx).trim(), part.substring(idx + 1).trim());
        }
        return meanings;
    }

    public String meaningOf(String raw, String value) {
        if (value == null) return null;
        return parseMeanings(raw).get(value.trim());
    }

    public String meaningsToJson(String raw) {
        Map<String, String> meanings = parseMeanings(raw);
        if (meanings.isEmpty()) return "{}";
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (var e : meanings.entrySet()) {
            if (!first) sb.append(',');
            sb.append('"').append(jsonEscape(e.getKey())).append("\":\"")
                    .append(jsonEscape(e.getValue())).append('"');
            first = false;
        }
        return sb.append('}').toString();
    }

    private List<SignalDefinition> mergeSignals(TemplateFile noMid, TemplateFile withMid, TemplateFile defaultFile,
                                                Map<String, CandidateDefinition> candidateBySignal) {
        Set<String> ids = new TreeSet<>(numericAwareComparator());
        ids.addAll(noMid.signals().keySet());
        ids.addAll(withMid.signals().keySet());
        List<SignalDefinition> result = new ArrayList<>();
        for (String id : ids) {
            Map<String, String> attrs = withMid.signals().getOrDefault(id, noMid.signals().get(id));
            boolean inNo = noMid.signals().containsKey(id);
            boolean inWith = withMid.signals().containsKey(id);
            boolean inDefault = defaultFile.signals().containsKey(id);
            CandidateDefinition candidate = candidateBySignal.get(id);
            String confidence = candidate != null ? candidate.confidence() : "TEMPLATE_ONLY";
            boolean needConfirm = candidate == null || candidate.needRealDataConfirm();
            result.add(new SignalDefinition(
                    id, attr(attrs, "SignalName"), attr(attrs, "SignalCategory"), attr(attrs, "SignalType"),
                    attr(attrs, "ChannelNo"), attr(attrs, "ChannelType"), attr(attrs, "Unit"),
                    attr(attrs, "BaseTypeId"), attr(attrs, "SignalMeanings"), attr(attrs, "Expression"),
                    parseInteger(attr(attrs, "DisplayIndex")), variant(inNo, inWith), confidence, needConfirm,
                    hasExpression(attr(attrs, "Expression")), parseBoolean(attr(attrs, "Enable")),
                    parseBoolean(attr(attrs, "Visible")), sourceFiles(inNo, inWith, inDefault)));
        }
        return result;
    }

    private List<EventDefinition> mergeEvents(TemplateFile noMid, TemplateFile withMid, TemplateFile defaultFile) {
        Set<String> ids = new TreeSet<>(numericAwareComparator());
        ids.addAll(noMid.events().keySet());
        ids.addAll(withMid.events().keySet());
        List<EventDefinition> result = new ArrayList<>();
        for (String id : ids) {
            EventRaw raw = withMid.events().getOrDefault(id, noMid.events().get(id));
            boolean inNo = noMid.events().containsKey(id);
            boolean inWith = withMid.events().containsKey(id);
            boolean inDefault = defaultFile.events().containsKey(id);
            Map<String, String> a = raw.eventAttrs();
            Map<String, String> c = raw.conditionAttrs();
            result.add(new EventDefinition(id, attr(a, "EventName"), attr(a, "SignalId"),
                    attr(a, "StartExpression"), attr(a, "EventCategory"), attr(c, "EventSeverity"),
                    attr(c, "StartOperation"), attr(c, "StartCompareValue"), attr(c, "Meanings"),
                    attr(c, "BaseTypeId"), variant(inNo, inWith), true, sourceFiles(inNo, inWith, inDefault),
                    parseBoolean(attr(a, "Enable")), parseBoolean(attr(a, "Visible"))));
        }
        return result;
    }

    private List<ControlDefinition> mergeControls(TemplateFile noMid, TemplateFile withMid, TemplateFile defaultFile) {
        Set<String> ids = new TreeSet<>(numericAwareComparator());
        ids.addAll(noMid.controls().keySet());
        ids.addAll(withMid.controls().keySet());
        List<ControlDefinition> result = new ArrayList<>();
        for (String id : ids) {
            Map<String, String> attrs = withMid.controls().getOrDefault(id, noMid.controls().get(id));
            boolean inNo = noMid.controls().containsKey(id);
            boolean inWith = withMid.controls().containsKey(id);
            boolean inDefault = defaultFile.controls().containsKey(id);
            result.add(new ControlDefinition(id, attr(attrs, "ControlName"), attr(attrs, "ControlCategory"),
                    attr(attrs, "ControlSeverity"), attr(attrs, "CmdToken"), attr(attrs, "SignalId"),
                    attr(attrs, "ControlMeanings"), sourceFiles(inNo, inWith, inDefault)));
        }
        return result;
    }

    private TemplateFile parseTemplate(String resourceLocation, String sourceFile) {
        try {
            Resource resource = resourceLoader.getResource(resourceLocation);
            try (InputStream in = resource.getInputStream()) {
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
                factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
                factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
                Document doc = factory.newDocumentBuilder().parse(in);
                doc.getDocumentElement().normalize();

                Map<String, Map<String, String>> signals = parseElements(doc, "Signal", "SignalId");
                Map<String, EventRaw> events = parseEvents(doc);
                Map<String, Map<String, String>> controls = parseElements(doc, "Control", "ControlId");
                return new TemplateFile(sourceFile, signals, events, controls);
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse eStoneII template: " + resourceLocation, e);
        }
    }

    private Map<String, Map<String, String>> parseElements(Document doc, String tag, String idAttr) {
        Map<String, Map<String, String>> result = new LinkedHashMap<>();
        NodeList nodes = doc.getElementsByTagName(tag);
        for (int i = 0; i < nodes.getLength(); i++) {
            Element e = (Element) nodes.item(i);
            String id = e.getAttribute(idAttr).trim();
            if (!id.isEmpty()) result.put(id, attributes(e));
        }
        return result;
    }

    private Map<String, EventRaw> parseEvents(Document doc) {
        Map<String, EventRaw> result = new LinkedHashMap<>();
        NodeList nodes = doc.getElementsByTagName("Event");
        for (int i = 0; i < nodes.getLength(); i++) {
            Element e = (Element) nodes.item(i);
            String id = e.getAttribute("EventId").trim();
            if (id.isEmpty()) continue;
            Map<String, String> condition = Map.of();
            NodeList conditions = e.getElementsByTagName("EventCondition");
            if (conditions.getLength() > 0) condition = attributes((Element) conditions.item(0));
            result.put(id, new EventRaw(attributes(e), condition));
        }
        return result;
    }

    private List<CandidateDefinition> parseCandidates() {
        try {
            Resource resource = resourceLoader.getResource(CANDIDATE_RESOURCE);
            List<CandidateDefinition> result = new ArrayList<>();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
                String header = reader.readLine();
                if (header == null) return List.of();
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.isBlank()) continue;
                    String[] v = parseCsv(line);
                    String deviceName = value(v, 0);
                    String deviceId = value(v, 1);
                    String signalId = value(v, 5);
                    String confidence = value(v, 7);
                    boolean needConfirm = "YES".equalsIgnoreCase(value(v, 10));
                    result.add(new CandidateDefinition(DEFAULT_FSU_ID, deviceId, deviceId, deviceName,
                            value(v, 2), value(v, 3), value(v, 4), signalId, value(v, 6),
                            confidence, normalizeVariant(value(v, 8)), needConfirm, CANDIDATE_SOURCE,
                            "HIGH".equals(confidence) ? "MAPPED_CANDIDATE" : "PENDING_REAL_DATA"));
                }
            }
            return result;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse eStoneII candidate CSV", e);
        }
    }

    private Map<String, String> attributes(Element e) {
        Map<String, String> attrs = new LinkedHashMap<>();
        for (int i = 0; i < e.getAttributes().getLength(); i++) {
            var n = e.getAttributes().item(i);
            attrs.put(n.getNodeName(), normalizeBlank(n.getNodeValue()));
        }
        return attrs;
    }

    private String[] parseCsv(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean quote = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == '"') quote = !quote;
            else if (ch == ',' && !quote) {
                values.add(cur.toString());
                cur.setLength(0);
            } else cur.append(ch);
        }
        values.add(cur.toString());
        return values.toArray(String[]::new);
    }

    private String value(String[] values, int index) {
        if (index < 0 || index >= values.length) return "";
        return normalizeBlank(values[index]);
    }

    private static String attr(Map<String, String> map, String key) {
        return normalizeBlank(map != null ? map.get(key) : null);
    }

    private static String normalizeBlank(String value) {
        if (value == null) return "";
        String trimmed = value.trim();
        return trimmed.isBlank() ? "" : trimmed;
    }

    private static boolean hasExpression(String expression) {
        return expression != null && !expression.isBlank();
    }

    private static boolean parseBoolean(String value) {
        return "true".equalsIgnoreCase(value);
    }

    private static Integer parseInteger(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static String variant(boolean inNo, boolean inWith) {
        if (inNo && inWith) return "BOTH";
        if (inWith) return "WITH_MIDPOINT";
        if (inNo) return "NO_MIDPOINT";
        return "UNKNOWN";
    }

    private static String normalizeVariant(String value) {
        if (value == null || value.isBlank()) return "BOTH";
        if (value.contains("NO_MIDPOINT") && value.contains("WITH_MIDPOINT")) return "BOTH";
        if (value.contains("WITH_MIDPOINT")) return "WITH_MIDPOINT";
        if (value.contains("NO_MIDPOINT")) return "NO_MIDPOINT";
        return value;
    }

    private static String sourceFiles(boolean inNo, boolean inWith, boolean inDefault) {
        List<String> files = new ArrayList<>();
        if (inNo) files.add(NO_MIDPOINT_SOURCE);
        if (inWith) files.add(WITH_MIDPOINT_SOURCE);
        if (inDefault) files.add(DEFAULT_SOURCE);
        return String.join(";", files);
    }

    private static Comparator<String> numericAwareComparator() {
        return (a, b) -> {
            try {
                return Long.compare(Long.parseLong(a), Long.parseLong(b));
            } catch (NumberFormatException ignored) {
                return a.compareTo(b);
            }
        };
    }

    private static String jsonEscape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private record TemplateFile(String sourceFile,
                                Map<String, Map<String, String>> signals,
                                Map<String, EventRaw> events,
                                Map<String, Map<String, String>> controls) {}

    private record EventRaw(Map<String, String> eventAttrs, Map<String, String> conditionAttrs) {}

    public record TemplateDictionaryData(List<SignalDefinition> signals,
                                         List<EventDefinition> events,
                                         List<ControlDefinition> controls,
                                         List<CandidateDefinition> candidates) {}

    public record SignalDefinition(String signalId, String signalName, String signalCategory, String signalType,
                                   String channelNo, String channelType, String unit, String baseTypeId,
                                   String signalMeaningsRaw, String expression, Integer displayIndex,
                                   String templateVariant, String mappingConfidence, boolean needRealDataConfirm,
                                   boolean derived, boolean enable, boolean visible, String sourceFile) {}

    public record EventDefinition(String eventId, String eventName, String signalId, String startExpression,
                                  String eventCategory, String eventSeverity, String startOperation,
                                  String startCompareValue, String meanings, String baseTypeId,
                                  String templateVariant, boolean needRealDataConfirm, String sourceFile,
                                  boolean enable, boolean visible) {}

    public record ControlDefinition(String commandId, String commandName, String commandCategory,
                                    String commandSeverity, String cmdToken, String signalId,
                                    String meanings, String sourceFile) {}

    public record CandidateDefinition(String fsuId, String deviceId, String deviceCode, String deviceName,
                                      String towerCategoryId, String towerDeviceType, String emersonDeviceTypeId,
                                      String signalId, String signalName, String confidence, String templateVariant,
                                      boolean needRealDataConfirm, String mappingSource, String mappingStatus) {}
}
