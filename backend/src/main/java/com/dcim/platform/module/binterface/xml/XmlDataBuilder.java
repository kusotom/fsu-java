package com.dcim.platform.module.binterface.xml;

import java.util.Map;

/**
 * B接口 xmlData 构造器。
 *
 * 将 {@link XmlDataModel} 构造为 xmlData XML 字符串，支持：
 * <ul>
 *   <li>平坦字段模式 — 每个 field 直接输出为叶子元素</li>
 *   <li>重复结构模式 — 每个 item 以 {@code <rootName>} 包裹输出</li>
 *   <li>叶子重复模式 — item 的键与 rootName 相同时输出为叶子元素</li>
 *   <li>空 model → 空字符串</li>
 * </ul>
 *
 * <b>示例：</b>
 * <pre>
 *   fields: {CPU→35, Memory→62}             → {@code <CPU>35</CPU><Memory>62</Memory>}
 *   rootName=Signal, items=[{SignalID→T,...}] → {@code <Signal><SignalID>T...</SignalID></Signal>}
 *   rootName=SignalID, items=[{SignalID→T}]   → {@code <SignalID>T</SignalID>}
 * </pre>
 *
 * @see XmlDataModel
 * @see XmlDataParser
 */
public class XmlDataBuilder {

    /**
     * 将 XmlDataModel 构造为 xmlData XML 字符串。
     *
     * @param model XmlDataModel，null 或空 model 返回空字符串
     * @return xmlData XML 字符串（不含 xmlData 根标签），不会返回 null
     */
    public String build(XmlDataModel model) {
        if (model == null || model.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();

        if (model.itemCount() > 0) {
            buildItems(sb, model);
        } else if (model.fieldCount() > 0) {
            buildFields(sb, model);
        }

        return sb.toString();
    }

    // ==================== Items 构造 ====================

    private void buildItems(StringBuilder sb, XmlDataModel model) {
        String rootName = model.getRootName();

        for (Map<String, String> item : model.getItems()) {
            if (item == null || item.isEmpty()) continue;

            if (isLeafItem(rootName, item)) {
                // 叶子重复：<SignalID>TEMP-001</SignalID>
                String value = item.get(rootName);
                if (value != null) {
                    sb.append('<').append(rootName).append('>')
                            .append(escapeXml(value))
                            .append("</").append(rootName).append(">\n");
                }
            } else if (rootName != null) {
                // 结构化重复：<Signal><SignalID>...</SignalID>...</Signal>
                sb.append('<').append(rootName).append(">\n");
                appendItemFields(sb, item);
                sb.append("</").append(rootName).append(">\n");
            } else {
                // 无 rootName 兜底：直接输出字段
                appendItemFields(sb, item);
            }
        }
    }

    private boolean isLeafItem(String rootName, Map<String, String> item) {
        return rootName != null && item.size() == 1 && item.containsKey(rootName);
    }

    private void appendItemFields(StringBuilder sb, Map<String, String> item) {
        for (Map.Entry<String, String> entry : item.entrySet()) {
            sb.append('<').append(entry.getKey()).append('>')
                    .append(escapeXml(entry.getValue()))
                    .append("</").append(entry.getKey()).append(">\n");
        }
    }

    // ==================== Fields 构造 ====================

    private void buildFields(StringBuilder sb, XmlDataModel model) {
        for (Map.Entry<String, String> entry : model.getFields().entrySet()) {
            if (entry.getValue() == null) continue;
            sb.append('<').append(entry.getKey()).append('>')
                    .append(escapeXml(entry.getValue()))
                    .append("</").append(entry.getKey()).append(">\n");
        }
    }

    // ==================== XML 转义 ====================

    private String escapeXml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}
