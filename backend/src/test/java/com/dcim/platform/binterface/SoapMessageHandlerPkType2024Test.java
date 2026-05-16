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

    // ==================== 辅助 ====================

    private String docStyleEnvelope(String innerXml) {
        return "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">"
                + "<soap:Body>" + innerXml + "</soap:Body>"
                + "</soap:Envelope>";
    }
}
