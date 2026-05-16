package com.dcim.platform.module.binterface.xml;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * B接口 xmlData 统一数据模型。
 *
 * 承载各命令 xmlData 的解析结果，支持三种数据结构：
 * 1. 简单字段 — Map<String, String>（如 HEARTBEAT 的 CPU/Memory）
 * 2. 重复结构 — List<Map<String, String>>（如 SEND_DATA 的 Signal[]）
 * 3. 空 — xmlData 为空节点
 *
 * field 解析已内置大小写兼容策略：
 * - FSUCode / FsuCode → 统一按原 XML 标签存储，查找时忽略大小写
 * - DeviceID / DeviceId → 同上
 * - SignalID / SignalId → 同上
 * - PK_Type / PkType / Pk_Type → 同上
 * - ResultCode / resultCode → 同上
 *
 * @see XmlDataParser
 * @see XmlDataBuilder
 */
public class XmlDataModel {

    /** 根元素名称（如 Signal, Alarm, DeviceInfo, FTPConfig, LoginInfo） */
    private String rootName;

    /** 简单字段键值对 */
    private Map<String, String> fields = new LinkedHashMap<>();

    /** 重复结构列表（每个元素是一个字段映射） */
    private List<Map<String, String>> items = new ArrayList<>();

    /** 原始 XML 字符串 */
    private String rawXml;

    /** 解析是否成功 */
    private boolean valid = true;

    /** 解析错误信息 */
    private List<String> parseErrors = new ArrayList<>();

    public XmlDataModel() {
    }

    // ==================== 字段访问（大小写兼容） ====================

    /**
     * 获取字段值（大小写不敏感）。
     *
     * @param name 字段名（任意大小写）
     * @return 字段值，不存在返回 null
     */
    public String getField(String name) {
        if (name == null || fields == null) return null;
        // 精确匹配优先
        String value = fields.get(name);
        if (value != null) return value;
        // 大小写不敏感匹配
        for (Map.Entry<String, String> entry : fields.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(name)) {
                return entry.getValue();
            }
        }
        return null;
    }

    /**
     * 获取字段值，不存在返回默认值。
     */
    public String getField(String name, String defaultValue) {
        String value = getField(name);
        return (value != null) ? value : defaultValue;
    }

    /**
     * 判断是否包含指定字段（大小写不敏感）。
     */
    public boolean hasField(String name) {
        return getField(name) != null;
    }

    /**
     * 添加字段。
     */
    public void setField(String name, String value) {
        if (fields == null) {
            fields = new LinkedHashMap<>();
        }
        fields.put(Objects.requireNonNull(name), value);
    }

    /**
     * 获取所有字段（原始映射，大小写敏感）。
     */
    public Map<String, String> getFields() {
        return fields;
    }

    /**
     * 获取字段数量。
     */
    public int fieldCount() {
        return (fields != null) ? fields.size() : 0;
    }

    // ==================== 重复结构访问 ====================

    /**
     * 添加一个重复项。
     */
    public void addItem(Map<String, String> item) {
        if (items == null) {
            items = new ArrayList<>();
        }
        items.add(item);
    }

    /**
     * 获取所有重复项。
     */
    public List<Map<String, String>> getItems() {
        return items;
    }

    /**
     * 获取重复项数量。
     */
    public int itemCount() {
        return (items != null) ? items.size() : 0;
    }

    /**
     * 获取第一个重复项中指定字段的值。
     */
    public String getFirstItemField(String fieldName) {
        if (items == null || items.isEmpty()) return null;
        Map<String, String> first = items.get(0);
        if (first == null) return null;
        for (Map.Entry<String, String> entry : first.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(fieldName)) {
                return entry.getValue();
            }
        }
        return null;
    }

    /**
     * 从所有重复项中提取指定字段的值列表。
     */
    public List<String> getItemFieldValues(String fieldName) {
        List<String> values = new ArrayList<>();
        if (items == null) return values;
        for (Map<String, String> item : items) {
            if (item == null) continue;
            for (Map.Entry<String, String> entry : item.entrySet()) {
                if (entry.getKey().equalsIgnoreCase(fieldName)) {
                    values.add(entry.getValue());
                    break;
                }
            }
        }
        return values;
    }

    // ==================== 错误处理 ====================

    public void addError(String error) {
        this.valid = false;
        if (parseErrors == null) {
            parseErrors = new ArrayList<>();
        }
        parseErrors.add(error);
    }

    public boolean isValid() {
        return valid;
    }

    public List<String> getParseErrors() {
        return parseErrors;
    }

    public String getFirstError() {
        if (parseErrors == null || parseErrors.isEmpty()) return null;
        return parseErrors.get(0);
    }

    // ==================== 通用属性 ====================

    public String getRootName() {
        return rootName;
    }

    public void setRootName(String rootName) {
        this.rootName = rootName;
    }

    public String getRawXml() {
        return rawXml;
    }

    public void setRawXml(String rawXml) {
        this.rawXml = rawXml;
    }

    /**
     * xmlData 是否为空（无字段、无重复项）。
     */
    public boolean isEmpty() {
        return (fields == null || fields.isEmpty()) && (items == null || items.isEmpty());
    }

    // ==================== 快捷访问 ====================

    public String getFsuCode() {
        return getField("FSUCode");
    }

    public String getDeviceId() {
        return getField("DeviceID");
    }

    public String getSignalId() {
        return getField("SignalID");
    }

    public String getResultCode() {
        return getField("ResultCode");
    }

    /**
     * 获取 ResultCode 整数值，解析失败返回 -1。
     */
    public int getResultCodeInt() {
        String code = getResultCode();
        if (code == null) return -1;
        try {
            return Integer.parseInt(code.trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    @Override
    public String toString() {
        return "XmlDataModel{" +
                "rootName='" + rootName + '\'' +
                ", fieldCount=" + fieldCount() +
                ", itemCount=" + itemCount() +
                ", valid=" + valid +
                ", errors=" + parseErrors +
                '}';
    }
}
