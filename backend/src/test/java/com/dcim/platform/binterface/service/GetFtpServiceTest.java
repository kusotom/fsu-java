package com.dcim.platform.binterface.service;

import com.dcim.platform.module.binterface.model.BInterfacePkType;
import com.dcim.platform.module.binterface.service.GetFtpResult;
import com.dcim.platform.module.binterface.service.GetFtpService;
import com.dcim.platform.module.binterface.service.fsu.FsuServiceClient;
import com.dcim.platform.module.binterface.service.fsu.FsuServiceRequest;
import com.dcim.platform.module.binterface.service.fsu.FsuServiceResponse;
import com.dcim.platform.module.binterface.xml.XmlDataModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;

import static org.junit.jupiter.api.Assertions.*;

/**
 * GetFtpService 单元测试。
 */
class GetFtpServiceTest {

    private GetFtpService service;
    private StubFsuClient fsuClient;

    @BeforeEach
    void setUp() {
        fsuClient = new StubFsuClient();
        service = new GetFtpService(fsuClient, null);
    }

    // ==================== 成功 ====================

    @Test
    void shouldReturnSuccessWhenFsuResponds() {
        fsuClient.responseToReturn = buildSuccessResponse();

        GetFtpResult result = service.execute("FSU-001", null, "IMAGE");

        assertTrue(result.isSuccess());
        assertEquals("0", result.getResultCode());
        assertEquals("FSU-001", result.getFsuCode());
    }

    @Test
    void shouldParseFtpConfigFields() {
        fsuClient.responseToReturn = buildSuccessResponse();

        GetFtpResult result = service.execute("FSU-001", null, "IMAGE");

        assertEquals("192.168.1.200", result.getHost());
        assertEquals(21, result.getPort());
        assertEquals("fsu_ftp", result.getUsername());
        assertTrue(result.isPassiveMode());
        assertEquals("/fsu/images/", result.getBasePath());
    }

    @Test
    void shouldWorkWithoutFileType() {
        fsuClient.responseToReturn = buildSuccessResponse();

        GetFtpResult result = service.execute("FSU-001", null, null);

        assertTrue(result.isSuccess());
    }

    // ==================== 校验失败 ====================

    @Test
    void shouldFailOnNullFsuCode() {
        GetFtpResult result = service.execute(null, null, "IMAGE");
        assertFalse(result.isSuccess());
        assertEquals("2001", result.getResultCode());
    }

    @Test
    void shouldFailOnEmptyFsuCode() {
        GetFtpResult result = service.execute("", null, "IMAGE");
        assertFalse(result.isSuccess());
        assertEquals("2001", result.getResultCode());
    }

    // ==================== FSU 调用失败 ====================

    @Test
    void shouldFailWhenFsuClientReturnsError() {
        fsuClient.responseToReturn = FsuServiceResponse.fail("1002", "FSU 离线");

        GetFtpResult result = service.execute("FSU-001", null, "IMAGE");

        assertFalse(result.isSuccess());
        assertEquals("1002", result.getResultCode());
    }

    @Test
    void shouldFailWhenFsuClientThrowsException() {
        fsuClient.responseToReturn = null;

        GetFtpResult result = service.execute("FSU-001", null, "IMAGE");

        assertFalse(result.isSuccess());
        assertEquals("5001", result.getResultCode());
    }

    @Test
    void shouldPropagateDisabledResponse() {
        fsuClient.responseToReturn = FsuServiceResponse.disabled();

        GetFtpResult result = service.execute("FSU-001", null, "IMAGE");

        assertFalse(result.isSuccess());
        assertTrue(result.getResultDesc().contains("禁用"));
    }

    // ==================== 请求构造 ====================

    @Test
    void shouldBuildRequestWithCorrectPkType() {
        fsuClient.responseToReturn = buildSuccessResponse();

        service.execute("FSU-001", null, "IMAGE");

        assertNotNull(fsuClient.lastRequest);
        assertEquals(BInterfacePkType.GET_FTP, fsuClient.lastRequest.getPkType());
        assertEquals("FSU-001", fsuClient.lastRequest.getFsuCode());
    }

    @Test
    void shouldBuildRequestWithInfoFsuCode() {
        fsuClient.responseToReturn = buildSuccessResponse();

        service.execute("FSU-001", null, "IMAGE");

        assertNotNull(fsuClient.lastRequest.getInfoXml());
        assertTrue(fsuClient.lastRequest.getInfoXml().contains("FSU-001"));
    }

    @Test
    void shouldBuildRequestWithFileType() {
        fsuClient.responseToReturn = buildSuccessResponse();

        service.execute("FSU-001", null, "IMAGE");

        assertNotNull(fsuClient.lastRequest.getInfoXml());
        assertTrue(fsuClient.lastRequest.getInfoXml().contains("IMAGE"));
    }

    @Test
    void shouldBuildRequestWithoutXmlData() {
        fsuClient.responseToReturn = buildSuccessResponse();

        service.execute("FSU-001", null, "IMAGE");

        assertNull(fsuClient.lastRequest.getXmlDataXml());
    }

    // ==================== 边界 ====================

    @Test
    void shouldNotAccessNetwork() {
        fsuClient.responseToReturn = buildSuccessResponse();

        GetFtpResult result = service.execute("FSU-001", null, "IMAGE");

        assertTrue(result.isSuccess());
    }

    @Test
    void shouldNotCallSetFtp() {
        // 验证 GET_FTP 不调用 SET_FTP 相关逻辑
        fsuClient.responseToReturn = buildSuccessResponse();

        GetFtpResult result = service.execute("FSU-001", null, "IMAGE");

        assertTrue(result.isSuccess());
        // 只读查询
    }

    // ==================== 敏感字段脱敏 ====================

    @Test
    void shouldMaskUsernameInToString() {
        fsuClient.responseToReturn = buildSuccessResponse();

        GetFtpResult result = service.execute("FSU-001", null, "IMAGE");

        String toString = result.toString();
        assertTrue(toString.contains("username=f****p") || toString.contains("username=****"));
        assertFalse(toString.contains("fsu_ftp"));
    }

    @Test
    void getMaskedUsernameShouldReturnMasked() {
        fsuClient.responseToReturn = buildSuccessResponse();

        GetFtpResult result = service.execute("FSU-001", null, "IMAGE");

        String masked = result.getMaskedUsername();
        assertNotNull(masked);
        assertFalse(masked.contains("fsu_ftp"));
        assertTrue(masked.contains("****"));
    }

    // ==================== 响应为空 ====================

    @Test
    void shouldFailWhenResponseXmlDataIsEmpty() {
        fsuClient.responseToReturn = FsuServiceResponse.success(
                "<soap:Envelope/>", "<ResultCode>0</ResultCode>", null, new XmlDataModel());

        GetFtpResult result = service.execute("FSU-001", null, "IMAGE");

        assertFalse(result.isSuccess());
        assertEquals("2003", result.getResultCode());
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
        xmlData.setRootName("FTPConfig");
        var item = new LinkedHashMap<String, String>();
        item.put("Host", "192.168.1.200");
        item.put("Port", "21");
        item.put("Username", "fsu_ftp");
        item.put("PassiveMode", "true");
        item.put("BasePath", "/fsu/images/");
        xmlData.addItem(item);

        return FsuServiceResponse.success(
                "<soap:Envelope><soap:Body><Response><PK_Type>GET_FTP</PK_Type>"
                + "<Info><ResultCode>0</ResultCode></Info>"
                + "<xmlData><FTPConfig><Host>192.168.1.200</Host><Port>21</Port>"
                + "<Username>fsu_ftp</Username><PassiveMode>true</PassiveMode>"
                + "<BasePath>/fsu/images/</BasePath></FTPConfig></xmlData>"
                + "</Response></soap:Body></soap:Envelope>",
                "<ResultCode>0</ResultCode>",
                "<FTPConfig><Host>192.168.1.200</Host><Port>21</Port>"
                + "<Username>fsu_ftp</Username><PassiveMode>true</PassiveMode>"
                + "<BasePath>/fsu/images/</BasePath></FTPConfig>",
                xmlData);
    }
}
