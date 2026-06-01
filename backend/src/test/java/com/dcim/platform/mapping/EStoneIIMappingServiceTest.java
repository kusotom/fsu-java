package com.dcim.platform.mapping;

import com.dcim.platform.module.mapping.entity.DeviceSignalCandidateEntity;
import com.dcim.platform.module.mapping.entity.EStoneIIEventDictionaryEntity;
import com.dcim.platform.module.mapping.entity.EStoneIISignalDictionaryEntity;
import com.dcim.platform.module.mapping.repository.DeviceSignalCandidateRepository;
import com.dcim.platform.module.mapping.repository.EStoneIIControlReferenceRepository;
import com.dcim.platform.module.mapping.repository.EStoneIIEventDictionaryRepository;
import com.dcim.platform.module.mapping.repository.EStoneIISignalDictionaryRepository;
import com.dcim.platform.module.mapping.service.EStoneIIDictionaryImportService;
import com.dcim.platform.module.mapping.service.EStoneIIMappingResult;
import com.dcim.platform.module.mapping.service.EStoneIIMappingService;
import com.dcim.platform.module.mapping.service.EStoneIITemplateDictionaryParser;
import com.dcim.platform.module.mapping.service.BInterface2016StandardSignalIndexService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;

import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class EStoneIIMappingServiceTest {

    private final Map<String, EStoneIISignalDictionaryEntity> signals = new HashMap<>();
    private final Map<String, EStoneIIEventDictionaryEntity> events = new HashMap<>();
    private final Map<String, EStoneIIEventDictionaryEntity> eventsBySignal = new HashMap<>();
    private final Map<String, DeviceSignalCandidateEntity> candidates = new HashMap<>();
    private EStoneIIMappingService mappingService;

    @BeforeEach
    void setUp() {
        EStoneIITemplateDictionaryParser parser =
                new EStoneIITemplateDictionaryParser(new DefaultResourceLoader());
        EStoneIISignalDictionaryRepository signalRepository = signalRepository();
        EStoneIIEventDictionaryRepository eventRepository = eventRepository();
        EStoneIIControlReferenceRepository controlRepository = repositoryProxy(EStoneIIControlReferenceRepository.class);
        DeviceSignalCandidateRepository candidateRepository = candidateRepository();
        EStoneIIDictionaryImportService importService = new EStoneIIDictionaryImportService(
                parser, signalRepository, eventRepository, controlRepository, candidateRepository);
        mappingService = new EStoneIIMappingService(importService, signalRepository, eventRepository,
                candidateRepository, parser,
                new BInterface2016StandardSignalIndexService(new DefaultResourceLoader()));
    }

    @Test
    void resolvesRealtimeByDeviceAndSignalCandidateBeforeDictionaryFallback() {
        signals.put("510000250", signal("510000250", "烟感", "", "2", "0:无告警;1:有告警", "HIGH", "BOTH"));
        candidates.put(candidateKey("51051243812345", "51051241820004", "510000250"),
                candidate("51051241820004", "Smoke", "510000250", "HIGH", "MAPPED_CANDIDATE"));

        EStoneIIMappingResult result = mappingService.resolveRealtime(
                "51051243812345", "51051241820004", null, "510000250", "510000250", "1");

        assertEquals("烟感", result.signalName());
        assertEquals("有告警", result.valueMeaning());
        assertEquals("MAPPED_CANDIDATE", result.mappingStatus());
        assertEquals("HIGH", result.mappingConfidence());
        assertEquals("DI", result.signalType());
        assertEquals("device_signal_candidate", result.source());
    }

    @Test
    void resolvesRealtimeBySignalDictionaryAsTemplateOnlyFallback() {
        signals.put("510000211", signal("510000211", "I2C温度", "℃", "1", "", "HIGH", "BOTH"));

        EStoneIIMappingResult result = mappingService.resolveRealtime(
                "51051243812345", "unknown-device", null, "510000211", "510000211", "25.5");

        assertEquals("I2C温度", result.signalName());
        assertEquals("℃", result.unit());
        assertEquals("TEMPLATE_ONLY", result.mappingStatus());
        assertTrue(result.needRealDataConfirm());
        assertEquals("signal_dictionary_fallback", result.source());
    }

    @Test
    void unresolvedRealtimeSignalBecomesUnmapped() {
        EStoneIIMappingResult result = mappingService.resolveRealtime(
                "51051243812345", "unknown-device", null, "999999999", "999999999", "1");

        assertEquals("UNMAPPED", result.mappingStatus());
        assertEquals("UNKNOWN_SIGNAL_ID", result.reason());
    }

    @Test
    void resolvesRealtimeByBInterface2016StandardSignalIndexFallback() {
        EStoneIIMappingResult result = mappingService.resolveRealtime(
                "51051243812345", null, null, "0407102001", "0407102001", "54.2000");

        assertEquals("总电压", result.signalName());
        assertEquals("V", result.unit());
        assertEquals("MAPPED_CANDIDATE", result.mappingStatus());
        assertEquals("LOW", result.mappingConfidence());
        assertEquals("BINTERFACE_2016_STANDARD", result.templateVariant());
        assertEquals("BINTERFACE_2016_STANDARD", result.source());
        assertTrue(result.needRealDataConfirm());
        assertEquals("54.2000", result.value());
        assertNull(result.reason());
    }

    @Test
    void preservesZeroValueAndUnknownUnitForBInterface2016StandardFallback() {
        EStoneIIMappingResult result = mappingService.resolveRealtime(
                "51051243812345", null, null, "0407107001", "0407107001", "0.0000");

        assertEquals("后半组电压", result.signalName());
        assertEquals("", result.unit());
        assertEquals("0.0000", result.value());
        assertEquals("MAPPED_CANDIDATE", result.mappingStatus());
        assertEquals("LOW", result.mappingConfidence());
        assertEquals("BINTERFACE_2016_STANDARD", result.source());
    }

    @Test
    void eStoneIISignalDictionaryKeepsPriorityOverBInterface2016Fallback() {
        signals.put("0407102001", signal("0407102001", "eStoneII优先点", "X", "1", "", "HIGH", "BOTH"));

        EStoneIIMappingResult result = mappingService.resolveRealtime(
                "51051243812345", null, null, "0407102001", "0407102001", "54.2000");

        assertEquals("eStoneII优先点", result.signalName());
        assertEquals("X", result.unit());
        assertEquals("TEMPLATE_ONLY", result.mappingStatus());
        assertEquals("signal_dictionary_fallback", result.source());
    }

    @Test
    void resolvesAlarmByEventDictionaryAndSignalDictionary() {
        signals.put("510000250", signal("510000250", "烟感", "", "2", "0:无告警;1:有告警", "HIGH", "BOTH"));
        EStoneIIEventDictionaryEntity event = event("510000250", "烟感告警", "510000250", "一级告警");
        events.put("510000250", event);
        eventsBySignal.put("510000250", event);
        candidates.put(candidateKey("51051243812345", "51051241820004", "510000250"),
                candidate("51051241820004", "Smoke", "510000250", "HIGH", "MAPPED_CANDIDATE"));

        EStoneIIMappingResult result = mappingService.resolveAlarm(
                "51051243812345", "51051241820004", null, "510000250", "510000250", "510000250", "1");

        assertEquals("烟感", result.signalName());
        assertEquals("烟感告警", result.eventName());
        assertEquals("一级告警", result.eventSeverity());
        assertEquals("有告警", result.valueMeaning());
        assertEquals("MAPPED_CANDIDATE", result.mappingStatus());
    }

    @Test
    void preservesUnknownExplicitAlarmEventIdAsUnmappedReason() {
        signals.put("510000250", signal("510000250", "烟感", "", "2", "0:无告警;1:有告警", "HIGH", "BOTH"));
        EStoneIIEventDictionaryEntity event = event("510000250", "烟感告警", "510000250", "一级告警");
        eventsBySignal.put("510000250", event);

        EStoneIIMappingResult result = mappingService.resolveAlarm(
                "51051243812345", "51051241820004", null, "510000250", "510000250", "UNKNOWN-EVENT", "1");

        assertEquals("烟感", result.signalName());
        assertEquals("烟感告警", result.eventName());
        assertEquals("UNKNOWN-EVENT", result.eventId());
        assertEquals("UNKNOWN_EVENT_ID", result.reason());
        assertTrue(result.mapped());
    }

    @SuppressWarnings("unchecked")
    private EStoneIISignalDictionaryRepository signalRepository() {
        return (EStoneIISignalDictionaryRepository) Proxy.newProxyInstance(
                EStoneIISignalDictionaryRepository.class.getClassLoader(),
                new Class<?>[]{EStoneIISignalDictionaryRepository.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "findBySignalId" -> Optional.ofNullable(signals.get((String) args[0]));
                    case "count" -> 39L;
                    case "save" -> args[0];
                    case "toString" -> "SignalRepoProxy";
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }

    private EStoneIIEventDictionaryRepository eventRepository() {
        return (EStoneIIEventDictionaryRepository) Proxy.newProxyInstance(
                EStoneIIEventDictionaryRepository.class.getClassLoader(),
                new Class<?>[]{EStoneIIEventDictionaryRepository.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "findByEventId" -> Optional.ofNullable(events.get((String) args[0]));
                    case "findFirstBySignalId" -> Optional.ofNullable(eventsBySignal.get((String) args[0]));
                    case "count" -> 13L;
                    case "save" -> args[0];
                    case "toString" -> "EventRepoProxy";
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }

    private DeviceSignalCandidateRepository candidateRepository() {
        return (DeviceSignalCandidateRepository) Proxy.newProxyInstance(
                DeviceSignalCandidateRepository.class.getClassLoader(),
                new Class<?>[]{DeviceSignalCandidateRepository.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "findFirstByFsuIdAndDeviceIdAndSignalId" ->
                            Optional.ofNullable(candidates.get(candidateKey((String) args[0], (String) args[1], (String) args[2])));
                    case "findFirstByFsuIdAndDeviceCodeAndSignalId",
                         "findFirstByDeviceIdAndSignalId",
                         "findFirstByDeviceCodeAndSignalId",
                         "findFirstByFsuIdAndDeviceIdAndSignalIdAndMappingSource" -> Optional.empty();
                    case "count" -> 23L;
                    case "save" -> args[0];
                    case "toString" -> "CandidateRepoProxy";
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }

    @SuppressWarnings("unchecked")
    private static <T> T repositoryProxy(Class<T> type) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, (proxy, method, args) -> switch (method.getName()) {
            case "count" -> 4L;
            case "save" -> args[0];
            case "toString" -> type.getSimpleName() + "Proxy";
            case "hashCode" -> System.identityHashCode(proxy);
            case "equals" -> proxy == args[0];
            default -> Optional.empty();
        });
    }

    private static String candidateKey(String fsuId, String deviceId, String signalId) {
        return fsuId + "|" + deviceId + "|" + signalId;
    }

    private static EStoneIISignalDictionaryEntity signal(String id, String name, String unit,
                                                         String category, String meanings,
                                                         String confidence, String variant) {
        EStoneIISignalDictionaryEntity signal = new EStoneIISignalDictionaryEntity();
        signal.setSignalId(id);
        signal.setSignalName(name);
        signal.setUnit(unit);
        signal.setSignalCategory(category);
        signal.setSignalType(category);
        signal.setSignalMeaningsRaw(meanings);
        signal.setMappingConfidence(confidence);
        signal.setTemplateVariant(variant);
        signal.setNeedRealDataConfirm(true);
        signal.setDerived(false);
        return signal;
    }

    private static EStoneIIEventDictionaryEntity event(String id, String name, String signalId, String severity) {
        EStoneIIEventDictionaryEntity event = new EStoneIIEventDictionaryEntity();
        event.setEventId(id);
        event.setEventName(name);
        event.setSignalId(signalId);
        event.setMeanings(name);
        event.setEventSeverity(severity);
        event.setTemplateVariant("BOTH");
        event.setNeedRealDataConfirm(true);
        return event;
    }

    private static DeviceSignalCandidateEntity candidate(String deviceId, String deviceName, String signalId,
                                                         String confidence, String status) {
        DeviceSignalCandidateEntity candidate = new DeviceSignalCandidateEntity();
        candidate.setFsuId("51051243812345");
        candidate.setDeviceId(deviceId);
        candidate.setDeviceCode(deviceId);
        candidate.setDeviceName(deviceName);
        candidate.setSignalId(signalId);
        candidate.setSignalName(deviceName);
        candidate.setConfidence(confidence);
        candidate.setTemplateVariant("BOTH");
        candidate.setNeedRealDataConfirm(true);
        candidate.setVerifiedByRealData(false);
        candidate.setMappingStatus(status);
        candidate.setMappingSource("eStoneII_IO_标准码表.xlsx:DeviceSignalCandidate");
        return candidate;
    }
}
