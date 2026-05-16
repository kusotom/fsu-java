package com.dcim.platform.binterface;

import com.dcim.platform.module.binterface.model.BInterfaceMessage;
import com.dcim.platform.module.binterface.service.SetTimeResult;
import com.dcim.platform.module.binterface.service.SetTimeService;
import com.dcim.platform.module.binterface.service.fsu.FsuServiceClient;
import com.dcim.platform.module.binterface.service.fsu.FsuServiceResponse;
import com.dcim.platform.module.binterface.soap.SoapMessageHandler;
import com.dcim.platform.module.binterface.xml.XmlDataModel;
import com.dcim.platform.module.binterface.xml.XmlDataParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SET_TIME 服务测试 (BIF-P4-014)。
 */
class SetTimeServiceTest {

    private SoapMessageHandler soapHandler;
    private XmlDataParser xmlDataParser;

    private static final String STUB_RESPONSE =
            "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">"
            + "<soap:Body><Response>"
            + "<PK_Type><Name>SET_TIME_ACK</Name><Code>902</Code></PK_Type>"
            + "<Info><ResultCode>0</ResultCode></Info>"
            + "</Response></soap:Body></soap:Envelope>";

    @BeforeEach
    void setUp() {
        soapHandler = new SoapMessageHandler();
        xmlDataParser = new XmlDataParser();
    }

    private SetTimeService createService(String soap) {
        FsuServiceClient client = req -> {
            BInterfaceMessage msg = soapHandler.parse(soap);
            XmlDataModel xd = (msg.getXmlData() != null) ? xmlDataParser.parse(msg.getXmlData()) : new XmlDataModel();
            return FsuServiceResponse.success(soap, msg.getInfo(), msg.getXmlData(), xd);
        };
        return new SetTimeService(client, null);
    }

    @Test
    void shouldSucceedWithValidTime() {
        SetTimeService svc = createService(STUB_RESPONSE);
        SetTimeResult r = svc.execute("FSU-001", null, "2026-05-16 10:00:00");
        assertTrue(r.isSuccess());
        assertEquals("FSU-001", r.getSuid());
        assertNotNull(r.getSentTime());
    }

    @Test
    void shouldFailForNullFsuCode() {
        SetTimeService svc = createService(STUB_RESPONSE);
        assertFalse(svc.execute(null, null, "2026-05-16 10:00:00").isSuccess());
    }

    @Test
    void shouldFailForNullTime() {
        SetTimeService svc = createService(STUB_RESPONSE);
        assertFalse(svc.execute("FSU-001", null, null).isSuccess());
    }

    @Test
    void shouldFailForInvalidTimeFormat() {
        SetTimeService svc = createService(STUB_RESPONSE);
        SetTimeResult r = svc.execute("FSU-001", null, "2026/05/16");
        assertFalse(r.isSuccess());
        assertTrue(r.getResultDesc().contains("格式非法"));
    }

    @Test
    void shouldParseValidTimeFormat() {
        SetTimeService svc = createService(STUB_RESPONSE);
        SetTimeResult r = svc.execute("FSU-001", null, "2026-05-16 23:59:59");
        assertTrue(r.isSuccess());
    }

    @Test
    void shouldParseSingleDigitFormat() {
        SetTimeService svc = createService(STUB_RESPONSE);
        SetTimeResult r = svc.execute("FSU-001", null, "2026-01-01 00:00:00");
        assertTrue(r.isSuccess());
    }

    @Test
    void stubResponseContainsSetTimeAck() {
        assertTrue(STUB_RESPONSE.contains("SET_TIME_ACK"));
        assertTrue(STUB_RESPONSE.contains("902"));
    }

    @Test
    void timeCheckShouldAliasToSetTime() {
        var result = com.dcim.platform.module.binterface.compat.BInterfaceCommandAliasMapper
                .to2024("TIME_CHECK");
        assertTrue(result.isPresent());
        assertEquals("SET_TIME", result.get().getName());
        assertEquals(901, result.get().getCode());
    }

    @Test
    void shouldFailForFsuError() {
        FsuServiceClient client = req -> FsuServiceResponse.fail("3", "FSU 错误");
        SetTimeService svc = new SetTimeService(client, null);
        SetTimeResult r = svc.execute("FSU-001", null, "2026-05-16 10:00:00");
        assertFalse(r.isSuccess());
        assertEquals("3", r.getResultCode());
    }
}
