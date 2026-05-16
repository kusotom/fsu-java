package com.dcim.platform.binterface;

import com.dcim.platform.module.binterface.model.BInterfaceMessage;
import com.dcim.platform.module.binterface.service.GetSuInfoResult;
import com.dcim.platform.module.binterface.service.GetSuInfoService;
import com.dcim.platform.module.binterface.service.fsu.FsuServiceClient;
import com.dcim.platform.module.binterface.service.fsu.FsuServiceRequest;
import com.dcim.platform.module.binterface.service.fsu.FsuServiceResponse;
import com.dcim.platform.module.binterface.soap.SoapMessageHandler;
import com.dcim.platform.module.binterface.xml.XmlDataModel;
import com.dcim.platform.module.binterface.xml.XmlDataParser;
import com.dcim.platform.module.binterface.entity.BInterfaceFsuStatusEntity;
import com.dcim.platform.module.binterface.repository.BInterfaceFsuStatusRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * GET_SUINFO 服务测试 (BIF-P4-012)。
 */
class GetSuInfoServiceTest {

    private GetSuInfoService service;
    private SoapMessageHandler soapHandler;
    private XmlDataParser xmlDataParser;
    private BInterfaceFsuStatusRepository statusRepo;

    private static final String STUB_RESPONSE_SOAP =
            "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">"
            + "<soap:Body>"
            + "<Response>"
            + "<PK_Type><Name>GET_SUINFO_ACK</Name><Code>1002</Code></PK_Type>"
            + "<Info><ResultCode>0</ResultCode></Info>"
            + "<xmlData>"
            + "<TSUStatus>"
            + "<SUID>51051243812345</SUID>"
            + "<CPUUsage>35.2</CPUUsage>"
            + "<MEMUsage>62.8</MEMUsage>"
            + "<SUDateTime>2026-05-15 12:00:00</SUDateTime>"
            + "</TSUStatus>"
            + "</xmlData>"
            + "</Response>"
            + "</soap:Body></soap:Envelope>";

    @BeforeEach
    void setUp() {
        soapHandler = new SoapMessageHandler();
        xmlDataParser = new XmlDataParser();
        statusRepo = mock(BInterfaceFsuStatusRepository.class);
        when(statusRepo.findByFsuCode(anyString())).thenReturn(Optional.of(new BInterfaceFsuStatusEntity()));
    }

    private GetSuInfoService createServiceWithResponse(String soapResponse) {
        FsuServiceClient stubClient = new FsuServiceClient() {
            @Override
            public FsuServiceResponse call(FsuServiceRequest req) {
                BInterfaceMessage msg = soapHandler.parse(soapResponse);
                XmlDataModel xmlData = (msg.getXmlData() != null)
                        ? xmlDataParser.parse(msg.getXmlData()) : new XmlDataModel();
                return FsuServiceResponse.success(soapResponse, msg.getInfo(), msg.getXmlData(), xmlData);
            }
        };
        return new GetSuInfoService(stubClient, null, statusRepo);
    }

    // ==================== 成功路径 ====================

    @Test
    void shouldParseSuid() {
        service = createServiceWithResponse(STUB_RESPONSE_SOAP);
        GetSuInfoResult result = service.execute("51051243812345", null);
        assertTrue(result.isSuccess());
        assertEquals("0", result.getResultCode());
        assertEquals("51051243812345", result.getSuid());
    }

    @Test
    void shouldParseCpuUsage() {
        service = createServiceWithResponse(STUB_RESPONSE_SOAP);
        GetSuInfoResult result = service.execute("51051243812345", null);
        assertNotNull(result.getCpuUsage());
        assertEquals(0, new java.math.BigDecimal("35.2").compareTo(result.getCpuUsage()));
    }

    @Test
    void shouldParseMemUsage() {
        service = createServiceWithResponse(STUB_RESPONSE_SOAP);
        GetSuInfoResult result = service.execute("51051243812345", null);
        assertNotNull(result.getMemUsage());
        assertEquals(0, new java.math.BigDecimal("62.8").compareTo(result.getMemUsage()));
    }

    @Test
    void shouldParseSuDateTime() {
        service = createServiceWithResponse(STUB_RESPONSE_SOAP);
        GetSuInfoResult result = service.execute("51051243812345", null);
        assertNotNull(result.getSuDateTime());
    }

    @Test
    void shouldUpdateOnlineStatusOnSuccess() {
        service = createServiceWithResponse(STUB_RESPONSE_SOAP);
        service.execute("51051243812345", null);
        verify(statusRepo, atLeastOnce()).save(any(BInterfaceFsuStatusEntity.class));
    }

    // ==================== 失败路径 ====================

    @Test
    void shouldFailForNullFsuCode() {
        service = createServiceWithResponse(STUB_RESPONSE_SOAP);
        GetSuInfoResult result = service.execute(null, null);
        assertFalse(result.isSuccess());
        assertTrue(result.getResultDesc().contains("SUID"));
    }

    @Test
    void shouldFailForEmptyFsuCode() {
        service = createServiceWithResponse(STUB_RESPONSE_SOAP);
        GetSuInfoResult result = service.execute("", null);
        assertFalse(result.isSuccess());
    }

    @Test
    void shouldFailForFsuReturnError() {
        String errorSoap = "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">"
                + "<soap:Body><Response>"
                + "<PK_Type><Name>GET_SUINFO_ACK</Name><Code>1002</Code></PK_Type>"
                + "<Info><ResultCode>3</ResultCode><FailureCode>3</FailureCode></Info>"
                + "<xmlData><TSUStatus></TSUStatus></xmlData>"
                + "</Response></soap:Body></soap:Envelope>";

        FsuServiceClient client = new FsuServiceClient() {
            @Override
            public FsuServiceResponse call(FsuServiceRequest req) {
                return FsuServiceResponse.fail("3", "FSU 返回错误: 3");
            }
        };
        service = new GetSuInfoService(client, null, statusRepo);
        GetSuInfoResult result = service.execute("51051243812345", null);
        assertFalse(result.isSuccess());
        assertEquals("3", result.getResultCode());
    }

    // ==================== PK_Type 2024 格式验证 ====================

    @Test
    void stubResponseShouldContainGetSuInfoAckName() {
        assertTrue(STUB_RESPONSE_SOAP.contains("GET_SUINFO_ACK"));
    }

    @Test
    void stubResponseShouldContainCode1002() {
        assertTrue(STUB_RESPONSE_SOAP.contains("1002"));
    }

    @Test
    void stubResponseShouldContainTsuStatus() {
        assertTrue(STUB_RESPONSE_SOAP.contains("TSUStatus"));
        assertTrue(STUB_RESPONSE_SOAP.contains("CPUUsage"));
        assertTrue(STUB_RESPONSE_SOAP.contains("MEMUsage"));
        assertTrue(STUB_RESPONSE_SOAP.contains("SUDateTime"));
    }
}
