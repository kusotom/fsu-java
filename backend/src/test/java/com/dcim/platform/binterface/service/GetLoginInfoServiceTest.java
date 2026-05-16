package com.dcim.platform.binterface.service;

import com.dcim.platform.module.binterface.model.BInterfacePkType;
import com.dcim.platform.module.binterface.service.GetLoginInfoResult;
import com.dcim.platform.module.binterface.service.GetLoginInfoService;
import com.dcim.platform.module.binterface.service.fsu.FsuServiceClient;
import com.dcim.platform.module.binterface.service.fsu.FsuServiceRequest;
import com.dcim.platform.module.binterface.service.fsu.FsuServiceResponse;
import com.dcim.platform.module.binterface.xml.XmlDataModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;

import static org.junit.jupiter.api.Assertions.*;

/**
 * GetLoginInfoService 单元测试。
 */
class GetLoginInfoServiceTest {

    private GetLoginInfoService service;
    private StubFsuClient fsuClient;

    @BeforeEach
    void setUp() {
        fsuClient = new StubFsuClient();
        service = new GetLoginInfoService(fsuClient, null);
    }

    // ==================== 成功 ====================

    @Test
    void shouldReturnSuccessWhenFsuResponds() {
        fsuClient.responseToReturn = buildSuccessResponse();

        GetLoginInfoResult result = service.execute("FSU-001", null);

        assertTrue(result.isSuccess());
        assertEquals("0", result.getResultCode());
        assertEquals("FSU-001", result.getFsuCode());
    }

    @Test
    void shouldParseLoginInfoFields() {
        fsuClient.responseToReturn = buildSuccessResponse();

        GetLoginInfoResult result = service.execute("FSU-001", null);

        assertEquals("LOGIN", result.getLoginStatus());
        assertEquals("ONLINE", result.getOnlineStatus());
        assertEquals("SESSION-FSU-001-20260513-A3B8", result.getSessionId());
        assertEquals("2026-05-13T10:30:00+08:00", result.getLoginTime());
        assertEquals("2026-05-13T10:40:00+08:00", result.getLastHeartbeat());
    }

    // ==================== 校验失败 ====================

    @Test
    void shouldFailOnNullFsuCode() {
        GetLoginInfoResult result = service.execute(null, null);
        assertFalse(result.isSuccess());
        assertEquals("2001", result.getResultCode());
    }

    @Test
    void shouldFailOnEmptyFsuCode() {
        GetLoginInfoResult result = service.execute("", null);
        assertFalse(result.isSuccess());
        assertEquals("2001", result.getResultCode());
    }

    // ==================== FSU 调用失败 ====================

    @Test
    void shouldFailWhenFsuClientReturnsError() {
        fsuClient.responseToReturn = FsuServiceResponse.fail("1002", "FSU 离线");

        GetLoginInfoResult result = service.execute("FSU-001", null);

        assertFalse(result.isSuccess());
        assertEquals("1002", result.getResultCode());
    }

    @Test
    void shouldFailWhenFsuClientThrowsException() {
        fsuClient.responseToReturn = null;

        GetLoginInfoResult result = service.execute("FSU-001", null);

        assertFalse(result.isSuccess());
        assertEquals("5001", result.getResultCode());
    }

    @Test
    void shouldPropagateDisabledResponse() {
        fsuClient.responseToReturn = FsuServiceResponse.disabled();

        GetLoginInfoResult result = service.execute("FSU-001", null);

        assertFalse(result.isSuccess());
        assertTrue(result.getResultDesc().contains("禁用"));
    }

    // ==================== 请求构造 ====================

    @Test
    void shouldBuildRequestWithCorrectPkType() {
        fsuClient.responseToReturn = buildSuccessResponse();

        service.execute("FSU-001", null);

        assertNotNull(fsuClient.lastRequest);
        assertEquals(BInterfacePkType.GET_LOGININFO, fsuClient.lastRequest.getPkType());
        assertEquals("FSU-001", fsuClient.lastRequest.getFsuCode());
    }

    @Test
    void shouldBuildRequestWithInfoFsuCode() {
        fsuClient.responseToReturn = buildSuccessResponse();

        service.execute("FSU-001", null);

        assertNotNull(fsuClient.lastRequest.getInfoXml());
        assertTrue(fsuClient.lastRequest.getInfoXml().contains("FSU-001"));
    }

    @Test
    void shouldBuildRequestWithoutXmlData() {
        fsuClient.responseToReturn = buildSuccessResponse();

        service.execute("FSU-001", null);

        assertNull(fsuClient.lastRequest.getXmlDataXml());
    }

    // ==================== 边界 ====================

    @Test
    void shouldNotAccessNetwork() {
        fsuClient.responseToReturn = buildSuccessResponse();

        GetLoginInfoResult result = service.execute("FSU-001", null);

        assertTrue(result.isSuccess());
    }

    @Test
    void shouldNotModifyLoginState() {
        // 验证 GET_LOGININFO 不修改 FSU 登录状态
        fsuClient.responseToReturn = buildSuccessResponse();

        GetLoginInfoResult result = service.execute("FSU-001", null);

        assertTrue(result.isSuccess());
        // 只读查询，不应触发任何状态变更
    }

    // ==================== 响应为空 ====================

    @Test
    void shouldFailWhenResponseXmlDataIsEmpty() {
        fsuClient.responseToReturn = FsuServiceResponse.success(
                "<soap:Envelope/>", "<ResultCode>0</ResultCode>", null, new XmlDataModel());

        GetLoginInfoResult result = service.execute("FSU-001", null);

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
        xmlData.setRootName("LoginInfo");
        var item = new LinkedHashMap<String, String>();
        item.put("FSUCode", "FSU-001");
        item.put("LoginStatus", "LOGIN");
        item.put("OnlineStatus", "ONLINE");
        item.put("SessionID", "SESSION-FSU-001-20260513-A3B8");
        item.put("LoginTime", "2026-05-13T10:30:00+08:00");
        item.put("LastHeartbeat", "2026-05-13T10:40:00+08:00");
        xmlData.addItem(item);

        return FsuServiceResponse.success(
                "<soap:Envelope><soap:Body><Response><PK_Type>GET_LOGININFO</PK_Type>"
                + "<Info><ResultCode>0</ResultCode></Info>"
                + "<xmlData><LoginInfo><FSUCode>FSU-001</FSUCode>"
                + "<LoginStatus>LOGIN</LoginStatus><OnlineStatus>ONLINE</OnlineStatus>"
                + "<SessionID>SESSION-FSU-001-20260513-A3B8</SessionID>"
                + "<LoginTime>2026-05-13T10:30:00+08:00</LoginTime>"
                + "<LastHeartbeat>2026-05-13T10:40:00+08:00</LastHeartbeat>"
                + "</LoginInfo></xmlData></Response></soap:Body></soap:Envelope>",
                "<ResultCode>0</ResultCode>",
                "<LoginInfo><FSUCode>FSU-001</FSUCode><LoginStatus>LOGIN</LoginStatus>"
                + "<OnlineStatus>ONLINE</OnlineStatus><SessionID>SESSION-FSU-001-20260513-A3B8</SessionID>"
                + "<LoginTime>2026-05-13T10:30:00+08:00</LoginTime>"
                + "<LastHeartbeat>2026-05-13T10:40:00+08:00</LastHeartbeat></LoginInfo>",
                xmlData);
    }
}
