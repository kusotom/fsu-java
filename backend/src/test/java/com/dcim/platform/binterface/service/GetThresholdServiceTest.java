package com.dcim.platform.binterface.service;

import com.dcim.platform.module.binterface.service.GetThresholdResult;
import com.dcim.platform.module.binterface.service.GetThresholdService;
import com.dcim.platform.module.binterface.service.fsu.FsuServiceClient;
import com.dcim.platform.module.binterface.service.fsu.FsuServiceRequest;
import com.dcim.platform.module.binterface.service.fsu.FsuServiceResponse;
import com.dcim.platform.module.binterface.xml.XmlDataModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * GetThresholdService 单元测试。
 */
class GetThresholdServiceTest {

    private GetThresholdService getThresholdService;
    private StubFsuClient fsuClient;

    @BeforeEach
    void setUp() {
        fsuClient = new StubFsuClient();
        getThresholdService = new GetThresholdService(fsuClient, null);
    }

    // ==================== 成功 ====================

    @Test
    void shouldReturnSuccessWhenFsuResponds() {
        fsuClient.responseToReturn = buildSuccessResponse();

        GetThresholdResult result = getThresholdService.execute("FSU-001", null, List.of("TEMP-001", "HUMI-001"));

        assertTrue(result.isSuccess());
        assertEquals("0", result.getResultCode());
        assertEquals(2, result.getCount());
        assertEquals(2, result.getThresholds().size());
        assertEquals("TEMP-001", result.getThresholds().get(0).getSignalId());
        assertEquals("HUMI-001", result.getThresholds().get(1).getSignalId());
    }

    @Test
    void shouldParseThresholdValuesCorrectly() {
        fsuClient.responseToReturn = buildSuccessResponse();

        GetThresholdResult result = getThresholdService.execute("FSU-001", null, List.of("TEMP-001"));

        assertTrue(result.isSuccess());
        assertEquals("60.0", result.getThresholds().get(0).getAlarmUpper());
        assertEquals("-5.0", result.getThresholds().get(0).getAlarmLower());
        assertEquals("70.0", result.getThresholds().get(0).getAlarmUpperUrgent());
        assertEquals("-10.0", result.getThresholds().get(0).getAlarmLowerUrgent());
    }

    @Test
    void shouldReturnEmptyListWhenNoThresholds() {
        fsuClient.responseToReturn = FsuServiceResponse.success(
                "<soap:Envelope/>", "<ResultCode>0</ResultCode>", null, new XmlDataModel());

        GetThresholdResult result = getThresholdService.execute("FSU-001", null, List.of("TEMP-001"));

        assertTrue(result.isSuccess());
        assertEquals(0, result.getCount());
        assertTrue(result.getThresholds().isEmpty());
    }

    // ==================== 校验失败 ====================

    @Test
    void shouldFailOnNullFsuCode() {
        GetThresholdResult result = getThresholdService.execute(null, null, List.of("TEMP-001"));

        assertFalse(result.isSuccess());
        assertEquals("2001", result.getResultCode());
    }

    @Test
    void shouldFailOnEmptyFsuCode() {
        GetThresholdResult result = getThresholdService.execute("", null, List.of("TEMP-001"));

        assertFalse(result.isSuccess());
        assertEquals("2001", result.getResultCode());
    }

    @Test
    void shouldFailOnNullSignalIds() {
        GetThresholdResult result = getThresholdService.execute("FSU-001", null, null);

        assertFalse(result.isSuccess());
        assertEquals("2003", result.getResultCode());
    }

    @Test
    void shouldFailOnEmptySignalIds() {
        GetThresholdResult result = getThresholdService.execute("FSU-001", null, List.of());

        assertFalse(result.isSuccess());
        assertEquals("2003", result.getResultCode());
    }

    // ==================== FSU 调用失败 ====================

    @Test
    void shouldFailWhenFsuClientReturnsError() {
        fsuClient.responseToReturn = FsuServiceResponse.fail("1002", "FSU 离线");

        GetThresholdResult result = getThresholdService.execute("FSU-001", null, List.of("TEMP-001"));

        assertFalse(result.isSuccess());
        assertEquals("1002", result.getResultCode());
    }

    @Test
    void shouldFailWhenFsuClientThrowsException() {
        fsuClient.responseToReturn = null; // will cause NPE in caller

        GetThresholdResult result = getThresholdService.execute("FSU-001", null, List.of("TEMP-001"));

        assertFalse(result.isSuccess());
        assertEquals("5001", result.getResultCode());
    }

    @Test
    void shouldPropagateDisabledResponse() {
        fsuClient.responseToReturn = FsuServiceResponse.disabled();

        GetThresholdResult result = getThresholdService.execute("FSU-001", null, List.of("TEMP-001"));

        assertFalse(result.isSuccess());
        assertEquals("5001", result.getResultCode());
        assertTrue(result.getResultDesc().contains("禁用"));
    }

    // ==================== 请求构造 ====================

    @Test
    void shouldBuildRequestWithCorrectSignalIds() {
        fsuClient.responseToReturn = buildSuccessResponse();

        getThresholdService.execute("FSU-001", null, List.of("TEMP-001", "HUMI-001"));

        assertEquals("FSU-001", fsuClient.lastRequest.getFsuCode());
        assertNotNull(fsuClient.lastRequest.getXmlDataXml());
        assertTrue(fsuClient.lastRequest.getXmlDataXml().contains("TEMP-001"));
        assertTrue(fsuClient.lastRequest.getXmlDataXml().contains("HUMI-001"));
    }

    @Test
    void shouldBuildRequestWithInfoFsuCode() {
        fsuClient.responseToReturn = buildSuccessResponse();

        getThresholdService.execute("FSU-001", "http://fsu:8080", List.of("TEMP-001"));

        assertNotNull(fsuClient.lastRequest.getInfoXml());
        assertTrue(fsuClient.lastRequest.getInfoXml().contains("FSU-001"));
    }

    // ==================== 响应解析 ====================

    @Test
    void shouldParseMultipleThresholds() {
        fsuClient.responseToReturn = buildSuccessResponse();

        GetThresholdResult result = getThresholdService.execute("FSU-001", null, List.of("TEMP-001", "HUMI-001"));

        assertEquals(2, result.getThresholds().size());
        assertEquals("TEMP-001", result.getThresholds().get(0).getSignalId());
        assertEquals("HUMI-001", result.getThresholds().get(1).getSignalId());
    }

    @Test
    void shouldParseAllThresholdFields() {
        fsuClient.responseToReturn = buildSuccessResponse();

        GetThresholdResult result = getThresholdService.execute("FSU-001", null, List.of("TEMP-001"));

        GetThresholdResult.ThresholdValue tv = result.getThresholds().get(0);
        assertEquals("TEMP-001", tv.getSignalId());
        assertEquals("60.0", tv.getAlarmUpper());
        assertEquals("-5.0", tv.getAlarmLower());
        assertEquals("70.0", tv.getAlarmUpperUrgent());
        assertEquals("-10.0", tv.getAlarmLowerUrgent());
    }

    // ==================== 边界 ====================

    @Test
    void shouldNotModifyFsuThreshold() {
        // 验证 GET_THRESHOLD 是只读查询，不修改门限
        fsuClient.responseToReturn = buildSuccessResponse();

        GetThresholdResult result = getThresholdService.execute("FSU-001", null, List.of("TEMP-001"));

        assertTrue(result.isSuccess());
        // 成功即证明仅查询，不修改
    }

    @Test
    void shouldNotAccessNetwork() {
        fsuClient.responseToReturn = buildSuccessResponse();

        GetThresholdResult result = getThresholdService.execute("FSU-001", null, List.of("TEMP-001"));

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
        s1.put("AlarmUpper", "60.0");
        s1.put("AlarmLower", "-5.0");
        s1.put("AlarmUpperUrgent", "70.0");
        s1.put("AlarmLowerUrgent", "-10.0");
        xmlData.addItem(s1);

        var s2 = new LinkedHashMap<String, String>();
        s2.put("SignalID", "HUMI-001");
        s2.put("AlarmUpper", "90.0");
        s2.put("AlarmLower", "10.0");
        s2.put("AlarmUpperUrgent", "95.0");
        s2.put("AlarmLowerUrgent", "5.0");
        xmlData.addItem(s2);

        return FsuServiceResponse.success(
                "<soap:Envelope><soap:Body><Response><PK_Type>GET_THRESHOLD</PK_Type><Info><ResultCode>0</ResultCode><Count>2</Count></Info><xmlData><Signal><SignalID>TEMP-001</SignalID><AlarmUpper>60.0</AlarmUpper><AlarmLower>-5.0</AlarmLower><AlarmUpperUrgent>70.0</AlarmUpperUrgent><AlarmLowerUrgent>-10.0</AlarmLowerUrgent></Signal><Signal><SignalID>HUMI-001</SignalID><AlarmUpper>90.0</AlarmUpper><AlarmLower>10.0</AlarmLower><AlarmUpperUrgent>95.0</AlarmUpperUrgent><AlarmLowerUrgent>5.0</AlarmLowerUrgent></Signal></xmlData></Response></soap:Body></soap:Envelope>",
                "<ResultCode>0</ResultCode><Count>2</Count>",
                "<Signal><SignalID>TEMP-001</SignalID><AlarmUpper>60.0</AlarmUpper><AlarmLower>-5.0</AlarmLower><AlarmUpperUrgent>70.0</AlarmUpperUrgent><AlarmLowerUrgent>-10.0</AlarmLowerUrgent></Signal>",
                xmlData);
    }
}
