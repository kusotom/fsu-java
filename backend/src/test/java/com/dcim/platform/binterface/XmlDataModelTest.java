package com.dcim.platform.binterface;

import com.dcim.platform.module.binterface.xml.XmlDataBuilder;
import com.dcim.platform.module.binterface.xml.XmlDataModel;
import com.dcim.platform.module.binterface.xml.XmlDataParser;
import com.dcim.platform.module.binterface.xml.XmlDataValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * B接口 xmlData 解析/构造/校验测试。
 *
 * 覆盖：
 * 1. 所有 24 个 xmldata fixture 的解析
 * 2. 平坦字段（HEARTBEAT）、重复结构（Signal[]）、叶子重复（SignalID[]）、
 *    单层包裹（DeviceInfo/FTPConfig/LoginInfo）
 * 3. 从 model 构造 XML ⇒ 再次解析的往返验证
 * 4. 空 xmlData、解析失败、字段混合等边界
 * 5. XmlDataValidator 校验规则
 * 6. 大小写不敏感字段访问
 */
class XmlDataModelTest {

    private XmlDataParser parser;
    private XmlDataBuilder builder;
    private XmlDataValidator validator;

    @BeforeEach
    void setUp() {
        parser = new XmlDataParser();
        builder = new XmlDataBuilder();
        validator = new XmlDataValidator();
    }

    // ==================== 解析测试  ====================

    @Test
    void shouldParseLoginRequest() {
        XmlDataModel model = parseXmlData("login.request.xml");
        assertTrue(model.isValid());
        assertEquals("DeviceInfo", model.getRootName());
        assertEquals(1, model.itemCount());
        assertEquals("中兴", model.getItems().get(0).get("Manufacturer"));
        assertEquals("eStone II", model.getItems().get(0).get("Model"));
        assertEquals("B07D07", model.getItems().get(0).get("FirmwareVersion"));
        assertEquals("HW-2.1", model.getItems().get(0).get("HardwareVersion"));
        assertEquals("AA:BB:CC:DD:EE:01", model.getItems().get(0).get("MacAddr"));
        assertEquals("192.168.1.101", model.getItems().get(0).get("IPAddr"));
    }

    @Test
    void shouldParseHeartbeatRequest() {
        XmlDataModel model = parseXmlData("heartbeat.request.xml");
        assertTrue(model.isValid());
        assertEquals(4, model.fieldCount());
        assertEquals("35", model.getField("CPU"));
        assertEquals("62", model.getField("Memory"));
        assertEquals("42", model.getField("Temperature"));
        assertEquals("3600", model.getField("RunningTime"));
    }

    @Test
    void shouldParseSendDataRequest() {
        XmlDataModel model = parseXmlData("send_data.request.xml");
        assertTrue(model.isValid());
        assertEquals("Signal", model.getRootName());
        assertEquals(5, model.itemCount());

        Map<String, String> first = model.getItems().get(0);
        assertEquals("TEMP-001", first.get("SignalID"));
        assertEquals("25.5", first.get("Value"));
        assertEquals("1", first.get("Quality"));
        assertEquals("NORMAL", first.get("Status"));

        assertEquals("WATER-001", model.getItems().get(4).get("SignalID"));
    }

    @Test
    void shouldParseSendAlarmRequest() {
        XmlDataModel model = parseXmlData("send_alarm.request.xml");
        assertTrue(model.isValid());
        assertEquals("Alarm", model.getRootName());
        assertEquals(1, model.itemCount());
        Map<String, String> alarm = model.getItems().get(0);
        assertEquals("TEMP-001", alarm.get("SignalID"));
        assertEquals("TEMP-HIGH", alarm.get("AlarmCode"));
        assertEquals("机柜温度过高", alarm.get("AlarmName"));
        assertEquals("WARN", alarm.get("AlarmLevel"));
        assertEquals("62.0", alarm.get("AlarmValue"));
        assertEquals("温度超过上限60°C", alarm.get("AlarmDesc"));
        assertEquals("0", alarm.get("AlarmType"));
    }

    @Test
    void shouldParseGetDataRequest() {
        XmlDataModel model = parseXmlData("get_data.request.xml");
        assertTrue(model.isValid());
        assertEquals("SignalID", model.getRootName());
        assertEquals(5, model.itemCount());
        // 叶子重复：每个 item 是 {SignalID: value}
        assertEquals("TEMP-001", model.getItems().get(0).get("SignalID"));
        assertEquals("HUMI-001", model.getItems().get(1).get("SignalID"));
        assertEquals("VOLT-001", model.getItems().get(2).get("SignalID"));
        assertEquals("DOOR-001", model.getItems().get(3).get("SignalID"));
        assertEquals("WATER-001", model.getItems().get(4).get("SignalID"));
    }

    @Test
    void shouldParseGetDataResponse() {
        XmlDataModel model = parseXmlData("get_data.response.xml");
        assertTrue(model.isValid());
        assertEquals("Signal", model.getRootName());
        assertEquals(5, model.itemCount());
        Map<String, String> first = model.getItems().get(0);
        assertEquals("TEMP-001", first.get("SignalID"));
        assertEquals("25.8", first.get("Value"));
        assertEquals("NORMAL", first.get("Status"));
        assertEquals("2026-05-13T10:35:00+08:00", first.get("CollectTime"));
    }

    @Test
    void shouldParseGetThresholdResponse() {
        XmlDataModel model = parseXmlData("get_threshold.response.xml");
        assertTrue(model.isValid());
        assertEquals("Signal", model.getRootName());
        assertEquals(3, model.itemCount());
        Map<String, String> first = model.getItems().get(0);
        assertEquals("TEMP-001", first.get("SignalID"));
        assertEquals("60.0", first.get("AlarmUpper"));
        assertEquals("-5.0", first.get("AlarmLower"));
        assertEquals("70.0", first.get("AlarmUpperUrgent"));
        assertEquals("-10.0", first.get("AlarmLowerUrgent"));
    }

    @Test
    void shouldParseGetThresholdRequest() {
        XmlDataModel model = parseXmlData("get_threshold.request.xml");
        assertTrue(model.isValid());
        assertEquals("SignalID", model.getRootName());
        assertEquals(3, model.itemCount());
        assertEquals("TEMP-001", model.getItems().get(0).get("SignalID"));
        assertEquals("HUMI-001", model.getItems().get(1).get("SignalID"));
        assertEquals("VOLT-001", model.getItems().get(2).get("SignalID"));
    }

    @Test
    void shouldParseSetThresholdRequest() {
        XmlDataModel model = parseXmlData("set_threshold.request.xml");
        assertTrue(model.isValid());
        assertEquals("Signal", model.getRootName());
        assertEquals(1, model.itemCount());
        Map<String, String> signal = model.getItems().get(0);
        assertEquals("TEMP-001", signal.get("SignalID"));
        assertEquals("65.0", signal.get("AlarmUpper"));
        assertEquals("-5.0", signal.get("AlarmLower"));
    }

    @Test
    void shouldParseSetPointRequest() {
        XmlDataModel model = parseXmlData("set_point.request.xml");
        assertTrue(model.isValid());
        assertEquals("Signal", model.getRootName());
        assertEquals(1, model.itemCount());
        Map<String, String> signal = model.getItems().get(0);
        assertEquals("VOLT-001", signal.get("SignalID"));
        assertEquals("230.0", signal.get("SetValue"));
        assertEquals("admin", signal.get("Operator"));
    }

    @Test
    void shouldParseGetFtpResponse() {
        XmlDataModel model = parseXmlData("get_ftp.response.xml");
        assertTrue(model.isValid());
        assertEquals("FTPConfig", model.getRootName());
        assertEquals(1, model.itemCount());
        Map<String, String> cfg = model.getItems().get(0);
        assertEquals("192.168.1.200", cfg.get("Host"));
        assertEquals("21", cfg.get("Port"));
        assertEquals("fsu_ftp", cfg.get("Username"));
        assertEquals("true", cfg.get("PassiveMode"));
        assertEquals("/fsu/images/", cfg.get("BasePath"));
    }

    @Test
    void shouldParseSetFtpRequest() {
        XmlDataModel model = parseXmlData("set_ftp.request.xml");
        assertTrue(model.isValid());
        assertEquals("FTPConfig", model.getRootName());
        assertEquals(1, model.itemCount());
        Map<String, String> cfg = model.getItems().get(0);
        assertEquals("192.168.1.201", cfg.get("Host"));
        assertEquals("21", cfg.get("Port"));
        assertEquals("fsu_ftp_new", cfg.get("Username"));
        assertEquals("new_ftp_pass_2026", cfg.get("Password"));
    }

    @Test
    void shouldParseGetLoginInfoResponse() {
        XmlDataModel model = parseXmlData("get_logininfo.response.xml");
        assertTrue(model.isValid());
        assertEquals("LoginInfo", model.getRootName());
        assertEquals(1, model.itemCount());
        Map<String, String> info = model.getItems().get(0);
        assertEquals("FSU-001", info.get("FSUCode"));
        assertEquals("LOGIN", info.get("LoginStatus"));
        assertEquals("ONLINE", info.get("OnlineStatus"));
        assertEquals("SESSION-FSU-001-20260513-A3B8", info.get("SessionID"));
        assertEquals("2026-05-13T10:30:00+08:00", info.get("LoginTime"));
        assertEquals("2026-05-13T10:40:00+08:00", info.get("LastHeartbeat"));
    }

    // ==================== 空 xmlData 解析测试 ====================

    @Test
    void shouldHandleEmptyXmlData() {
        // xmlData 为空元素或自闭合
        XmlDataModel model = parser.parse("");
        assertTrue(model.isValid());
        assertTrue(model.isEmpty());
        assertEquals(0, model.fieldCount());
        assertEquals(0, model.itemCount());
    }

    @Test
    void shouldHandleNullXmlData() {
        XmlDataModel model = parser.parse(null);
        assertTrue(model.isValid());
        assertTrue(model.isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "login.response.xml",
            "heartbeat.response.xml",
            "send_data.response.xml",
            "send_alarm.response.xml",
            "get_ftp.request.xml",
            "get_logininfo.request.xml",
            "set_ftp.response.xml",
            "set_point.response.xml",
            "set_threshold.response.xml",
            "time_check.request.xml",
            "time_check.response.xml"
    })
    void shouldParseEmptyXmlDataCommands(String fixtureFile) {
        // 这些命令的 xmlData 为空
        XmlDataModel model = parseXmlData(fixtureFile);
        assertTrue(model.isValid(), fixtureFile + " 应解析为有效 model");
        assertTrue(model.isEmpty(), fixtureFile + " 的 xmlData 应为空");
    }

    // ==================== 字段大小写兼容测试 ====================

    @Test
    void shouldGetFieldCaseInsensitive() {
        XmlDataModel model = new XmlDataModel();
        model.setField("FSUCode", "FSU-001");
        model.setField("DeviceID", "DEV-001");
        model.setField("SignalID", "SIG-001");
        model.setField("ResultCode", "0");

        assertEquals("FSU-001", model.getField("FSUCode"));
        assertEquals("FSU-001", model.getField("FSUCODE"));
        assertEquals("FSU-001", model.getField("fsucode"));
        assertEquals("FSU-001", model.getField("FsuCode"));

        assertEquals("DEV-001", model.getField("DeviceID"));
        assertEquals("DEV-001", model.getField("deviceid"));
        assertEquals("DEV-001", model.getField("DeviceId"));

        assertEquals("SIG-001", model.getField("SignalID"));
        assertEquals("SIG-001", model.getField("signalid"));
        assertEquals("SIG-001", model.getField("SignalId"));

        assertEquals("0", model.getField("ResultCode"));
        assertEquals("0", model.getField("resultCode"));
        assertEquals("0", model.getField("RESULTCODE"));
    }

    @Test
    void shouldGetFieldWithDefault() {
        XmlDataModel model = new XmlDataModel();
        assertEquals("default", model.getField("nonexistent", "default"));
        model.setField("key", "value");
        assertEquals("value", model.getField("key", "default"));
    }

    @Test
    void shouldDetectFieldExistence() {
        XmlDataModel model = new XmlDataModel();
        assertFalse(model.hasField("anything"));
        model.setField("FSUCode", "FSU-001");
        assertTrue(model.hasField("FSUCode"));
        assertTrue(model.hasField("fsucode"));
        assertFalse(model.hasField("nonexistent"));
    }

    // ==================== 构造测试 ====================

    @Test
    void shouldBuildHeartbeatRequest() {
        XmlDataModel model = new XmlDataModel();
        model.setField("CPU", "35");
        model.setField("Memory", "62");
        model.setField("Temperature", "42");
        model.setField("RunningTime", "3600");

        String xml = builder.build(model);
        assertNotNull(xml);
        assertTrue(xml.contains("<CPU>35</CPU>"));
        assertTrue(xml.contains("<Memory>62</Memory>"));
        assertTrue(xml.contains("<Temperature>42</Temperature>"));
        assertTrue(xml.contains("<RunningTime>3600</RunningTime>"));
    }

    @Test
    void shouldBuildLoginRequestData() {
        XmlDataModel model = new XmlDataModel();
        model.setRootName("DeviceInfo");
        model.addItem(Map.of(
                "Manufacturer", "中兴",
                "Model", "eStone II",
                "FirmwareVersion", "B07D07",
                "HardwareVersion", "HW-2.1",
                "MacAddr", "AA:BB:CC:DD:EE:01",
                "IPAddr", "192.168.1.101"
        ));

        String xml = builder.build(model);
        assertNotNull(xml);
        assertTrue(xml.contains("<DeviceInfo>"));
        assertTrue(xml.contains("<Manufacturer>中兴</Manufacturer>"));
        assertTrue(xml.contains("</DeviceInfo>"));
        assertTrue(xml.contains("<IPAddr>192.168.1.101</IPAddr>"));
    }

    @Test
    void shouldBuildSendDataWithMultipleSignals() {
        XmlDataModel model = new XmlDataModel();
        model.setRootName("Signal");
        model.addItem(Map.of("SignalID", "TEMP-001", "Value", "25.5", "Quality", "1", "Status", "NORMAL"));
        model.addItem(Map.of("SignalID", "HUMI-001", "Value", "55.0", "Quality", "1", "Status", "NORMAL"));

        String xml = builder.build(model);
        assertNotNull(xml);
        assertTrue(xml.contains("<Signal>"));
        assertTrue(xml.contains("<SignalID>TEMP-001</SignalID>"));
        assertTrue(xml.contains("<SignalID>HUMI-001</SignalID>"));
        assertTrue(xml.contains("</Signal>"));
    }

    @Test
    void shouldBuildGetDataRequest() {
        // 叶子重复模式：SignalID[]
        XmlDataModel model = new XmlDataModel();
        model.setRootName("SignalID");
        model.addItem(Map.of("SignalID", "TEMP-001"));
        model.addItem(Map.of("SignalID", "HUMI-001"));

        String xml = builder.build(model);
        assertNotNull(xml);
        // 应输出叶子元素，无额外包裹
        assertEquals("<SignalID>TEMP-001</SignalID>\n<SignalID>HUMI-001</SignalID>", xml.trim());
    }

    @Test
    void shouldBuildFtpConfig() {
        XmlDataModel model = new XmlDataModel();
        model.setRootName("FTPConfig");
        model.addItem(Map.of(
                "Host", "192.168.1.200",
                "Port", "21",
                "Username", "fsu_ftp",
                "PassiveMode", "true",
                "BasePath", "/fsu/images/"
        ));

        String xml = builder.build(model);
        assertNotNull(xml);
        assertTrue(xml.contains("<FTPConfig>"));
        assertTrue(xml.contains("<Host>192.168.1.200</Host>"));
        assertTrue(xml.contains("<Port>21</Port>"));
        assertTrue(xml.contains("</FTPConfig>"));
    }

    @Test
    void shouldBuildEmptyModel() {
        assertEquals("", builder.build(new XmlDataModel()));
        assertEquals("", builder.build(null));
    }

    // ==================== 往返测试  ====================

    @Test
    void shouldRoundTripHeartbeat() {
        // 解析 → 构造 → 再解析，验证一致性
        XmlDataModel original = parseXmlData("heartbeat.request.xml");
        String builtXml = builder.build(original);
        XmlDataModel reparsed = parser.parse(builtXml);

        assertEquals(original.fieldCount(), reparsed.fieldCount());
        assertEquals("35", reparsed.getField("CPU"));
        assertEquals("62", reparsed.getField("Memory"));
        assertEquals("42", reparsed.getField("Temperature"));
        assertEquals("3600", reparsed.getField("RunningTime"));
    }

    @Test
    void shouldRoundTripSendData() {
        XmlDataModel original = parseXmlData("send_data.request.xml");
        String builtXml = builder.build(original);
        XmlDataModel reparsed = parser.parse(builtXml);

        assertEquals(original.itemCount(), reparsed.itemCount());
        assertEquals("Signal", reparsed.getRootName());
        assertEquals("TEMP-001", reparsed.getItems().get(0).get("SignalID"));
        assertEquals("DRY", reparsed.getItems().get(4).get("Value"));
    }

    @Test
    void shouldRoundTripLogin() {
        XmlDataModel original = parseXmlData("login.request.xml");
        String builtXml = builder.build(original);
        XmlDataModel reparsed = parser.parse(builtXml);

        assertEquals(original.itemCount(), reparsed.itemCount());
        assertEquals("DeviceInfo", reparsed.getRootName());
        assertEquals("中兴", reparsed.getItems().get(0).get("Manufacturer"));
    }

    @Test
    void shouldRoundTripGetDataRequest() {
        XmlDataModel original = parseXmlData("get_data.request.xml");
        String builtXml = builder.build(original);
        XmlDataModel reparsed = parser.parse(builtXml);

        assertEquals(original.itemCount(), reparsed.itemCount());
        assertEquals(5, reparsed.itemCount());
        assertEquals("SignalID", reparsed.getRootName());
        assertEquals("TEMP-001", reparsed.getItems().get(0).get("SignalID"));
    }

    // ==================== 错误处理测试 ====================

    @Test
    void shouldHandleMalformedXmlData() {
        XmlDataModel model = parser.parse("<unclosed>");
        assertFalse(model.isValid());
        assertNotNull(model.getFirstError());
        assertTrue(model.getFirstError().contains("解析失败"));
    }

    @Test
    void shouldHandleRandomText() {
        // 纯文本被 <wrap> 包裹后是有效 DOM，但无子元素 → 空 model
        XmlDataModel model = parser.parse("this is not xml");
        assertTrue(model.isValid());
        assertTrue(model.isEmpty());
    }

    @Test
    void shouldHandleOnlyComment() {
        XmlDataModel model = parser.parse("<!-- just a comment -->");
        assertTrue(model.isValid());
        assertTrue(model.isEmpty());
    }

    // ==================== 快捷方法测试 ====================

    @Test
    void shouldGetFsuCodeShortcut() {
        XmlDataModel model = new XmlDataModel();
        model.setField("FSUCode", "FSU-001");
        assertEquals("FSU-001", model.getFsuCode());
    }

    @Test
    void shouldGetDeviceIdShortcut() {
        XmlDataModel model = new XmlDataModel();
        model.setField("DeviceID", "DEV-001");
        assertEquals("DEV-001", model.getDeviceId());
    }

    @Test
    void shouldGetSignalIdShortcut() {
        XmlDataModel model = new XmlDataModel();
        model.setField("SignalID", "SIG-001");
        assertEquals("SIG-001", model.getSignalId());
    }

    @Test
    void shouldGetResultCodeAsInt() {
        XmlDataModel model = new XmlDataModel();
        model.setField("ResultCode", "0");
        assertEquals(0, model.getResultCodeInt());
        model.setField("ResultCode", "1001");
        assertEquals(1001, model.getResultCodeInt());
        // 非法时返回 -1
        model.setField("ResultCode", "abc");
        assertEquals(-1, model.getResultCodeInt());
        // null 时返回 -1
        assertEquals(-1, new XmlDataModel().getResultCodeInt());
    }

    // ==================== 解析器快捷方法测试 ====================

    @Test
    void shouldParseFieldsOnly() {
        String xml = "<CPU>35</CPU><Memory>62</Memory>";
        Map<String, String> fields = parser.parseFields(xml);
        assertEquals("35", fields.get("CPU"));
        assertEquals("62", fields.get("Memory"));
    }

    @Test
    void shouldParseItemsOnly() {
        String xml = "<Signal><SignalID>TEMP-001</SignalID><Value>25.5</Value></Signal>" +
                     "<Signal><SignalID>HUMI-001</SignalID><Value>55.0</Value></Signal>";
        List<Map<String, String>> items = parser.parseItems(xml);
        assertEquals(2, items.size());
        assertEquals("TEMP-001", items.get(0).get("SignalID"));
        assertEquals("HUMI-001", items.get(1).get("SignalID"));
    }

    // ==================== 校验测试 ====================

    @Test
    void shouldValidateHeartbeatRequest() {
        XmlDataModel model = parseXmlData("heartbeat.request.xml");
        XmlDataValidator.ValidationResult result = validator.validateRequest(model);
        assertTrue(result.isPassed(), "HEARTBEAT 请求应通过校验: " + result.getErrors());
    }

    @Test
    void shouldValidateLoginRequest() {
        XmlDataModel model = parseXmlData("login.request.xml");
        XmlDataValidator.ValidationResult result = validator.validateRequest(model);
        assertTrue(result.isPassed(), "LOGIN 请求应通过校验: " + result.getErrors());
    }

    @Test
    void shouldValidateResponseWithResultCode() {
        XmlDataModel model = parseXmlData("login.response.xml");
        XmlDataValidator.ValidationResult result = validator.validateResponse(model);
        assertTrue(result.isPassed(), "LOGIN 响应应通过校验: " + result.getErrors());
    }

    @Test
    void shouldReportSignalIdEmpty() {
        Map<String, String> badItem = new java.util.LinkedHashMap<>();
        badItem.put("SignalID", "  ");
        XmlDataModel model = new XmlDataModel();
        model.setRootName("Signal");
        model.addItem(badItem);
        XmlDataValidator.ValidationResult result = validator.validate(model);
        assertFalse(result.isPassed());
        assertTrue(result.getFirstError().contains("SignalID"));
    }

    @Test
    void shouldValidateInvalidModel() {
        XmlDataModel model = new XmlDataModel();
        model.addError("解析失败");
        XmlDataValidator.ValidationResult result = validator.validate(model);
        assertFalse(result.isPassed());
    }

    // ==================== Fixture 存在性测试 ====================

    @ParameterizedTest
    @ValueSource(strings = {
            "login.request.xml", "login.response.xml",
            "heartbeat.request.xml", "heartbeat.response.xml",
            "send_data.request.xml", "send_data.response.xml",
            "send_alarm.request.xml", "send_alarm.response.xml",
            "get_data.request.xml", "get_data.response.xml",
            "get_threshold.request.xml", "get_threshold.response.xml",
            "set_threshold.request.xml", "set_threshold.response.xml",
            "set_point.request.xml", "set_point.response.xml",
            "get_ftp.request.xml", "get_ftp.response.xml",
            "set_ftp.request.xml", "set_ftp.response.xml",
            "get_logininfo.request.xml", "get_logininfo.response.xml",
            "time_check.request.xml", "time_check.response.xml"
    })
    void allXmlDataFixturesShouldLoadAndParse(String fixtureFile) {
        assertTrue(BInterfaceFixtureLoader.exists("xmldata/" + fixtureFile),
                "Fixture 文件应存在: " + fixtureFile);
        XmlDataModel model = parseXmlData(fixtureFile);
        assertNotNull(model);
        // 所有 fixture 应有效（它们都是有效的 XML）
        assertTrue(model.isValid(), fixtureFile + " 应解析有效");
    }

    // ==================== 工具方法 ====================

    /**
     * 从 xmldata fixture 加载并提取 xmlData 内容，解析为 XmlDataModel。
     */
    private XmlDataModel parseXmlData(String fixtureFile) {
        String fullXml = BInterfaceFixtureLoader.loadXmlData(fixtureFile);
        String xmlDataContent = extractXmlDataContent(fullXml);
        return parser.parse(xmlDataContent);
    }

    /**
     * 从完整 Request/Response XML 中提取 xmlData 元素内的 XML 内容。
     */
    private String extractXmlDataContent(String fullXml) {
        // 处理 <xmlData>content</xmlData>
        Pattern pattern = Pattern.compile("<xmlData[^>]*>(.*?)</xmlData>", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(fullXml);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        // 处理 <xmlData/> 自闭合
        if (fullXml.contains("<xmlData/>") || fullXml.contains("<xmlData />")) {
            return "";
        }
        return "";
    }
}
