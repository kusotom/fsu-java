package com.dcim.platform.binterface;

import com.dcim.platform.module.binterface.model.BInterfaceMessage;
import com.dcim.platform.module.binterface.service.GetActiveAlarmResult;
import com.dcim.platform.module.binterface.service.GetActiveAlarmService;
import com.dcim.platform.module.binterface.service.fsu.FsuServiceClient;
import com.dcim.platform.module.binterface.service.fsu.FsuServiceResponse;
import com.dcim.platform.module.binterface.soap.SoapMessageHandler;
import com.dcim.platform.module.binterface.xml.XmlDataModel;
import com.dcim.platform.module.binterface.xml.XmlDataParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * GET_ACTIVEALARM 服务测试 (BIF-P4-016)。
 */
class GetActiveAlarmServiceTest {

    private SoapMessageHandler soapHandler;
    private XmlDataParser xmlDataParser;

    private static final String STUB_RESPONSE =
            "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">"
            + "<soap:Body><Response>"
            + "<PK_Type><Name>GET_ACTIVEALARM_ACK</Name><Code>604</Code></PK_Type>"
            + "<Info><ResultCode>0</ResultCode></Info>"
            + "<xmlData>"
            + "<Alarm>"
            + "<SerialNo>0012345678</SerialNo>"
            + "<DeviceID>11010110100001</DeviceID>"
            + "<SPID>0430101001</SPID>"
            + "<StartTime>2026-05-16 08:00:00</StartTime>"
            + "<TriggerVal>46.1</TriggerVal>"
            + "<AlarmLevel>二级</AlarmLevel>"
            + "<AlarmFlag>开始</AlarmFlag>"
            + "<AlarmDesc>欠压告警</AlarmDesc>"
            + "<AlarmFriDesc>电池电压低于门限</AlarmFriDesc>"
            + "</Alarm>"
            + "</xmlData>"
            + "</Response></soap:Body></soap:Envelope>";

    @BeforeEach
    void setUp() {
        soapHandler = new SoapMessageHandler();
        xmlDataParser = new XmlDataParser();
    }

    private GetActiveAlarmService createService(String soap) {
        FsuServiceClient client = req -> {
            BInterfaceMessage msg = soapHandler.parse(soap);
            XmlDataModel xd = (msg.getXmlData() != null) ? xmlDataParser.parse(msg.getXmlData()) : new XmlDataModel();
            return FsuServiceResponse.success(soap, msg.getInfo(), msg.getXmlData(), xd);
        };
        return new GetActiveAlarmService(client, null);
    }

    @Test
    void shouldSucceedWithOneAlarm() {
        GetActiveAlarmService svc = createService(STUB_RESPONSE);
        GetActiveAlarmResult r = svc.execute("FSU-001", null);
        assertTrue(r.isSuccess());
        assertEquals("FSU-001", r.getSuid());
        assertEquals(1, r.getActiveAlarms().size());
        assertEquals(1, r.getParsedCount());
    }

    @Test
    void shouldParseSerialNo() {
        GetActiveAlarmResult r = createService(STUB_RESPONSE).execute("FSU-001", null);
        assertEquals("0012345678", r.getActiveAlarms().get(0).getSerialNo());
    }

    @Test
    void shouldParseDeviceId() {
        assertEquals("11010110100001", createService(STUB_RESPONSE).execute("FSU-001", null)
                .getActiveAlarms().get(0).getDeviceId());
    }

    @Test
    void shouldParseSpid() {
        assertEquals("0430101001", createService(STUB_RESPONSE).execute("FSU-001", null)
                .getActiveAlarms().get(0).getSpid());
    }

    @Test
    void shouldParseStartTime() {
        assertNotNull(createService(STUB_RESPONSE).execute("FSU-001", null)
                .getActiveAlarms().get(0).getStartTime());
    }

    @Test
    void shouldParseTriggerVal() {
        assertEquals("46.1", createService(STUB_RESPONSE).execute("FSU-001", null)
                .getActiveAlarms().get(0).getTriggerVal());
    }

    @Test
    void shouldParseAlarmDesc() {
        assertEquals("欠压告警", createService(STUB_RESPONSE).execute("FSU-001", null)
                .getActiveAlarms().get(0).getAlarmDesc());
    }

    @Test
    void shouldContain2024Format() {
        assertTrue(STUB_RESPONSE.contains("GET_ACTIVEALARM_ACK"));
        assertTrue(STUB_RESPONSE.contains("604"));
    }

    @Test
    void emptyAlarmsShouldReturnZero() {
        String empty = "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">"
                + "<soap:Body><Response>"
                + "<PK_Type><Name>GET_ACTIVEALARM_ACK</Name><Code>604</Code></PK_Type>"
                + "<Info><ResultCode>0</ResultCode></Info>"
                + "<xmlData></xmlData>"
                + "</Response></soap:Body></soap:Envelope>";
        GetActiveAlarmResult r = createService(empty).execute("FSU-001", null);
        assertTrue(r.isSuccess());
        assertEquals(0, r.getTotalCount());
        assertEquals(0, r.getActiveAlarms().size());
    }

    @Test
    void shouldFailForNullFsuCode() {
        assertFalse(createService(STUB_RESPONSE).execute(null, null).isSuccess());
    }

    @Test
    void shouldFailForFsuError() {
        FsuServiceClient client = req -> FsuServiceResponse.fail("3", "错误");
        assertFalse(new GetActiveAlarmService(client, null).execute("FSU-001", null).isSuccess());
    }

    // ==================== TAlarm 包裹形态 (BIF-P4-FIX-001) ====================

    private static final String TALARM_RESPONSE =
            "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">"
            + "<soap:Body><Response>"
            + "<PK_Type><Name>GET_ACTIVEALARM_ACK</Name><Code>604</Code></PK_Type>"
            + "<Info><ResultCode>0</ResultCode></Info>"
            + "<xmlData>"
            + "<TAlarm>"
            + "<SerialNo>SN-T-001</SerialNo>"
            + "<DeviceID>DEV-T-001</DeviceID>"
            + "<SPID>SP-T-001</SPID>"
            + "<StartTime>2026-05-19 10:00:00</StartTime>"
            + "<TriggerVal>52.3</TriggerVal>"
            + "<AlarmLevel>一级</AlarmLevel>"
            + "<AlarmFlag>开始</AlarmFlag>"
            + "<AlarmDesc>温度过高告警</AlarmDesc>"
            + "</TAlarm>"
            + "</xmlData>"
            + "</Response></soap:Body></soap:Envelope>";

    @Test
    void shouldParseAlarmWrappedInTAlarm() {
        FsuServiceClient client = req -> {
            BInterfaceMessage msg = soapHandler.parse(TALARM_RESPONSE);
            XmlDataModel xd = (msg.getXmlData() != null) ? xmlDataParser.parse(msg.getXmlData()) : new XmlDataModel();
            return FsuServiceResponse.success(TALARM_RESPONSE, msg.getInfo(), msg.getXmlData(), xd);
        };
        GetActiveAlarmService svc = new GetActiveAlarmService(client, null);
        GetActiveAlarmResult r = svc.execute("FSU-T01", null);
        assertTrue(r.isSuccess());
        assertEquals(1, r.getParsedCount());
        assertEquals("SN-T-001", r.getActiveAlarms().get(0).getSerialNo());
        assertEquals("温度过高告警", r.getActiveAlarms().get(0).getAlarmDesc());
    }

    @Test
    void xmlDataWithUnexpectedFormatShouldNotCrash() {
        String unexpected = "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">"
                + "<soap:Body><Response>"
                + "<PK_Type><Name>GET_ACTIVEALARM_ACK</Name><Code>604</Code></PK_Type>"
                + "<Info><ResultCode>0</ResultCode></Info>"
                + "<xmlData><UnexpectedTag><Data>garbage</Data></UnexpectedTag></xmlData>"
                + "</Response></soap:Body></soap:Envelope>";
        FsuServiceClient client = req -> {
            BInterfaceMessage msg = soapHandler.parse(unexpected);
            XmlDataModel xd = (msg.getXmlData() != null) ? xmlDataParser.parse(msg.getXmlData()) : new XmlDataModel();
            return FsuServiceResponse.success(unexpected, msg.getInfo(), msg.getXmlData(), xd);
        };
        GetActiveAlarmService svc = new GetActiveAlarmService(client, null);
        GetActiveAlarmResult r = svc.execute("FSU-001", null);
        assertNotNull(r);  // 不应崩溃
        assertTrue(r.isSuccess());  // FSU 返回 ResultCode=0，解析失败不应影响 success
    }

    @Test
    void realDeviceAccessedShouldBeFalseFromStub() {
        GetActiveAlarmService svc = createService(STUB_RESPONSE);
        GetActiveAlarmResult r = svc.execute("FSU-001", null);
        assertFalse(r.isRealDeviceAccessed(), "Stub 响应应标记 realDeviceAccessed=false");
    }
}
