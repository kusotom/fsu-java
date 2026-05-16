package com.dcim.platform.binterface;

import com.dcim.platform.module.binterface.model.BInterfaceMessage;
import com.dcim.platform.module.binterface.service.GetSpConfigOptionResult;
import com.dcim.platform.module.binterface.service.GetSpConfigOptionService;
import com.dcim.platform.module.binterface.service.fsu.FsuServiceClient;
import com.dcim.platform.module.binterface.service.fsu.FsuServiceResponse;
import com.dcim.platform.module.binterface.soap.SoapMessageHandler;
import com.dcim.platform.module.binterface.xml.XmlDataModel;
import com.dcim.platform.module.binterface.xml.XmlDataParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * GET_SPCONFIGOPTION 服务测试 (BIF-P4-015)。
 */
class GetSpConfigOptionServiceTest {

    private SoapMessageHandler soapHandler;
    private XmlDataParser xmlDataParser;

    private static final String STUB_RESPONSE =
            "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">"
            + "<soap:Body><Response>"
            + "<PK_Type><Name>GET_SPCONFIGOPTION_ACK</Name><Code>402</Code></PK_Type>"
            + "<Info><ResultCode>0</ResultCode></Info>"
            + "</Response></soap:Body></soap:Envelope>";

    @BeforeEach
    void setUp() {
        soapHandler = new SoapMessageHandler();
        xmlDataParser = new XmlDataParser();
    }

    private GetSpConfigOptionService createService(String soap) {
        FsuServiceClient client = req -> {
            BInterfaceMessage msg = soapHandler.parse(soap);
            XmlDataModel xd = (msg.getXmlData() != null) ? xmlDataParser.parse(msg.getXmlData()) : new XmlDataModel();
            return FsuServiceResponse.success(soap, msg.getInfo(), msg.getXmlData(), xd);
        };
        return new GetSpConfigOptionService(client, null);
    }

    @Test
    void shouldSucceed() {
        GetSpConfigOptionService svc = createService(STUB_RESPONSE);
        GetSpConfigOptionResult r = svc.execute("FSU-001", null);
        assertTrue(r.isSuccess());
        assertEquals("0", r.getResultCode());
        assertEquals("FSU-001", r.getSuid());
    }

    @Test
    void shouldFailForNullFsuCode() {
        assertFalse(createService(STUB_RESPONSE).execute(null, null).isSuccess());
    }

    @Test
    void shouldFailForFsuError() {
        FsuServiceClient client = req -> FsuServiceResponse.fail("3", "FSU 错误");
        GetSpConfigOptionService svc = new GetSpConfigOptionService(client, null);
        assertFalse(svc.execute("FSU-001", null).isSuccess());
    }

    @Test
    void stubResponseContainsCorrectFields() {
        assertTrue(STUB_RESPONSE.contains("GET_SPCONFIGOPTION_ACK"));
        assertTrue(STUB_RESPONSE.contains("402"));
    }

    @Test
    void getThresholdShouldBeCompat() {
        assertTrue(com.dcim.platform.module.binterface.compat.BInterfaceCommandAliasMapper
                .isCompatCommand("GET_THRESHOLD"));
    }
}
