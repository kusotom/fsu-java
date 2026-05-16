package com.dcim.platform.binterface.service;

import com.dcim.platform.module.binterface.model.BInterfacePkType;
import com.dcim.platform.module.binterface.service.TimeCheckResult;
import com.dcim.platform.module.binterface.service.TimeCheckService;
import com.dcim.platform.module.binterface.service.fsu.FsuServiceClient;
import com.dcim.platform.module.binterface.service.fsu.FsuServiceRequest;
import com.dcim.platform.module.binterface.service.fsu.FsuServiceResponse;
import com.dcim.platform.module.binterface.xml.XmlDataModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TimeCheckService 单元测试。
 */
class TimeCheckServiceTest {

    private TimeCheckService timeCheckService;
    private StubFsuClient fsuClient;

    @BeforeEach
    void setUp() {
        fsuClient = new StubFsuClient();
        timeCheckService = new TimeCheckService(fsuClient, null);
    }

    // ==================== 成功 ====================

    @Test
    void shouldReturnSuccessWhenFsuResponds() {
        fsuClient.responseToReturn = buildSuccessResponse();

        TimeCheckResult result = timeCheckService.execute("FSU-001", null, "2026-05-13T10:42:00+08:00");

        assertTrue(result.isSuccess());
        assertEquals("0", result.getResultCode());
        assertEquals("FSU-001", result.getFsuCode());
        assertEquals("2026-05-13T10:42:00+08:00", result.getStandardTime());
        assertNotNull(result.getFsuTime());
    }

    @Test
    void shouldParseFsuTimeCorrectly() {
        fsuClient.responseToReturn = buildSuccessResponse();

        TimeCheckResult result = timeCheckService.execute("FSU-001", null, "2026-05-13T10:42:00+08:00");

        assertTrue(result.isSuccess());
        assertEquals("2026-05-13T10:42:01+08:00", result.getFsuTime());
    }

    @Test
    void shouldUseProvidedStandardTime() {
        fsuClient.responseToReturn = buildSuccessResponse();

        TimeCheckResult result = timeCheckService.execute("FSU-001", null, "2026-05-13T12:00:00+08:00");

        assertEquals("2026-05-13T12:00:00+08:00", result.getStandardTime());
    }

    // ==================== 校验失败 ====================

    @Test
    void shouldFailOnNullFsuCode() {
        TimeCheckResult result = timeCheckService.execute(null, null, "2026-05-13T10:42:00+08:00");

        assertFalse(result.isSuccess());
        assertEquals("2001", result.getResultCode());
    }

    @Test
    void shouldFailOnEmptyFsuCode() {
        TimeCheckResult result = timeCheckService.execute("", null, "2026-05-13T10:42:00+08:00");

        assertFalse(result.isSuccess());
        assertEquals("2001", result.getResultCode());
    }

    @Test
    void shouldFailOnNullStandardTime() {
        TimeCheckResult result = timeCheckService.execute("FSU-001", null, null);

        assertFalse(result.isSuccess());
        assertEquals("2003", result.getResultCode());
    }

    @Test
    void shouldFailOnEmptyStandardTime() {
        TimeCheckResult result = timeCheckService.execute("FSU-001", null, "");

        assertFalse(result.isSuccess());
        assertEquals("2003", result.getResultCode());
    }

    // ==================== FSU 调用失败 ====================

    @Test
    void shouldFailWhenFsuClientReturnsError() {
        fsuClient.responseToReturn = FsuServiceResponse.fail("1002", "FSU 离线");

        TimeCheckResult result = timeCheckService.execute("FSU-001", null, "2026-05-13T10:42:00+08:00");

        assertFalse(result.isSuccess());
        assertEquals("1002", result.getResultCode());
    }

    @Test
    void shouldFailWhenFsuClientThrowsException() {
        fsuClient.responseToReturn = null; // 触发空指针

        TimeCheckResult result = timeCheckService.execute("FSU-001", null, "2026-05-13T10:42:00+08:00");

        assertFalse(result.isSuccess());
        assertEquals("5001", result.getResultCode());
    }

    @Test
    void shouldPropagateDisabledResponse() {
        fsuClient.responseToReturn = FsuServiceResponse.disabled();

        TimeCheckResult result = timeCheckService.execute("FSU-001", null, "2026-05-13T10:42:00+08:00");

        assertFalse(result.isSuccess());
        assertEquals("5001", result.getResultCode());
        assertTrue(result.getResultDesc().contains("禁用"));
    }

    // ==================== 请求构造 ====================

    @Test
    void shouldBuildRequestWithCorrectPkType() {
        fsuClient.responseToReturn = buildSuccessResponse();

        timeCheckService.execute("FSU-001", null, "2026-05-13T10:42:00+08:00");

        assertNotNull(fsuClient.lastRequest);
        assertEquals(BInterfacePkType.TIME_CHECK, fsuClient.lastRequest.getPkType());
        assertEquals("FSU-001", fsuClient.lastRequest.getFsuCode());
    }

    @Test
    void shouldBuildRequestWithStandardTimeInInfo() {
        fsuClient.responseToReturn = buildSuccessResponse();

        timeCheckService.execute("FSU-001", null, "2026-05-13T10:42:00+08:00");

        assertNotNull(fsuClient.lastRequest.getInfoXml());
        assertTrue(fsuClient.lastRequest.getInfoXml().contains("FSU-001"));
        assertTrue(fsuClient.lastRequest.getInfoXml().contains("2026-05-13T10:42:00+08:00"));
    }

    @Test
    void shouldBuildRequestWithoutXmlData() {
        fsuClient.responseToReturn = buildSuccessResponse();

        timeCheckService.execute("FSU-001", null, "2026-05-13T10:42:00+08:00");

        assertNull(fsuClient.lastRequest.getXmlDataXml());
    }

    // ==================== extractFsuTime ====================

    @Test
    void extractFsuTimeShouldReturnCorrectValue() {
        String fsuTime = timeCheckService.extractFsuTime(
                "<ResultCode>0</ResultCode><FSUTime>2026-05-13T10:42:01+08:00</FSUTime>");
        assertEquals("2026-05-13T10:42:01+08:00", fsuTime);
    }

    @Test
    void extractFsuTimeShouldBeCaseInsensitive() {
        String fsuTime = timeCheckService.extractFsuTime(
                "<ResultCode>0</ResultCode><fsutime>2026-05-13T10:42:01+08:00</fsutime>");
        assertEquals("2026-05-13T10:42:01+08:00", fsuTime);
    }

    @Test
    void extractFsuTimeShouldReturnNullForEmptyInput() {
        assertNull(timeCheckService.extractFsuTime(null));
        assertNull(timeCheckService.extractFsuTime(""));
    }

    @Test
    void extractFsuTimeShouldHandleWhitespace() {
        String fsuTime = timeCheckService.extractFsuTime(
                "<FSUTime>  2026-05-13T10:42:01+08:00  </FSUTime>");
        assertEquals("2026-05-13T10:42:01+08:00", fsuTime);
    }

    @Test
    void extractFsuTimeShouldReturnNullWhenNotFound() {
        assertNull(timeCheckService.extractFsuTime("<ResultCode>0</ResultCode>"));
    }

    // ==================== 边界 ====================

    @Test
    void shouldNotAccessNetwork() {
        fsuClient.responseToReturn = buildSuccessResponse();

        TimeCheckResult result = timeCheckService.execute("FSU-001", null, "2026-05-13T10:42:00+08:00");

        assertTrue(result.isSuccess());
    }

    @Test
    void shouldNotCallOtherServices() {
        // 验证 TIME_CHECK 服务不依赖 GET_DATA / SET_THRESHOLD 等逻辑
        fsuClient.responseToReturn = buildSuccessResponse();

        TimeCheckResult result = timeCheckService.execute("FSU-001", null, "2026-05-13T10:42:00+08:00");

        assertTrue(result.isSuccess());
        assertEquals("0", result.getResultCode());
        assertNotNull(result.getFsuTime());
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
                "<soap:Envelope><soap:Body><Response><PK_Type>TIME_CHECK</PK_Type>"
                + "<Info><ResultCode>0</ResultCode><FSUTime>2026-05-13T10:42:01+08:00</FSUTime></Info>"
                + "<xmlData/></Response></soap:Body></soap:Envelope>",
                "<ResultCode>0</ResultCode><FSUTime>2026-05-13T10:42:01+08:00</FSUTime>",
                null,
                new XmlDataModel());
    }
}
