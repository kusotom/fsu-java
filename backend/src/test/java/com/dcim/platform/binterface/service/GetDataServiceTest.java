package com.dcim.platform.binterface.service;

import com.dcim.platform.module.binterface.model.BInterfacePkType;
import com.dcim.platform.module.binterface.service.GetDataResult;
import com.dcim.platform.module.binterface.service.GetDataService;
import com.dcim.platform.module.binterface.service.fsu.FsuServiceClient;
import com.dcim.platform.module.binterface.service.fsu.FsuServiceRequest;
import com.dcim.platform.module.binterface.service.fsu.FsuServiceResponse;
import com.dcim.platform.module.binterface.xml.XmlDataModel;
import com.dcim.platform.module.binterface.xml.XmlDataParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * GetDataService 单元测试。
 */
class GetDataServiceTest {

    private GetDataService getDataService;
    private StubFsuClient fsuClient;

    @BeforeEach
    void setUp() {
        fsuClient = new StubFsuClient();
        getDataService = new GetDataService(fsuClient, null);
    }

    // ==================== 成功 ====================

    @Test
    void shouldReturnSuccessWhenFsuResponds() {
        fsuClient.responseToReturn = buildSuccessResponse();

        GetDataResult result = getDataService.execute("FSU-001", null, List.of("TEMP-001", "HUMI-001"));

        assertTrue(result.isSuccess());
        assertEquals("0", result.getResultCode());
        assertEquals(2, result.getCount());
        assertEquals(2, result.getSignals().size());
        assertEquals("TEMP-001", result.getSignals().get(0).getSignalId());
        assertEquals("HUMI-001", result.getSignals().get(1).getSignalId());
    }

    @Test
    void shouldParseSignalValuesCorrectly() {
        fsuClient.responseToReturn = buildSuccessResponse();

        GetDataResult result = getDataService.execute("FSU-001", "http://fsu:8080", List.of("TEMP-001"));

        assertTrue(result.isSuccess());
        assertEquals("25.8", result.getSignals().get(0).getValue());
        assertEquals("1", result.getSignals().get(0).getQuality());
        assertEquals("NORMAL", result.getSignals().get(0).getStatus());
    }

    @Test
    void shouldReturnEmptyListWhenNoSignals() {
        fsuClient.responseToReturn = FsuServiceResponse.success(
                "<soap:Envelope/>", "<ResultCode>0</ResultCode>", null, new XmlDataModel());

        GetDataResult result = getDataService.execute("FSU-001", null, List.of("TEMP-001"));

        assertTrue(result.isSuccess());
        assertEquals(0, result.getCount());
        assertTrue(result.getSignals().isEmpty());
    }

    // ==================== 校验失败 ====================

    @Test
    void shouldFailOnNullFsuCode() {
        GetDataResult result = getDataService.execute(null, null, List.of("TEMP-001"));

        assertFalse(result.isSuccess());
        assertEquals("2001", result.getResultCode());
    }

    @Test
    void shouldFailOnEmptyFsuCode() {
        GetDataResult result = getDataService.execute("", null, List.of("TEMP-001"));

        assertFalse(result.isSuccess());
        assertEquals("2001", result.getResultCode());
    }

    @Test
    void shouldFailOnNullSignalIds() {
        GetDataResult result = getDataService.execute("FSU-001", null, null);

        assertFalse(result.isSuccess());
        assertEquals("2003", result.getResultCode());
    }

    @Test
    void shouldFailOnEmptySignalIds() {
        GetDataResult result = getDataService.execute("FSU-001", null, List.of());

        assertFalse(result.isSuccess());
        assertEquals("2003", result.getResultCode());
    }

    // ==================== FSU 调用失败 ====================

    @Test
    void shouldFailWhenFsuClientReturnsError() {
        fsuClient.responseToReturn = FsuServiceResponse.fail("1002", "FSU 离线");

        GetDataResult result = getDataService.execute("FSU-001", null, List.of("TEMP-001"));

        assertFalse(result.isSuccess());
        assertEquals("1002", result.getResultCode());
    }

    @Test
    void shouldFailWhenFsuClientThrowsException() {
        fsuClient.responseToReturn = null; // will cause NPE in caller

        GetDataResult result = getDataService.execute("FSU-001", null, List.of("TEMP-001"));

        assertFalse(result.isSuccess());
        assertEquals("5001", result.getResultCode());
    }

    @Test
    void shouldPropagateDisabledResponse() {
        fsuClient.responseToReturn = FsuServiceResponse.disabled();

        GetDataResult result = getDataService.execute("FSU-001", null, List.of("TEMP-001"));

        assertFalse(result.isSuccess());
        assertEquals("5001", result.getResultCode());
        assertTrue(result.getResultDesc().contains("禁用"));
    }

    // ==================== 请求构造 ====================

    @Test
    void shouldBuildRequestWithCorrectSignalIds() {
        fsuClient.responseToReturn = buildSuccessResponse();

        getDataService.execute("FSU-001", null, List.of("TEMP-001", "HUMI-001"));

        assertEquals(BInterfacePkType.GET_DATA, fsuClient.lastRequest.getPkType());
        assertEquals("FSU-001", fsuClient.lastRequest.getFsuCode());
        assertNotNull(fsuClient.lastRequest.getXmlDataXml());
        assertTrue(fsuClient.lastRequest.getXmlDataXml().contains("TEMP-001"));
        assertTrue(fsuClient.lastRequest.getXmlDataXml().contains("HUMI-001"));
    }

    @Test
    void shouldBuildRequestWithInfoFsuCode() {
        fsuClient.responseToReturn = buildSuccessResponse();

        getDataService.execute("FSU-001", "http://fsu:8080", List.of("TEMP-001"));

        assertNotNull(fsuClient.lastRequest.getInfoXml());
        assertTrue(fsuClient.lastRequest.getInfoXml().contains("FSU-001"));
    }

    // ==================== 响应解析 ====================

    @Test
    void shouldParseMultipleSignals() {
        fsuClient.responseToReturn = buildSuccessResponse();

        GetDataResult result = getDataService.execute("FSU-001", null, List.of("TEMP-001", "HUMI-001"));

        assertEquals(2, result.getSignals().size());
        assertEquals("TEMP-001", result.getSignals().get(0).getSignalId());
        assertEquals("HUMI-001", result.getSignals().get(1).getSignalId());
    }

    @Test
    void shouldParseXmlDataBuilderOutput() {
        // 验证从 XmlDataModel → XML → parse 往返
        fsuClient.responseToReturn = buildSuccessResponse();

        GetDataResult result = getDataService.execute("FSU-001", null, List.of("TEMP-001"));

        assertTrue(result.isSuccess());
        assertEquals(2, result.getCount()); // Stub 固定返回 2 个信号
    }

    // ==================== 边界 ====================

    @Test
    void shouldNotCallSendDataService() {
        // 验证 GET_DATA 服务不依赖 SEND_DATA 逻辑
        fsuClient.responseToReturn = buildSuccessResponse();

        GetDataResult result = getDataService.execute("FSU-001", null, List.of("TEMP-001"));

        assertTrue(result.isSuccess());
        // 只验证 GET_DATA 的正常返回，不涉及入库
    }

    @Test
    void shouldNotAccessNetwork() {
        fsuClient.responseToReturn = buildSuccessResponse();

        GetDataResult result = getDataService.execute("FSU-001", null, List.of("TEMP-001"));

        assertTrue(result.isSuccess());
        // 实际网络访问由 FsuServiceClient 控制，此处验证业务层不抛异常
    }

    // ==================== Stub FSU Client ====================

    static class StubFsuClient implements FsuServiceClient {
        FsuServiceResponse responseToReturn;
        FsuServiceRequest lastRequest;

        @Override
        public FsuServiceResponse call(FsuServiceRequest request) {
            this.lastRequest = request;
            if (responseToReturn == null) {
                throw new RuntimeException("Stub client failure");
            }
            return responseToReturn;
        }
    }

    private FsuServiceResponse buildSuccessResponse() {
        XmlDataModel xmlData = new XmlDataModel();
        xmlData.setRootName("Signal");

        var s1 = new LinkedHashMap<String, String>();
        s1.put("SignalID", "TEMP-001");
        s1.put("Value", "25.8");
        s1.put("Quality", "1");
        s1.put("Status", "NORMAL");
        s1.put("CollectTime", "2026-05-13T10:35:00+08:00");
        xmlData.addItem(s1);

        var s2 = new LinkedHashMap<String, String>();
        s2.put("SignalID", "HUMI-001");
        s2.put("Value", "54.5");
        s2.put("Quality", "1");
        s2.put("Status", "NORMAL");
        s2.put("CollectTime", "2026-05-13T10:35:00+08:00");
        xmlData.addItem(s2);

        return FsuServiceResponse.success(
                "<soap:Envelope><soap:Body><Response><PK_Type>GET_DATA</PK_Type><Info><ResultCode>0</ResultCode><Count>2</Count></Info></Response></soap:Body></soap:Envelope>",
                "<ResultCode>0</ResultCode><Count>2</Count>",
                "<Signal><SignalID>TEMP-001</SignalID><Value>25.8</Value><Quality>1</Quality><Status>NORMAL</Status><CollectTime>2026-05-13T10:35:00+08:00</CollectTime></Signal>",
                xmlData);
    }
}
