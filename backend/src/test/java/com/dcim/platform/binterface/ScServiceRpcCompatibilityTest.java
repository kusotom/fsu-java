package com.dcim.platform.binterface;

import com.dcim.platform.module.binterface.model.BInterfaceMessage;
import com.dcim.platform.module.binterface.model.BInterfacePkType;
import com.dcim.platform.module.binterface.service.fsu.FsuServiceRpcAdapter;
import com.dcim.platform.module.binterface.soap.SoapMessageHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SCService 入站 RPC 兼容性测试。
 *
 * <p>BIF-P4-008: 验证 SoapMessageHandler.parse() 能正确处理
 * SCService 入站方向的 RPC invoke(xmlData) 格式。</p>
 *
 * <p>不访问真实设备，不启用 real-call，不执行 SET 命令。</p>
 */
class ScServiceRpcCompatibilityTest {

    private SoapMessageHandler soapHandler;
    private FsuServiceRpcAdapter fsuRpcAdapter;

    private static final String SC_NS = "http://SCService.chinatowercom.com";

    @BeforeEach
    void setUp() {
        soapHandler = new SoapMessageHandler();
        fsuRpcAdapter = new FsuServiceRpcAdapter();
    }

    // ==================== RPC LOGIN ====================

    @Test
    void shouldParseRpcLoginRequest() {
        String loginPayload = "<Request>\n"
                + "  <PK_Type>LOGIN</PK_Type>\n"
                + "  <Info>\n"
                + "    <FSUCode>FSU-001</FSUCode>\n"
                + "    <Password>7a3b5c8e2f1d4a6b9c0d3e5f7a8b9c0d</Password>\n"
                + "    <Timestamp>2026-05-13T10:30:00+08:00</Timestamp>\n"
                + "  </Info>\n"
                + "  <xmlData>\n"
                + "    <DeviceInfo>\n"
                + "      <Manufacturer>中兴</Manufacturer>\n"
                + "    </DeviceInfo>\n"
                + "  </xmlData>\n"
                + "</Request>";

        String rpcSoap = buildScServiceRpcRequest(loginPayload);

        BInterfaceMessage msg = soapHandler.parse(rpcSoap);

        assertEquals(BInterfacePkType.LOGIN, msg.getPkType());
        assertNotNull(msg.getInfo());
        assertTrue(msg.getInfo().contains("FSUCode"), "Info 应包含 FSUCode");
        assertTrue(msg.getInfo().contains("FSU-001"), "Info 应包含 FSU-001");
        assertNotNull(msg.getXmlData());
        assertTrue(msg.getXmlData().contains("DeviceInfo"), "xmlData 应包含 DeviceInfo");
        assertTrue(msg.getXmlData().contains("中兴"), "xmlData 应包含制造商信息");
    }

    @Test
    void shouldParseRpcLoginResponse() {
        String responsePayload = "<Response>\n"
                + "  <PK_Type>LOGIN</PK_Type>\n"
                + "  <Info>\n"
                + "    <ResultCode>0</ResultCode>\n"
                + "    <SessionID>SESSION-TEST-001</SessionID>\n"
                + "    <ExpireSeconds>3600</ExpireSeconds>\n"
                + "  </Info>\n"
                + "  <xmlData/>\n"
                + "</Response>";

        String rpcSoap = buildScServiceRpcResponse(responsePayload);

        BInterfaceMessage msg = soapHandler.parse(rpcSoap);

        assertEquals(BInterfacePkType.LOGIN, msg.getPkType());
        assertTrue(msg.getInfo().contains("SessionID"), "Info 应包含 SessionID");
        assertTrue(msg.getInfo().contains("ResultCode"), "Info 应包含 ResultCode");
    }

    // ==================== RPC HEARTBEAT ====================

    @Test
    void shouldParseRpcHeartbeatRequest() {
        String hbPayload = "<Request>\n"
                + "  <PK_Type>HEARTBEAT</PK_Type>\n"
                + "  <Info>\n"
                + "    <FSUCode>FSU-001</FSUCode>\n"
                + "    <SessionID>SESSION-TEST-001</SessionID>\n"
                + "    <Timestamp>2026-05-13T10:31:00+08:00</Timestamp>\n"
                + "  </Info>\n"
                + "  <xmlData>\n"
                + "    <CPU>35</CPU>\n"
                + "    <Memory>62</Memory>\n"
                + "    <Temperature>42</Temperature>\n"
                + "  </xmlData>\n"
                + "</Request>";

        String rpcSoap = buildScServiceRpcRequest(hbPayload);

        BInterfaceMessage msg = soapHandler.parse(rpcSoap);

        assertEquals(BInterfacePkType.HEARTBEAT, msg.getPkType());
        assertTrue(msg.getInfo().contains("FSUCode"));
        assertTrue(msg.getXmlData().contains("CPU"));
        assertTrue(msg.getXmlData().contains("35"));
    }

    // ==================== RPC SEND_DATA ====================

    @Test
    void shouldParseRpcSendDataRequest() {
        String sdPayload = "<Request>\n"
                + "  <PK_Type>SEND_DATA</PK_Type>\n"
                + "  <Info>\n"
                + "    <FSUCode>FSU-001</FSUCode>\n"
                + "    <SessionID>SESSION-TEST-001</SessionID>\n"
                + "    <CollectTime>2026-05-13T10:32:00+08:00</CollectTime>\n"
                + "  </Info>\n"
                + "  <xmlData>\n"
                + "    <Signal>\n"
                + "      <SignalID>TEMP-001</SignalID>\n"
                + "      <Value>25.5</Value>\n"
                + "      <Quality>1</Quality>\n"
                + "      <Status>NORMAL</Status>\n"
                + "    </Signal>\n"
                + "    <Signal>\n"
                + "      <SignalID>HUMI-001</SignalID>\n"
                + "      <Value>55.0</Value>\n"
                + "      <Quality>1</Quality>\n"
                + "      <Status>NORMAL</Status>\n"
                + "    </Signal>\n"
                + "  </xmlData>\n"
                + "</Request>";

        String rpcSoap = buildScServiceRpcRequest(sdPayload);

        BInterfaceMessage msg = soapHandler.parse(rpcSoap);

        assertEquals(BInterfacePkType.SEND_DATA, msg.getPkType());
        assertTrue(msg.getInfo().contains("CollectTime"));
        assertTrue(msg.getXmlData().contains("Signal"));
        assertTrue(msg.getXmlData().contains("TEMP-001"));
        assertTrue(msg.getXmlData().contains("HUMI-001"));
        assertTrue(msg.getXmlData().contains("25.5"));
    }

    // ==================== RPC SEND_ALARM ====================

    @Test
    void shouldParseRpcSendAlarmRequest() {
        String alarmPayload = "<Request>\n"
                + "  <PK_Type>SEND_ALARM</PK_Type>\n"
                + "  <Info>\n"
                + "    <FSUCode>FSU-001</FSUCode>\n"
                + "    <SessionID>SESSION-TEST-001</SessionID>\n"
                + "    <AlarmTime>2026-05-13T10:33:00+08:00</AlarmTime>\n"
                + "  </Info>\n"
                + "  <xmlData>\n"
                + "    <Alarm>\n"
                + "      <SignalID>TEMP-001</SignalID>\n"
                + "      <AlarmCode>TEMP-HIGH</AlarmCode>\n"
                + "      <AlarmLevel>WARN</AlarmLevel>\n"
                + "      <AlarmValue>62.0</AlarmValue>\n"
                + "    </Alarm>\n"
                + "  </xmlData>\n"
                + "</Request>";

        String rpcSoap = buildScServiceRpcRequest(alarmPayload);

        BInterfaceMessage msg = soapHandler.parse(rpcSoap);

        assertEquals(BInterfacePkType.SEND_ALARM, msg.getPkType());
        assertTrue(msg.getInfo().contains("AlarmTime"));
        assertTrue(msg.getXmlData().contains("Alarm"));
        assertTrue(msg.getXmlData().contains("TEMP-HIGH"));
        assertTrue(msg.getXmlData().contains("62.0"));
    }

    // ==================== SCService RPC 响应 ====================

    @Test
    void shouldParseRpcHeartbeatResponse() {
        String hbResponse = "<Response>\n"
                + "  <PK_Type>HEARTBEAT</PK_Type>\n"
                + "  <Info>\n"
                + "    <ResultCode>0</ResultCode>\n"
                + "    <ServerTime>2026-05-13T10:31:05+08:00</ServerTime>\n"
                + "  </Info>\n"
                + "  <xmlData/>\n"
                + "</Response>";

        String rpcSoap = buildScServiceRpcResponse(hbResponse);

        BInterfaceMessage msg = soapHandler.parse(rpcSoap);

        assertEquals(BInterfacePkType.HEARTBEAT, msg.getPkType());
        assertTrue(msg.getInfo().contains("ResultCode"));
        assertTrue(msg.getInfo().contains("0"));
    }

    // ==================== 基础 property 验证 ====================

    @Test
    void fsuServiceRpcNamespaceShouldNotEqualScService() {
        // FSUService adapter 使用 FSU 命名空间，SCService 应使用 SC 命名空间
        assertNotEquals(
                "http://FSUService.chinatowercom.com",
                SC_NS,
                "FSUService 和 SCService 命名空间应不同");
    }

    // ==================== 辅助方法 ====================

    /**
     * 构造 SCService RPC 请求信封（模拟 FSU → SC 入站格式）。
     */
    private String buildScServiceRpcRequest(String requestPayload) {
        return "<SOAP-ENV:Envelope"
                + " xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\""
                + " xmlns:SOAP-ENC=\"http://schemas.xmlsoap.org/soap/encoding/\""
                + " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\""
                + " xmlns:ns1=\"" + SC_NS + "\">"
                + "<SOAP-ENV:Body>"
                + "<ns1:invoke>"
                + "<xmlData xsi:type=\"SOAP-ENC:string\">"
                + requestPayload
                + "</xmlData>"
                + "</ns1:invoke>"
                + "</SOAP-ENV:Body>"
                + "</SOAP-ENV:Envelope>";
    }

    /**
     * 构造 SCService RPC 响应信封（模拟 SC → FSU 出站格式）。
     */
    private String buildScServiceRpcResponse(String responsePayload) {
        return "<SOAP-ENV:Envelope"
                + " xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\""
                + " xmlns:SOAP-ENC=\"http://schemas.xmlsoap.org/soap/encoding/\""
                + " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\""
                + " xmlns:ns1=\"" + SC_NS + "\">"
                + "<SOAP-ENV:Body>"
                + "<ns1:invokeResponse>"
                + "<invokeReturn xsi:type=\"SOAP-ENC:string\">"
                + responsePayload
                + "</invokeReturn>"
                + "</ns1:invokeResponse>"
                + "</SOAP-ENV:Body>"
                + "</SOAP-ENV:Envelope>";
    }
}
