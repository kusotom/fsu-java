package com.dcim.platform.binterface;

import com.dcim.platform.module.binterface.model.BInterfaceMessage;
import com.dcim.platform.module.binterface.service.GetSuFtpResult;
import com.dcim.platform.module.binterface.service.GetSuFtpService;
import com.dcim.platform.module.binterface.service.fsu.FsuServiceClient;
import com.dcim.platform.module.binterface.service.fsu.FsuServiceRequest;
import com.dcim.platform.module.binterface.service.fsu.FsuServiceResponse;
import com.dcim.platform.module.binterface.soap.SoapMessageHandler;
import com.dcim.platform.module.binterface.xml.XmlDataModel;
import com.dcim.platform.module.binterface.xml.XmlDataParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * GET_SUFTP 服务测试 (BIF-P4-013)。
 */
class GetSuFtpServiceTest {

    private SoapMessageHandler soapHandler;
    private XmlDataParser xmlDataParser;

    private static final String STUB_RESPONSE =
            "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">"
            + "<soap:Body>"
            + "<Response>"
            + "<PK_Type><Name>GET_SUFTP_ACK</Name><Code>802</Code></PK_Type>"
            + "<Info><ResultCode>0</ResultCode></Info>"
            + "<xmlData>"
            + "<SUFTPConfig>"
            + "<SUID>51051243812345</SUID>"
            + "<UserName>fsu_user</UserName>"
            + "<Password>secret123</Password>"
            + "<FTPPort>21</FTPPort>"
            + "</SUFTPConfig>"
            + "</xmlData>"
            + "</Response>"
            + "</soap:Body></soap:Envelope>";

    @BeforeEach
    void setUp() {
        soapHandler = new SoapMessageHandler();
        xmlDataParser = new XmlDataParser();
    }

    private GetSuFtpService createService(String soap) {
        FsuServiceClient client = req -> {
            BInterfaceMessage msg = soapHandler.parse(soap);
            XmlDataModel xd = (msg.getXmlData() != null) ? xmlDataParser.parse(msg.getXmlData()) : new XmlDataModel();
            return FsuServiceResponse.success(soap, msg.getInfo(), msg.getXmlData(), xd);
        };
        return new GetSuFtpService(client, null);
    }

    @Test
    void shouldParseSuid() {
        GetSuFtpService svc = createService(STUB_RESPONSE);
        GetSuFtpResult r = svc.execute("51051243812345", null);
        assertTrue(r.isSuccess());
        assertEquals("51051243812345", r.getSuid());
    }

    @Test
    void shouldParseUserName() {
        GetSuFtpService svc = createService(STUB_RESPONSE);
        assertEquals("fsu_user", svc.execute("51051243812345", null).getUserName());
    }

    @Test
    void shouldParsePassword() {
        GetSuFtpService svc = createService(STUB_RESPONSE);
        assertEquals("secret123", svc.execute("51051243812345", null).getPassword());
    }

    @Test
    void shouldParseFtpPort() {
        GetSuFtpService svc = createService(STUB_RESPONSE);
        assertEquals(21, svc.execute("51051243812345", null).getFtpPort());
    }

    @Test
    void shouldMaskPasswordInToString() {
        GetSuFtpService svc = createService(STUB_RESPONSE);
        String s = svc.execute("51051243812345", null).toString();
        assertFalse(s.contains("secret123"), "toString 不得包含明文密码");
    }

    @Test
    void shouldMaskPasswordGetter() {
        GetSuFtpService svc = createService(STUB_RESPONSE);
        GetSuFtpResult r = svc.execute("51051243812345", null);
        assertNotEquals("secret123", r.getMaskedPassword());
        assertTrue(r.getMaskedPassword().contains("****"));
    }

    @Test
    void shouldMaskUserName() {
        GetSuFtpService svc = createService(STUB_RESPONSE);
        String s = svc.execute("51051243812345", null).toString();
        assertFalse(s.contains("fsu_user"), "toString 不得包含明文用户名");
    }

    @Test
    void shouldFailForNullFsuCode() {
        GetSuFtpService svc = createService(STUB_RESPONSE);
        assertFalse(svc.execute(null, null).isSuccess());
    }

    @Test
    void stubResponseContainsGetSuFtpAck() {
        assertTrue(STUB_RESPONSE.contains("GET_SUFTP_ACK"));
    }

    @Test
    void stubResponseContainsCode802() {
        assertTrue(STUB_RESPONSE.contains("802"));
    }

    // === GET_FTP 旧路径兼容 (alias) ===

    @Test
    void getFtpShouldAliasToGetSuFtp() {
        var result = com.dcim.platform.module.binterface.compat.BInterfaceCommandAliasMapper
                .to2024("GET_FTP");
        assertTrue(result.isPresent());
        assertEquals("GET_SUFTP", result.get().getName());
        assertEquals(801, result.get().getCode());
    }
}
