package com.dcim.platform.module.binterface.xml;

import com.dcim.platform.module.binterface.compat.BInterfaceFieldAliasMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * B接口 xmlData 解析器。
 *
 * 将 xmlData XML 字符串解析为 {@link XmlDataModel}，支持以下结构：
 *
 * <ol>
 *   <li>空数据 — {@code <xmlData/>} → 空 model</li>
 *   <li>平坦字段 — CPU/Memory/Temperature → {@code fields} 映射</li>
 *   <li>重复结构 — Signal[] / Alarm[] → {@code items} 列表</li>
 *   <li>叶子重复 — SignalID[] → 每个叶子为单键条目加入 {@code items}</li>
 *   <li>单层包裹 — DeviceInfo / FTPConfig / LoginInfo → 以 {@code rootName}+单个 item 表示</li>
 * </ol>
 *
 * <b>解析策略：</b>
 * <ul>
 *   <li>无子元素 → 空 model</li>
 *   <li>单一子元素且该元素有子元素 → 包裹模式（items 含 1 条目，rootName=包裹标签名）</li>
 *   <li>多子元素且标签名全部相同 → 重复模式（items，rootName=标签名）</li>
 *   <li>多子元素且标签名混合 → 平坦字段模式（fields）</li>
 * </ul>
 *
 * 注意：本解析器只处理 xmlData 内部 XML，不处理 SOAP 封装。
 *
 * @see XmlDataModel
 * @see XmlDataBuilder
 */
@Component
public class XmlDataParser {

    private static final Logger log = LoggerFactory.getLogger(XmlDataParser.class);

    private final DocumentBuilderFactory docFactory;

    public XmlDataParser() {
        this.docFactory = DocumentBuilderFactory.newInstance();
        docFactory.setNamespaceAware(false);
    }

    /**
     * 解析 xmlData XML 字符串。
     *
     * @param xmlDataContent xmlData 内部的 XML 内容（由 SoapMessageHandler
     *                       serializeElementContent 提取所得），可为 null 或空字符串
     * @return 解析后的 XmlDataModel，不会返回 null
     */
    public XmlDataModel parse(String xmlDataContent) {
        XmlDataModel model = new XmlDataModel();
        model.setRawXml(xmlDataContent);

        if (xmlDataContent == null || xmlDataContent.trim().isEmpty()) {
            return model;
        }

        try {
            DocumentBuilder builder = docFactory.newDocumentBuilder();
            // 用合成根包装以支持多顶层元素（如 HEARTBEAT 的 CPU/Memory 等）
            Document doc = builder.parse(new InputSource(new StringReader("<wrap>" + xmlDataContent + "</wrap>")));
            Element wrap = doc.getDocumentElement();
            List<Element> children = getChildElements(wrap);

            if (children.isEmpty()) {
                return model;
            }

            if (children.size() == 1) {
                handleSingleChild(model, children.get(0));
            } else {
                handleMultipleChildren(model, children);
            }

        } catch (Exception e) {
            log.warn("xmlData 解析失败: {}", e.getMessage());
            model.addError("xmlData 解析失败: " + e.getMessage());
        }

        return model;
    }

    /**
     * 快捷方法：从字符串解析并只取 fields 映射。
     */
    public Map<String, String> parseFields(String xmlDataContent) {
        return parse(xmlDataContent).getFields();
    }

    /**
     * 快捷方法：从字符串解析并只取 items 列表。
     */
    public List<Map<String, String>> parseItems(String xmlDataContent) {
        return parse(xmlDataContent).getItems();
    }

    // ==================== 字段别名归一化 (BIF-P4-011) ====================

    /**
     * 将 Map 中的旧字段名归一化为 2024 标准字段名。
     * 如果同时存在标准字段和别名字段，标准字段优先（后出现的标准字段覆盖别名）。
     */
    public Map<String, String> normalizeFields(Map<String, String> fields) {
        if (fields == null || fields.isEmpty()) return fields;
        Map<String, String> result = new LinkedHashMap<>();
        for (Map.Entry<String, String> e : fields.entrySet()) {
            String standardKey = BInterfaceFieldAliasMapper.toStandard(e.getKey());
            boolean isStandardKey = standardKey.equals(e.getKey());
            if (result.containsKey(standardKey)) {
                if (isStandardKey) {
                    // 标准字段覆盖别名值
                    result.put(standardKey, e.getValue());
                }
                // 别名→已存在，跳过
            } else {
                result.put(standardKey, e.getValue());
            }
        }
        return result;
    }

    /**
     * 将 items 列表中每个 item 的 key 归一化为标准字段名。
     */
    public List<Map<String, String>> normalizeItems(List<Map<String, String>> items) {
        if (items == null || items.isEmpty()) return items;
        List<Map<String, String>> result = new ArrayList<>();
        for (Map<String, String> item : items) {
            result.add(normalizeFields(item));
        }
        return result;
    }

    // ==================== 内部解析策略 ====================

    private void handleSingleChild(XmlDataModel model, Element child) {
        List<Element> subChildren = getChildElements(child);

        if (subChildren.isEmpty()) {
            // 单叶子元素 → 作为字段存入
            model.setField(child.getTagName(), child.getTextContent().trim());
        } else {
            // 包裹模式：DeviceInfo / FTPConfig / LoginInfo 等
            model.setRootName(child.getTagName());
            model.addItem(buildItemMap(child));
        }
    }

    private void handleMultipleChildren(XmlDataModel model, List<Element> children) {
        if (allSameTag(children)) {
            // 重复模式：Signal[]、Alarm[]、SignalID[]
            model.setRootName(children.get(0).getTagName());
            for (Element child : children) {
                List<Element> subChildren = getChildElements(child);
                if (subChildren.isEmpty()) {
                    // 叶子重复项：SignalID[]
                    Map<String, String> item = new LinkedHashMap<>();
                    item.put(child.getTagName(), child.getTextContent().trim());
                    model.addItem(item);
                } else {
                    // 结构化重复项：Signal{SignalID,Value,...}
                    model.addItem(buildItemMap(child));
                }
            }
        } else {
            // 平坦字段模式：HEARTBEAT 的 CPU/Memory/Temperature/RunningTime
            for (Element child : children) {
                // 只取叶子元素作为字段，忽略非叶子
                List<Element> subChildren = getChildElements(child);
                if (subChildren.isEmpty()) {
                    model.setField(child.getTagName(), child.getTextContent().trim());
                } else {
                    // 混合模式下忽略复杂子元素（避免丢失数据，但标记警告）
                    log.debug("平坦字段模式下忽略非叶子元素: {}", child.getTagName());
                }
            }
        }
    }

    // ==================== 工具方法 ====================

    private Map<String, String> buildItemMap(Element element) {
        Map<String, String> map = new LinkedHashMap<>();
        for (Element child : getChildElements(element)) {
            map.put(child.getTagName(), child.getTextContent().trim());
        }
        return map;
    }

    private List<Element> getChildElements(Element parent) {
        List<Element> elements = new ArrayList<>();
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                elements.add((Element) node);
            }
        }
        return elements;
    }

    private boolean allSameTag(List<Element> elements) {
        if (elements.isEmpty()) return true;
        String firstTag = elements.get(0).getTagName();
        for (int i = 1; i < elements.size(); i++) {
            if (!elements.get(i).getTagName().equals(firstTag)) {
                return false;
            }
        }
        return true;
    }
}
