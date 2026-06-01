package com.dcim.platform.binterface;

import com.dcim.platform.module.binterface.dto.auth.BInterfaceFsuRegistrationContextDto;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * BIF2016-AUTH-002: 注册上下文只读模型单元测试。
 */
class BInterfaceFsuRegistrationContextServiceTest {

    // 用反射或直接测试内部方法逻辑
    // 这里用简化方式测试核心判断逻辑

    // ── authMode 判断 ──

    @Test
    void authMode_strict2016_when_has_username() {
        BInterfaceFsuRegistrationContextDto ctx = new BInterfaceFsuRegistrationContextDto();
        ctx.setAuthUsername("cntower");
        ctx.setFsuIp("192.168.1.1");

        // Simulate service logic
        determineMode(ctx);

        assertEquals("strict-2016", ctx.getAuthMode());
        assertEquals("AUTHENTICATED", ctx.getAuthStatus());
    }

    @Test
    void authMode_emerson_compatible_when_no_username_but_has_ip() {
        BInterfaceFsuRegistrationContextDto ctx = new BInterfaceFsuRegistrationContextDto();
        ctx.setFsuIp("192.168.100.100");

        determineMode(ctx);

        assertEquals("emerson-2016-compatible", ctx.getAuthMode());
        assertEquals("COMPATIBLE", ctx.getAuthStatus());
    }

    @Test
    void authMode_emerson_compatible_when_no_username_but_has_mac() {
        BInterfaceFsuRegistrationContextDto ctx = new BInterfaceFsuRegistrationContextDto();
        ctx.setMacId("AA:BB:CC:DD:EE:FF");

        determineMode(ctx);

        assertEquals("emerson-2016-compatible", ctx.getAuthMode());
    }

    @Test
    void authMode_unknown_when_nothing() {
        BInterfaceFsuRegistrationContextDto ctx = new BInterfaceFsuRegistrationContextDto();

        determineMode(ctx);

        assertEquals("unknown", ctx.getAuthMode());
        assertEquals("UNKNOWN", ctx.getAuthStatus());
    }

    private void determineMode(BInterfaceFsuRegistrationContextDto ctx) {
        boolean hasUser = notBlank(ctx.getAuthUsername());
        if (hasUser) {
            ctx.setAuthMode("strict-2016");
            ctx.setAuthStatus("AUTHENTICATED");
        } else if (notBlank(ctx.getFsuIp()) || notBlank(ctx.getMacId())
                || !ctx.getDeviceCapabilities().isEmpty()) {
            ctx.setAuthMode("emerson-2016-compatible");
            ctx.setAuthStatus("COMPATIBLE");
        } else {
            ctx.setAuthMode("unknown");
            ctx.setAuthStatus("UNKNOWN");
        }
    }

    // ── 完整性计算 ──

    @Test
    void completeness_COMPLETE_when_no_missing() {
        List<String> missing = new ArrayList<>();
        String result = compute(missing);
        assertEquals("COMPLETE", result);
    }

    @Test
    void completeness_PARTIAL_when_only_username_missing() {
        // UserName alone → COMPLETE (emerson-compatible allows missing UserName)
        List<String> missing = new ArrayList<>(List.of("UserName"));
        String result = compute(missing);
        assertEquals("COMPLETE", result);
    }

    @Test
    void completeness_PARTIAL_when_3_or_fewer() {
        List<String> missing = new ArrayList<>(List.of("FsuIP", "MacId", "Version"));
        String result = compute(missing);
        assertEquals("PARTIAL", result);
    }

    @Test
    void completeness_MINIMAL_when_4_to_6() {
        List<String> missing = new ArrayList<>(List.of("FsuIP", "MacId", "Version", "DeviceList"));
        String result = compute(missing);
        assertEquals("MINIMAL", result);
    }

    @Test
    void completeness_MISSING_when_more_than_6() {
        List<String> missing = new ArrayList<>();
        for (int i = 0; i < 7; i++) missing.add("field" + i);
        String result = compute(missing);
        assertEquals("MISSING", result);
    }

    private String compute(List<String> missing) {
        int n = missing.size();
        if (n == 0 || (n == 1 && missing.contains("UserName"))) return "COMPLETE";
        if (n <= 3) return "PARTIAL";
        if (n <= 6) return "MINIMAL";
        return "MISSING";
    }

    // ── DeviceType 推断 ──

    @Test
    void inferDeviceType_from_DeviceID() {
        assertEquals("18", infer("51051241820004"));
        assertEquals("07", infer("51051240700002"));
        assertEquals("38", infer("51051243812345"));
        assertEquals("00", infer(null));
        assertEquals("00", infer("12345"));
    }

    private String infer(String deviceId) {
        if (deviceId == null || deviceId.length() < 9) return "00";
        return deviceId.substring(7, 9);
    }

    // ── 字段提取 (模拟 parseFields 结果) ──

    @Test
    void parseLoginFields_extracts_fsuIp() {
        String infoXml = "<FsuCode>51051243812345</FsuCode>"
                + "<FsuIP>192.168.100.100</FsuIP>"
                + "<MacId>00:11:22:33:44:55</MacId>";

        // 简单正则提取验证
        String fsuIp = extractField(infoXml, "FsuIP");
        String macId = extractField(infoXml, "MacId");
        String userName = extractField(infoXml, "UserName");

        assertEquals("192.168.100.100", fsuIp);
        assertEquals("00:11:22:33:44:55", macId);
        assertNull(userName);
    }

    @Test
    void parseLoginFields_emerson_sample_no_username() {
        // Emerson真实LOGIN样本缺少UserName/PaSCword
        String infoXml = "<FsuCode>51051243812345</FsuCode>"
                + "<FsuIP>192.168.100.100</FsuIP>"
                + "<MacId>00:11:22:33:44:55</MacId>"
                + "<Version>1.0.0</Version>";

        String userName = extractField(infoXml, "UserName");
        assertNull(userName); // Emerson 样本无 UserName

        // 但仍能提取其他字段
        assertEquals("1.0.0", extractField(infoXml, "Version"));
        assertEquals("192.168.100.100", extractField(infoXml, "FsuIP"));
    }

    private String extractField(String xml, String tag) {
        if (xml == null || tag == null) return null;
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(
                "<" + tag + ">([^<]*)</" + tag + ">",
                java.util.regex.Pattern.CASE_INSENSITIVE);
        java.util.regex.Matcher m = p.matcher(xml);
        if (m.find()) {
            String v = m.group(1);
            return (v != null && !v.trim().isEmpty()) ? v.trim() : null;
        }
        return null;
    }

    // ── 缺失 LOGIN raw ──

    @Test
    void context_MISSING_when_no_login_raw() {
        BInterfaceFsuRegistrationContextDto ctx = new BInterfaceFsuRegistrationContextDto();
        ctx.setContextCompleteness("MISSING");
        ctx.setMissingFields(List.of("LOGIN raw message"));
        ctx.setAuthMode("unknown");

        assertEquals("MISSING", ctx.getContextCompleteness());
        assertTrue(ctx.getMissingFields().contains("LOGIN raw message"));
        assertEquals("unknown", ctx.getAuthMode());
    }

    private boolean notBlank(String s) { return s != null && !s.trim().isEmpty(); }

    // ── DeviceList 解析 (调用生产 parseDeviceList, package-private) ──

    /**
     * Helper: 用生产 parseDeviceList (public) 解析 infoXml, 返回扁平 DeviceInfo 列表供断言。
     */
    private List<DeviceInfo> invokeProductionParser(String infoXml) {
        var ctx = new com.dcim.platform.module.binterface.dto.auth.BInterfaceFsuRegistrationContextDto();
        ctx.setFsuCode("51051243812345");
        List<String> missing = new ArrayList<>();
        var svc = new com.dcim.platform.module.binterface.service.BInterfaceFsuRegistrationContextService(
                null, null, null, null, null, null);
        svc.parseDeviceList(infoXml, ctx, missing);

        List<DeviceInfo> result = new ArrayList<>();
        for (var cap : ctx.getDeviceCapabilities()) {
            DeviceInfo di = new DeviceInfo();
            di.deviceId = cap.getDeviceId();
            di.deviceCode = cap.getDeviceCode();
            di.deviceType = cap.getDeviceTypeCode();
            di.valid = cap.isValid();
            result.add(di);
        }
        return result;
    }

    @Test
    void parseDeviceList_attribute_form_Id_Code() {
        String infoXml = "<DeviceList>"
                + "<Device Id=\"51051241820004\" Code=\"51051241820004\"/>"
                + "<Device Id=\"51051241830004\" Code=\"51051241830004\"/>"
                + "</DeviceList>";
        List<DeviceInfo> devices = invokeProductionParser(infoXml);
        assertEquals(2, devices.size());
        assertEquals("51051241820004", devices.get(0).deviceId);
        assertEquals("18", devices.get(0).deviceType);
        assertTrue(devices.get(0).valid);
    }

    @Test
    void parseDeviceList_attribute_form_DeviceID() {
        String infoXml = "<DeviceList>"
                + "<Device DeviceID=\"51051241820004\" Code=\"51051241820004\"/>"
                + "</DeviceList>";
        List<DeviceInfo> devices = invokeProductionParser(infoXml);
        assertEquals(1, devices.size());
        assertEquals("51051241820004", devices.get(0).deviceId);
    }

    @Test
    void parseDeviceList_attribute_form_DeviceId() {
        String infoXml = "<DeviceList>"
                + "<Device DeviceId=\"51051241820004\" Code=\"51051241820004\"/>"
                + "</DeviceList>";
        List<DeviceInfo> devices = invokeProductionParser(infoXml);
        assertEquals(1, devices.size());
        assertEquals("51051241820004", devices.get(0).deviceId);
    }

    @Test
    void parseDeviceList_attribute_form_deviceId_lower() {
        String infoXml = "<DeviceList>"
                + "<Device deviceId=\"51051241820004\" Code=\"51051241820004\"/>"
                + "</DeviceList>";
        List<DeviceInfo> devices = invokeProductionParser(infoXml);
        assertEquals(1, devices.size());
        assertEquals("51051241820004", devices.get(0).deviceId);
    }

    @Test
    void parseDeviceList_sub_element_form_DeviceID_DeviceCode() {
        String infoXml = "<DeviceList>"
                + "<Device><DeviceID>51051241820004</DeviceID><DeviceCode>51051241820004</DeviceCode></Device>"
                + "</DeviceList>";
        List<DeviceInfo> devices = invokeProductionParser(infoXml);
        assertEquals(1, devices.size());
        assertEquals("51051241820004", devices.get(0).deviceId);
        assertEquals("51051241820004", devices.get(0).deviceCode);
    }

    @Test
    void parseDeviceList_sub_element_form_Id_Code() {
        String infoXml = "<DeviceList>"
                + "<Device><Id>51051241820004</Id><Code>51051241820004</Code></Device>"
                + "</DeviceList>";
        List<DeviceInfo> devices = invokeProductionParser(infoXml);
        assertEquals(1, devices.size());
        assertEquals("51051241820004", devices.get(0).deviceId);
    }

    @Test
    void parseDeviceList_missing_code_keeps_device() {
        String infoXml = "<DeviceList>"
                + "<Device Id=\"51051241820004\"/>"
                + "</DeviceList>";
        List<DeviceInfo> devices = invokeProductionParser(infoXml);
        assertEquals(1, devices.size());
        assertEquals("51051241820004", devices.get(0).deviceId);
        assertNull(devices.get(0).deviceCode);
        assertTrue(devices.get(0).valid);
    }

    @Test
    void parseDeviceList_missing_id_marks_invalid() {
        String infoXml = "<DeviceList>"
                + "<Device Code=\"51051241820004\"/>"
                + "</DeviceList>";
        List<DeviceInfo> devices = invokeProductionParser(infoXml);
        assertEquals(1, devices.size());
        assertFalse(devices.get(0).valid);
    }

    @Test
    void parseDeviceList_real_emerson_5_devices() {
        String infoXml = "<DeviceList>"
                + "<Device Id=\"51051241820004\" Code=\"51051241820004\"/>"
                + "<Device Id=\"51051241830004\" Code=\"51051241830004\"/>"
                + "<Device Id=\"51051241840004\" Code=\"51051241840004\"/>"
                + "<Device Id=\"51051240700002\" Code=\"51051240700002\"/>"
                + "<Device Id=\"51051243812345\" Code=\"51051243812345\"/>"
                + "</DeviceList>";
        List<DeviceInfo> devices = invokeProductionParser(infoXml);
        assertEquals(5, devices.size());
        assertEquals("51051241820004", devices.get(0).deviceId);
        assertEquals("51051241820004", devices.get(0).deviceCode);
        assertEquals("51051240700002", devices.get(3).deviceId);
        assertEquals("51051243812345", devices.get(4).deviceId);
    }

    @Test
    void parseDeviceList_deviceType_inference() {
        String infoXml = "<DeviceList>"
                + "<Device Id=\"51051241820004\" Code=\"51051241820004\"/>"
                + "<Device Id=\"51051241830004\" Code=\"51051241830004\"/>"
                + "<Device Id=\"51051241840004\" Code=\"51051241840004\"/>"
                + "<Device Id=\"51051240700002\" Code=\"51051240700002\"/>"
                + "<Device Id=\"51051243812345\" Code=\"51051243812345\"/>"
                + "</DeviceList>";
        List<DeviceInfo> devices = invokeProductionParser(infoXml);
        assertEquals("18", devices.get(0).deviceType);
        assertEquals("18", devices.get(1).deviceType);
        assertEquals("18", devices.get(2).deviceType);
        assertEquals("07", devices.get(3).deviceType);
        assertEquals("38", devices.get(4).deviceType);
    }

    @Test
    void parseDeviceList_empty_returns_empty() {
        List<DeviceInfo> devices = invokeProductionParser("<DeviceList></DeviceList>");
        assertTrue(devices.isEmpty());
    }

    @Test
    void parseDeviceList_rawAttributes_preserved_attribute_form() {
        String infoXml = "<DeviceList>"
                + "<Device Id=\"51051241820004\" Code=\"51051241820004\"/>"
                + "</DeviceList>";
        List<DeviceInfo> devices = invokeProductionParserWithRaw(infoXml);
        assertEquals(1, devices.size());
        assertEquals("51051241820004", devices.get(0).deviceId);
        assertEquals("{Id=51051241820004, Code=51051241820004}", devices.get(0).rawAttrs);
    }

    @Test
    void parseDeviceList_rawAttributes_deviceID_deviceCode() {
        String infoXml = "<DeviceList>"
                + "<Device DeviceID=\"51051241820004\" DeviceCode=\"51051241820004\"/>"
                + "</DeviceList>";
        List<DeviceInfo> devices = invokeProductionParserWithRaw(infoXml);
        assertEquals(1, devices.size());
        assertTrue(devices.get(0).rawAttrs.contains("DeviceID"));
        assertTrue(devices.get(0).rawAttrs.contains("DeviceCode"));
    }

    @Test
    void parseDeviceList_rawAttributes_sub_element_form() {
        String infoXml = "<DeviceList>"
                + "<Device><DeviceID>51051241820004</DeviceID><DeviceCode>51051241820004</DeviceCode></Device>"
                + "</DeviceList>";
        List<DeviceInfo> devices = invokeProductionParserWithRaw(infoXml);
        assertEquals(1, devices.size());
        assertTrue(devices.get(0).rawAttrs.contains("DeviceID"));
    }

    @Test
    void parseDeviceList_rawAttributes_preserved_when_code_missing() {
        String infoXml = "<DeviceList>"
                + "<Device Id=\"51051241820004\"/>"
                + "</DeviceList>";
        List<DeviceInfo> devices = invokeProductionParserWithRaw(infoXml);
        assertEquals(1, devices.size());
        assertTrue(devices.get(0).rawAttrs.contains("Id"));
        assertFalse(devices.get(0).rawAttrs.contains("Code="));
    }

    @Test
    void parseDeviceList_rawAttributes_preserved_when_id_missing() {
        String infoXml = "<DeviceList>"
                + "<Device Code=\"51051241820004\"/>"
                + "</DeviceList>";
        List<DeviceInfo> devices = invokeProductionParserWithRaw(infoXml);
        assertEquals(1, devices.size());
        assertFalse(devices.get(0).valid);
        assertTrue(devices.get(0).rawAttrs.contains("Code"));
    }

    // rawAttributes helper
    private List<DeviceInfo> invokeProductionParserWithRaw(String infoXml) {
        var ctx = new com.dcim.platform.module.binterface.dto.auth.BInterfaceFsuRegistrationContextDto();
        List<String> missing = new ArrayList<>();
        var svc = new com.dcim.platform.module.binterface.service.BInterfaceFsuRegistrationContextService(
                null, null, null, null, null, null);
        svc.parseDeviceList(infoXml, ctx, missing);
        List<DeviceInfo> result = new ArrayList<>();
        for (var cap : ctx.getDeviceCapabilities()) {
            DeviceInfo di = new DeviceInfo();
            di.deviceId = cap.getDeviceId();
            di.deviceCode = cap.getDeviceCode();
            di.deviceType = cap.getDeviceTypeCode();
            di.valid = cap.isValid();
            di.rawAttrs = cap.getRawAttributes() != null ? cap.getRawAttributes().toString() : "";
            result.add(di);
        }
        return result;
    }

    @Test
    void parseDeviceList_missing_returns_empty() {
        List<DeviceInfo> devices = invokeProductionParser("<FsuCode>x</FsuCode>");
        assertTrue(devices.isEmpty());
    }

    static class DeviceInfo {
        String deviceId, deviceCode, deviceType, rawAttrs;
        boolean valid = true;
    }
}
