package com.dcim.platform.binterface;

import com.dcim.platform.module.binterface.model.BInterfaceMessage;
import com.dcim.platform.module.binterface.model.BInterfacePkType;
import com.dcim.platform.module.binterface.soap.SoapMessageHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * B接口 SOAP 报文解析/构造测试。
 *
 * 覆盖：
 * 1. 解析 SOAP Envelope → 提取 PK_Type / Info / xmlData
 * 2. 根据 PK_Type / Info / xmlData → 构造 SOAP Envelope
 * 3. 各命令类型的 SOAP 报文解析
 * 4. SOAP Fault 解析
 * 5. 异常/边界情况
 */
class SoapMessageHandlerTest {

    private SoapMessageHandler handler;

    @BeforeEach
    void setUp() {
        handler = new SoapMessageHandler();
    }

    // ==================== SOAP 解析测试 ====================

    @Test
    void shouldParseLoginRequest() {
        String xml = BInterfaceFixtureLoader.loadSoap("sc_service/login.request.xml");
        BInterfaceMessage msg = handler.parse(xml);
        assertEquals(BInterfacePkType.LOGIN, msg.getPkType());
        assertTrue(msg.getInfo().contains("FSU-001"));
        assertTrue(msg.getInfo().contains("FSUCode"));
        assertTrue(msg.getXmlData().contains("DeviceInfo"));
    }

    @Test
    void shouldParseHeartbeatRequest() {
        String xml = BInterfaceFixtureLoader.loadSoap("sc_service/heartbeat.request.xml");
        BInterfaceMessage msg = handler.parse(xml);
        assertEquals(BInterfacePkType.HEARTBEAT, msg.getPkType());
        assertTrue(msg.getInfo().contains("FSUCode"));
        assertTrue(msg.getXmlData().contains("CPU"));
    }

    @Test
    void shouldParseSendDataRequest() {
        String xml = BInterfaceFixtureLoader.loadSoap("sc_service/send_data.request.xml");
        BInterfaceMessage msg = handler.parse(xml);
        assertEquals(BInterfacePkType.SEND_DATA, msg.getPkType());
        assertTrue(msg.getInfo().contains("CollectTime"));
        assertTrue(msg.getXmlData().contains("Signal"));
    }

    @Test
    void shouldParseSendAlarmRequest() {
        String xml = BInterfaceFixtureLoader.loadSoap("sc_service/send_alarm.request.xml");
        BInterfaceMessage msg = handler.parse(xml);
        assertEquals(BInterfacePkType.SEND_ALARM, msg.getPkType());
        assertTrue(msg.getInfo().contains("AlarmTime"));
        assertTrue(msg.getXmlData().contains("Alarm"));
    }

    @Test
    void shouldParseGetDataRequest() {
        String xml = BInterfaceFixtureLoader.loadSoap("fsu_service/get_data.request.xml");
        BInterfaceMessage msg = handler.parse(xml);
        assertEquals(BInterfacePkType.GET_DATA, msg.getPkType());
        assertTrue(msg.getInfo().contains("RequestTime"));
        assertTrue(msg.getXmlData().contains("SignalID"));
    }

    @Test
    void shouldParseGetThresholdRequest() {
        String xml = BInterfaceFixtureLoader.loadSoap("fsu_service/get_threshold.request.xml");
        BInterfaceMessage msg = handler.parse(xml);
        assertEquals(BInterfacePkType.GET_THRESHOLD, msg.getPkType());
        assertTrue(msg.getXmlData().contains("SignalID"));
    }

    @Test
    void shouldParseSetThresholdRequest() {
        String xml = BInterfaceFixtureLoader.loadSoap("fsu_service/set_threshold.request.xml");
        BInterfaceMessage msg = handler.parse(xml);
        assertEquals(BInterfacePkType.SET_THRESHOLD, msg.getPkType());
        assertTrue(msg.getXmlData().contains("AlarmUpper"));
    }

    @Test
    void shouldParseSetPointRequest() {
        String xml = BInterfaceFixtureLoader.loadSoap("fsu_service/set_point.request.xml");
        BInterfaceMessage msg = handler.parse(xml);
        assertEquals(BInterfacePkType.SET_POINT, msg.getPkType());
        assertTrue(msg.getXmlData().contains("SetValue"));
    }

    @Test
    void shouldParseGetFtpRequest() {
        String xml = BInterfaceFixtureLoader.loadSoap("fsu_service/get_ftp.request.xml");
        BInterfaceMessage msg = handler.parse(xml);
        assertEquals(BInterfacePkType.GET_FTP, msg.getPkType());
        assertTrue(msg.getInfo().contains("FileType"));
    }

    @Test
    void shouldParseTimeCheckRequest() {
        String xml = BInterfaceFixtureLoader.loadSoap("fsu_service/time_check.request.xml");
        BInterfaceMessage msg = handler.parse(xml);
        assertEquals(BInterfacePkType.TIME_CHECK, msg.getPkType());
        assertTrue(msg.getInfo().contains("StandardTime"));
    }

    @Test
    void shouldParseLoginResponse() {
        String xml = BInterfaceFixtureLoader.loadSoap("sc_service/login.response.xml");
        BInterfaceMessage msg = handler.parse(xml);
        assertEquals(BInterfacePkType.LOGIN, msg.getPkType());
        assertTrue(msg.getInfo().contains("SessionID"));
        assertTrue(msg.getInfo().contains("ResultCode"));
    }

    @Test
    void shouldParseGetDataResponse() {
        String xml = BInterfaceFixtureLoader.loadSoap("fsu_service/get_data.response.xml");
        BInterfaceMessage msg = handler.parse(xml);
        assertEquals(BInterfacePkType.GET_DATA, msg.getPkType());
        assertTrue(msg.getInfo().contains("Count"));
        assertTrue(msg.getXmlData().contains("Signal"));
    }

    // ==================== SOAP 构造测试 ====================

    @Test
    void shouldBuildLoginResponse() {
        String soap = handler.buildResponse("LOGIN",
                "<ResultCode>0</ResultCode><SessionID>TEST-SESSION</SessionID>", null);
        assertNotNull(soap);
        assertTrue(soap.contains("Envelope"));
        assertTrue(soap.contains("LOGIN"));
        assertTrue(soap.contains("TEST-SESSION"));
    }

    @Test
    void shouldBuildHeartbeatResponse() {
        String soap = handler.buildResponse("HEARTBEAT",
                "<ResultCode>0</ResultCode><ServerTime>2026-05-13T10:31:05+08:00</ServerTime>", null);
        assertNotNull(soap);
        assertTrue(soap.contains("HEARTBEAT"));
        assertTrue(soap.contains("<ResultCode>0</ResultCode>"));
    }

    @Test
    void shouldBuildSendDataResponse() {
        String soap = handler.buildResponse("SEND_DATA",
                "<ResultCode>0</ResultCode><Count>5</Count>", null);
        assertNotNull(soap);
        assertTrue(soap.contains("SEND_DATA"));
        assertTrue(soap.contains("<Count>5</Count>"));
    }

    @Test
    void shouldBuildGetDataRequest() {
        String soap = handler.buildRequest("GET_DATA",
                "<FSUCode>FSU-001</FSUCode><SessionID>S1</SessionID>",
                "<SignalID>TEMP-001</SignalID>");
        assertNotNull(soap);
        assertTrue(soap.contains("GET_DATA"));
        assertTrue(soap.contains("TEMP-001"));
    }

    @Test
    void shouldBuildFault() {
        String fault = handler.buildFault("soap:Server", "内部错误",
                "<error><code>5001</code></error>");
        assertNotNull(fault);
        assertTrue(fault.contains("Fault"));
        assertTrue(fault.contains("soap:Server") || fault.contains("Server"));
        assertTrue(fault.contains("内部错误"));
        assertTrue(fault.contains("5001"));
    }

    // ==================== 异常/边界测试 ====================

    @Test
    void shouldHandleEmptyBody() {
        String xml = BInterfaceFixtureLoader.loadInvalid("empty_body.request.xml");
        assertThrows(RuntimeException.class, () -> handler.parse(xml));
    }

    @Test
    void shouldHandleMalformedXml() {
        String xml = BInterfaceFixtureLoader.loadInvalid("malformed_xml.request.xml");
        assertThrows(Exception.class, () -> handler.parse(xml));
    }

    @Test
    void shouldHandleSoapFault() {
        String xml = BInterfaceFixtureLoader.loadInvalid("soap_fault.response.xml");
        BInterfaceMessage msg = handler.parse(xml);
        assertEquals(BInterfacePkType.UNKNOWN, msg.getPkType());
        assertNotNull(msg.getInfo());
        assertTrue(msg.getInfo().contains("Fault"));
    }

    @Test
    void shouldHandleUnknownPkType() {
        String xml = BInterfaceFixtureLoader.loadInvalid("unknown_msg_type.request.xml");
        BInterfaceMessage msg = handler.parse(xml);
        assertEquals(BInterfacePkType.UNKNOWN, msg.getPkType());
    }

    @Test
    void shouldParseEmptyXmlData() {
        String xml = BInterfaceFixtureLoader.loadSoap("fsu_service/time_check.request.xml");
        BInterfaceMessage msg = handler.parse(xml);
        assertEquals(BInterfacePkType.TIME_CHECK, msg.getPkType());
        // time_check.request.xml 的 xmlData 为空，应为 null 或空
        assertTrue(msg.getXmlData() == null || msg.getXmlData().isEmpty());
    }

    @Test
    void shouldBuildAndParseRoundTrip() {
        String originalInfo = "<ResultCode>0</ResultCode><SessionID>ROUND-TRIP</SessionID>";
        String soap = handler.buildResponse("LOGIN", originalInfo, null);
        BInterfaceMessage parsed = handler.parse(soap);
        assertEquals(BInterfacePkType.LOGIN, parsed.getPkType());
        assertTrue(parsed.getInfo().contains("ROUND-TRIP"));
    }

    // ==================== Fixture 加载验证 ====================

    @ParameterizedTest
    @ValueSource(strings = {
            "soap/sc_service/login.request.xml",
            "soap/sc_service/login.response.xml",
            "soap/sc_service/heartbeat.request.xml",
            "soap/sc_service/heartbeat.response.xml",
            "soap/sc_service/send_data.request.xml",
            "soap/sc_service/send_data.response.xml",
            "soap/sc_service/send_alarm.request.xml",
            "soap/sc_service/send_alarm.response.xml",
            "soap/fsu_service/get_data.request.xml",
            "soap/fsu_service/get_data.response.xml",
            "soap/fsu_service/get_threshold.request.xml",
            "soap/fsu_service/get_threshold.response.xml",
            "soap/fsu_service/set_threshold.request.xml",
            "soap/fsu_service/set_threshold.response.xml",
            "soap/fsu_service/set_point.request.xml",
            "soap/fsu_service/set_point.response.xml",
            "soap/fsu_service/get_ftp.request.xml",
            "soap/fsu_service/get_ftp.response.xml",
            "soap/fsu_service/set_ftp.request.xml",
            "soap/fsu_service/set_ftp.response.xml",
            "soap/fsu_service/get_logininfo.request.xml",
            "soap/fsu_service/get_logininfo.response.xml",
            "soap/fsu_service/time_check.request.xml",
            "soap/fsu_service/time_check.response.xml"
    })
    void shouldLoadAllSoapFixtures(String path) {
        String content = BInterfaceFixtureLoader.load(path);
        assertNotNull(content);
        assertFalse(content.isEmpty());
        assertTrue(content.contains("soap:Envelope"), "SOAP fixture 应包含 Envelope: " + path);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "xmldata/login.request.xml",
            "xmldata/login.response.xml",
            "xmldata/heartbeat.request.xml",
            "xmldata/heartbeat.response.xml",
            "xmldata/send_data.request.xml",
            "xmldata/send_data.response.xml",
            "xmldata/send_alarm.request.xml",
            "xmldata/send_alarm.response.xml",
            "xmldata/get_data.request.xml",
            "xmldata/get_data.response.xml",
            "xmldata/get_threshold.request.xml",
            "xmldata/get_threshold.response.xml",
            "xmldata/set_threshold.request.xml",
            "xmldata/set_threshold.response.xml",
            "xmldata/set_point.request.xml",
            "xmldata/set_point.response.xml",
            "xmldata/get_ftp.request.xml",
            "xmldata/get_ftp.response.xml",
            "xmldata/set_ftp.request.xml",
            "xmldata/set_ftp.response.xml",
            "xmldata/get_logininfo.request.xml",
            "xmldata/get_logininfo.response.xml",
            "xmldata/time_check.request.xml",
            "xmldata/time_check.response.xml"
    })
    void shouldLoadAllXmlDataFixtures(String path) {
        String content = BInterfaceFixtureLoader.load(path);
        assertNotNull(content);
        assertFalse(content.isEmpty());
        assertTrue(content.contains("PK_Type"), "xmlData fixture 应包含 PK_Type: " + path);
    }
}
