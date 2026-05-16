package com.dcim.platform.binterface;

import com.dcim.platform.module.binterface.xml.XmlDataParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * XmlDataParser 字段别名归一化集成测试 (BIF-P4-011)。
 */
class XmlDataParserFieldAliasTest {

    private XmlDataParser parser;

    @BeforeEach
    void setUp() {
        parser = new XmlDataParser();
    }

    // ==================== fields 归一化 ====================

    @Test
    void fsuCodeShouldNormalizeToSuid() {
        Map<String, String> fields = mapOf("FSUCode", "51051243812345");
        Map<String, String> result = parser.normalizeFields(fields);
        assertEquals("51051243812345", result.get("SUID"));
        assertFalse(result.containsKey("FSUCode"), "旧字段名应被替换");
    }

    @Test
    void fsuCodeVarCaseShouldNormalize() {
        Map<String, String> fields = mapOf("FsuCode", "TEST");
        Map<String, String> result = parser.normalizeFields(fields);
        assertEquals("TEST", result.get("SUID"));
    }

    @Test
    void signalIdShouldNormalizeToSpid() {
        Map<String, String> fields = mapOf("SignalID", "TEMP-001");
        Map<String, String> result = parser.normalizeFields(fields);
        assertEquals("TEMP-001", result.get("SPID"));
    }

    @Test
    void pointIdShouldNormalizeToSpid() {
        Map<String, String> fields = mapOf("PointID", "P001");
        Map<String, String> result = parser.normalizeFields(fields);
        assertEquals("P001", result.get("SPID"));
    }

    @Test
    void deviceIdShouldStayDeviceId() {
        Map<String, String> fields = mapOf("DeviceID", "DEV-001");
        Map<String, String> result = parser.normalizeFields(fields);
        assertEquals("DEV-001", result.get("DeviceID"));
    }

    // ==================== 标准字段优先 ====================

    @Test
    void standardFieldShouldWinOverAlias() {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("FSUCode", "old-value");
        fields.put("SUID", "standard-value");
        Map<String, String> result = parser.normalizeFields(fields);
        assertEquals("standard-value", result.get("SUID"), "标准字段应优先");
    }

    @Test
    void standardSpidShouldWinOverSignalId() {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("SignalID", "old-spid");
        fields.put("SPID", "standard-spid");
        Map<String, String> result = parser.normalizeFields(fields);
        assertEquals("standard-spid", result.get("SPID"));
    }

    // ==================== 混合字段 ====================

    @Test
    void shouldNormalizeMixedFields() {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("FSUCode", "FSU-001");
        fields.put("SignalID", "TEMP-001");
        fields.put("DeviceID", "DEV-001");
        fields.put("FSUIP", "192.168.1.1");
        Map<String, String> result = parser.normalizeFields(fields);
        assertEquals("FSU-001", result.get("SUID"));
        assertEquals("TEMP-001", result.get("SPID"));
        assertEquals("DEV-001", result.get("DeviceID"));
        assertEquals("192.168.1.1", result.get("SUIP"));
    }

    // ==================== 未知字段保留 ====================

    @Test
    void unknownFieldShouldBePreserved() {
        Map<String, String> fields = mapOf("SomeCustomField", "custom-value");
        Map<String, String> result = parser.normalizeFields(fields);
        assertEquals("custom-value", result.get("SomeCustomField"), "未知字段不应丢失");
    }

    // ==================== items 归一化 ====================

    @Test
    void shouldNormalizeSignalItemsFields() {
        Map<String, String> item = new LinkedHashMap<>();
        item.put("SignalID", "TEMP-001");
        item.put("Value", "25.5");
        item.put("FSUCode", "FSU-X");
        List<Map<String, String>> items = List.of(item);
        List<Map<String, String>> result = parser.normalizeItems(items);
        assertEquals(1, result.size());
        assertEquals("TEMP-001", result.get(0).get("SPID"));
        assertEquals("25.5", result.get(0).get("Value"));
        assertEquals("FSU-X", result.get(0).get("SUID"));
    }

    // ==================== null/empty ====================

    @Test
    void nullFieldsShouldReturnNull() {
        assertNull(parser.normalizeFields(null));
    }

    @Test
    void emptyFieldsShouldReturnEmpty() {
        Map<String, String> result = parser.normalizeFields(Map.of());
        assertTrue(result.isEmpty());
    }

    @Test
    void nullItemsShouldReturnNull() {
        assertNull(parser.normalizeItems(null));
    }

    // ==================== 辅助 ====================

    private Map<String, String> mapOf(String k, String v) {
        Map<String, String> m = new LinkedHashMap<>();
        m.put(k, v);
        return m;
    }
}
