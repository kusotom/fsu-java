package com.dcim.platform.binterface;

import com.dcim.platform.module.binterface.model.BInterfaceMessage;
import com.dcim.platform.module.binterface.model.PkTypeDescriptor;
import com.dcim.platform.module.binterface.soap.SoapMessageHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SoapMessageHandler PK_Type 2024 双格式解析集成测试 (BIF-P4-011)。
 */
class SoapMessageHandlerPkType2024Test {

    private SoapMessageHandler handler;

    @BeforeEach
    void setUp() {
        handler = new SoapMessageHandler();
    }

    // ==================== 旧格式仍能解析 ====================

    @Test
    void shouldParseLegacyPkType() {
        String xml = docStyleEnvelope(
                "<Request>"
                + "<PK_Type>GET_DATA</PK_Type>"
                + "<Info><FSUCode>FSU-001</FSUCode></Info>"
                + "</Request>");

        BInterfaceMessage msg = handler.parse(xml);
        assertNotNull(msg.getPkTypeDescriptor());
        assertEquals(PkTypeDescriptor.Format.LEGACY_TEXT, msg.getPkTypeDescriptor().getFormat());
        assertEquals("GET_DATA", msg.getPkTypeDescriptor().getOriginalText());
    }

    @Test
    void shouldParseLegacyLogin() {
        String xml = docStyleEnvelope(
                "<Request>"
                + "<PK_Type>LOGIN</PK_Type>"
                + "<Info><FSUCode>FSU-001</FSUCode></Info>"
                + "</Request>");

        BInterfaceMessage msg = handler.parse(xml);
        assertEquals(PkTypeDescriptor.Format.LEGACY_TEXT, msg.getPkTypeDescriptor().getFormat());
    }

    // ==================== 2024 Name+Code 格式 ====================

    @Test
    void shouldParseNameCodePkType() {
        String xml = docStyleEnvelope(
                "<Request>"
                + "<PK_Type><Name>GET_DATA</Name><Code>501</Code></PK_Type>"
                + "<Info><FSUCode>FSU-001</FSUCode></Info>"
                + "</Request>");

        BInterfaceMessage msg = handler.parse(xml);
        PkTypeDescriptor d = msg.getPkTypeDescriptor();
        assertNotNull(d);
        assertEquals(PkTypeDescriptor.Format.NAME_CODE, d.getFormat());
        assertEquals("GET_DATA", d.getOriginalName());
        assertEquals(501, d.getOriginalCode());
        assertTrue(d.isNameCodeConsistent());
    }

    @Test
    void shouldParseNameCodeLogin101() {
        String xml = docStyleEnvelope(
                "<Request>"
                + "<PK_Type><Name>LOGIN</Name><Code>101</Code></PK_Type>"
                + "<Info><FSUCode>FSU-001</FSUCode></Info>"
                + "</Request>");

        BInterfaceMessage msg = handler.parse(xml);
        PkTypeDescriptor d = msg.getPkTypeDescriptor();
        assertEquals(PkTypeDescriptor.Format.NAME_CODE, d.getFormat());
        assertTrue(d.isNameCodeConsistent());
    }

    // ==================== Name+Code 不一致检测 ====================

    @Test
    void shouldDetectNameCodeMismatch() {
        String xml = docStyleEnvelope(
                "<Request>"
                + "<PK_Type><Name>GET_DATA</Name><Code>601</Code></PK_Type>"
                + "<Info><FSUCode>FSU-001</FSUCode></Info>"
                + "</Request>");

        BInterfaceMessage msg = handler.parse(xml);
        PkTypeDescriptor d = msg.getPkTypeDescriptor();
        assertFalse(d.isNameCodeConsistent());
        assertNotNull(d.getValidationMessage());
    }

    // ==================== RPC envelope 内的 2024 PK_Type ====================

    @Test
    void shouldParse2024PkTypeInRpcEnvelope() {
        String rpcXml = "<SOAP-ENV:Envelope"
                + " xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\""
                + " xmlns:ns1=\"http://FSUService.chinatowercom.com\""
                + " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\""
                + " xmlns:SOAP-ENC=\"http://schemas.xmlsoap.org/soap/encoding/\">"
                + "<SOAP-ENV:Body>"
                + "<ns1:invoke>"
                + "<xmlData xsi:type=\"SOAP-ENC:string\">"
                + "<Request>"
                + "<PK_Type><Name>GET_DATA</Name><Code>501</Code></PK_Type>"
                + "<Info><FSUCode>FSU-001</FSUCode></Info>"
                + "</Request>"
                + "</xmlData>"
                + "</ns1:invoke>"
                + "</SOAP-ENV:Body>"
                + "</SOAP-ENV:Envelope>";

        BInterfaceMessage msg = handler.parse(rpcXml);
        assertNotNull(msg.getPkTypeDescriptor());
        assertEquals("GET_DATA", msg.getPkTypeDescriptor().getOriginalName());
        assertEquals(501, msg.getPkTypeDescriptor().getOriginalCode());
    }

    // ==================== 向后兼容：不影响现有功能 ====================

    @Test
    void shouldStillSetLegacyPkType() {
        String xml = docStyleEnvelope(
                "<Request>"
                + "<PK_Type>GET_THRESHOLD</PK_Type>"
                + "<Info><FSUCode>FSU-001</FSUCode></Info>"
                + "</Request>");

        BInterfaceMessage msg = handler.parse(xml);
        // 旧 PkType 仍正常设置
        assertNotNull(msg.getPkType());
        // 新 PkTypeDescriptor 也有
        assertNotNull(msg.getPkTypeDescriptor());
        assertTrue(msg.getPkTypeDescriptor().isCompatCommand());
    }

    // ==================== LANDING-004: buildRequest 双格式输出测试 ====================

    @Test
    void shouldBuildRequestWithStructuredPkType() {
        String result = handler.buildRequest("GET_DATA", 501,
                "<FSUCode>FSU-001</FSUCode>", "<SignalID>TEMP-001</SignalID>");
        assertTrue(result.contains("<PK_Type>"), "应包含 PK_Type");
        assertTrue(result.contains("<Name>GET_DATA</Name>"), "应包含 Name 元素");
        assertTrue(result.contains("<Code>501</Code>"), "应包含 Code 元素");
        assertTrue(result.contains("TEMP-001"), "应保留 xmlData");
    }

    @Test
    void shouldBuildRequestWithLegacyPkType() {
        String result = handler.buildRequest("GET_DATA", null,
                "<FSUCode>FSU-001</FSUCode>", "<SignalID>TEMP-001</SignalID>");
        assertTrue(result.contains("<PK_Type>GET_DATA</PK_Type>"), "应为纯文本 PK_Type");
        assertFalse(result.contains("<Name>"), "不应包含 Name 元素");
        assertFalse(result.contains("<Code>"), "不应包含 Code 元素");
    }

    @Test
    void legacyFormatShouldNotContainNameCodeElements() {
        // 验证所有 5 个允许重试的命令在 legacy 格式下均不含 Name/Code
        String[] commands = {"GET_SUINFO", "GET_DATA", "GET_ACTIVEALARM", "GET_SPCONFIGOPTION", "GET_THRESHOLD"};
        for (String cmd : commands) {
            String result = handler.buildRequest(cmd, null, "<SUID>TEST</SUID>", null);
            assertTrue(result.contains("<PK_Type>" + cmd + "</PK_Type>"),
                    cmd + " legacy 格式应为纯文本 PK_Type");
            assertFalse(result.contains("<Name>"), cmd + " legacy 格式不应有 Name");
            assertFalse(result.contains("<Code>"), cmd + " legacy 格式不应有 Code");
        }
    }

    @Test
    void structuredFormatShouldContainNameAndCode() {
        // 验证 structured 格式包含 Name+Code
        String result = handler.buildRequest("GET_ACTIVEALARM", 603,
                "<SUID>TEST</SUID>", null);
        assertTrue(result.contains("<Name>GET_ACTIVEALARM</Name>"));
        assertTrue(result.contains("<Code>603</Code>"));
    }

    @Test
    void legacyGetActiveAlarmShouldStillContainCommandName() {
        String result = handler.buildRequest("GET_ACTIVEALARM", null,
                "<SUID>TEST</SUID>", null);
        assertTrue(result.contains("GET_ACTIVEALARM"), "legacy 格式应包含命令名");
        assertFalse(result.contains("<Name>"), "legacy 格式不应有结构化 Name");
    }

    // ==================== 辅助 ====================

    private String docStyleEnvelope(String innerXml) {
        return "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">"
                + "<soap:Body>" + innerXml + "</soap:Body>"
                + "</soap:Envelope>";
    }
}
