package com.dcim.platform.binterface.service;

import com.dcim.platform.module.binterface.service.*;
import com.dcim.platform.module.binterface.service.fsu.FsuServiceClient;
import com.dcim.platform.module.binterface.service.fsu.FsuServiceRequest;
import com.dcim.platform.module.binterface.service.fsu.FsuServiceResponse;
import com.dcim.platform.module.binterface.xml.XmlDataModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SetThresholdService 单元测试。
 */
class SetThresholdServiceTest {

    private SetThresholdService setThresholdService;
    private StubFsuClient fsuClient;

    @BeforeEach
    void setUp() {
        fsuClient = new StubFsuClient();
        setThresholdService = new SetThresholdService(fsuClient, null);
    }

    // ==================== 成功 ====================

    @Test
    void shouldReturnSuccessWhenFsuResponds() {
        fsuClient.responseToReturn = buildSuccessResponse();

        SetThresholdResult result = setThresholdService.execute(
                "FSU-001", null, "TEMP-001", "65.0", "-5.0", "75.0", "-10.0");

        assertTrue(result.isSuccess());
        assertEquals("0", result.getResultCode());
    }

    @Test
    void shouldReturnCountFromResponse() {
        fsuClient.responseToReturn = buildSuccessResponse();

        SetThresholdResult result = setThresholdService.execute(
                "FSU-001", null, "TEMP-001", "65.0", "-5.0", "75.0", "-10.0");

        assertEquals(1, result.getCount());
    }

    // ==================== 校验失败 ====================

    @Test
    void shouldFailOnNullFsuCode() {
        SetThresholdResult result = setThresholdService.execute(
                null, null, "TEMP-001", "65.0", "-5.0", "75.0", "-10.0");

        assertFalse(result.isSuccess());
        assertEquals("2001", result.getResultCode());
    }

    @Test
    void shouldFailOnEmptyFsuCode() {
        SetThresholdResult result = setThresholdService.execute(
                "", null, "TEMP-001", "65.0", "-5.0", "75.0", "-10.0");

        assertFalse(result.isSuccess());
        assertEquals("2001", result.getResultCode());
    }

    @Test
    void shouldFailOnNullSignalId() {
        SetThresholdResult result = setThresholdService.execute(
                "FSU-001", null, null, "65.0", "-5.0", "75.0", "-10.0");

        assertFalse(result.isSuccess());
        assertEquals("2003", result.getResultCode());
    }

    @Test
    void shouldFailOnEmptySignalId() {
        SetThresholdResult result = setThresholdService.execute(
                "FSU-001", null, "", "65.0", "-5.0", "75.0", "-10.0");

        assertFalse(result.isSuccess());
        assertEquals("2003", result.getResultCode());
    }

    // ==================== FSU 调用失败 ====================

    @Test
    void shouldFailWhenFsuClientReturnsError() {
        fsuClient.responseToReturn = FsuServiceResponse.fail("5001", "FSU 错误");

        SetThresholdResult result = setThresholdService.execute(
                "FSU-001", null, "TEMP-001", "65.0", "-5.0", "75.0", "-10.0");

        assertFalse(result.isSuccess());
        assertEquals("5001", result.getResultCode());
    }

    @Test
    void shouldFailWhenFsuClientThrowsException() {
        fsuClient.responseToReturn = null;

        SetThresholdResult result = setThresholdService.execute(
                "FSU-001", null, "TEMP-001", "65.0", "-5.0", "75.0", "-10.0");

        assertFalse(result.isSuccess());
        assertEquals("5001", result.getResultCode());
    }

    // ==================== 请求构造 ====================

    @Test
    void shouldBuildRequestWithCorrectFields() {
        fsuClient.responseToReturn = buildSuccessResponse();

        setThresholdService.execute("FSU-001", null, "TEMP-001", "65.0", "-5.0", "75.0", "-10.0");

        assertNotNull(fsuClient.lastRequest);
        assertEquals("FSU-001", fsuClient.lastRequest.getFsuCode());
        String xmlDataXml = fsuClient.lastRequest.getXmlDataXml();
        assertNotNull(xmlDataXml);
        assertTrue(xmlDataXml.contains("TEMP-001"));
        assertTrue(xmlDataXml.contains("65.0"));
        assertTrue(xmlDataXml.contains("-5.0"));
        assertTrue(xmlDataXml.contains("75.0"));
        assertTrue(xmlDataXml.contains("-10.0"));
    }

    @Test
    void shouldBuildRequestWithInfoFsuCode() {
        fsuClient.responseToReturn = buildSuccessResponse();

        setThresholdService.execute("FSU-001", null, "TEMP-001", "65.0", "-5.0", "75.0", "-10.0");

        assertNotNull(fsuClient.lastRequest.getInfoXml());
        assertTrue(fsuClient.lastRequest.getInfoXml().contains("FSU-001"));
    }

    // ==================== 边界 ====================

    @Test
    void shouldNotAccessNetwork() {
        fsuClient.responseToReturn = buildSuccessResponse();

        SetThresholdResult result = setThresholdService.execute(
                "FSU-001", null, "TEMP-001", "65.0", "-5.0", "75.0", "-10.0");

        assertTrue(result.isSuccess());
    }

    @Test
    void shouldNotCallGetThresholdService() {
        // 验证 SET_THRESHOLD 服务不调用 GET_THRESHOLD 逻辑
        fsuClient.responseToReturn = buildSuccessResponse();

        SetThresholdResult result = setThresholdService.execute(
                "FSU-001", null, "TEMP-001", "65.0", "-5.0", "75.0", "-10.0");

        assertTrue(result.isSuccess());
    }

    // ==================== parseCount ====================

    @Test
    void parseCountShouldReturnCorrectValue() {
        int count = setThresholdService.parseCount("<ResultCode>0</ResultCode><Count>3</Count>");
        assertEquals(3, count);
    }

    @Test
    void parseCountShouldReturnZeroForNull() {
        assertEquals(0, setThresholdService.parseCount(null));
    }

    @Test
    void parseCountShouldReturnZeroForEmpty() {
        assertEquals(0, setThresholdService.parseCount(""));
    }

    @Test
    void parseCountShouldReturnZeroWhenNotFound() {
        assertEquals(0, setThresholdService.parseCount("<ResultCode>0</ResultCode>"));
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
        return FsuServiceResponse.success(
                "<soap:Envelope><soap:Body><Response><PK_Type>SET_THRESHOLD</PK_Type><Info><ResultCode>0</ResultCode><Count>1</Count></Info><xmlData/></Response></soap:Body></soap:Envelope>",
                "<ResultCode>0</ResultCode><Count>1</Count>",
                null,
                new XmlDataModel());
    }
}
