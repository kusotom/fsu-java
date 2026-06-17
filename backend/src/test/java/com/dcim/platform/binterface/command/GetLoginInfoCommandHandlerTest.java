package com.dcim.platform.binterface.command;

import com.dcim.platform.module.binterface.command.CommandContext;
import com.dcim.platform.module.binterface.command.CommandResult;
import com.dcim.platform.module.binterface.command.GetLoginInfoCommandHandler;
import com.dcim.platform.module.binterface.model.BInterfaceMessage;
import com.dcim.platform.module.binterface.model.BInterfacePkType;
import com.dcim.platform.module.binterface.entity.BInterfaceFsuStatusEntity;
import com.dcim.platform.module.binterface.service.GetLoginInfoResult;
import com.dcim.platform.module.binterface.service.GetLoginInfoService;
import com.dcim.platform.module.binterface.service.LoginService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * GetLoginInfoCommandHandler 单元测试。
 */
class GetLoginInfoCommandHandlerTest {

    private GetLoginInfoCommandHandler handler;
    private StubLoginService loginService;
    private StubGetLoginInfoService getLoginInfoService;

    @BeforeEach
    void setUp() {
        loginService = new StubLoginService();
        getLoginInfoService = new StubGetLoginInfoService();
        handler = new GetLoginInfoCommandHandler(loginService, getLoginInfoService);
    }

    // ==================== 成功 ====================

    @Test
    void shouldHandleGetLoginInfoSuccessfully() {
        loginService.loggedIn = true;
        getLoginInfoService.successResult = true;

        CommandResult result = handler.handle(createContext("FSU-001"));

        assertTrue(result.isSuccess());
        assertEquals("1", result.getResultCode(), "Result=1 SUCCESS per 2016 EnumResult");
        assertTrue(result.isImplemented());
        assertEquals(BInterfacePkType.GET_LOGININFO, result.getPkType());
    }

    @Test
    void shouldIncludeLoginInfoInXmlData() {
        loginService.loggedIn = true;
        getLoginInfoService.successResult = true;

        CommandResult result = handler.handle(createContext("FSU-001"));

        String xmlData = result.toXmlDataXml();
        assertNotNull(xmlData);
        assertTrue(xmlData.contains("LOGIN"));
        assertTrue(xmlData.contains("ONLINE"));
    }

    // ==================== pkType ====================

    @Test
    void shouldSupportGetLoginInfoPkType() {
        assertEquals(BInterfacePkType.GET_LOGININFO, handler.getSupportedPkType());
    }

    // ==================== 校验失败 ====================

    @Test
    void shouldFailOnNullContext() {
        CommandResult result = handler.handle(null);

        assertFalse(result.isSuccess());
        assertEquals("5001", result.getResultCode());
        assertEquals(BInterfacePkType.GET_LOGININFO, result.getPkType());
    }

    @Test
    void shouldFailOnNullSoapMessage() {
        CommandContext ctx = new CommandContext(BInterfacePkType.GET_LOGININFO, null, null, null, null);
        CommandResult result = handler.handle(ctx);

        assertFalse(result.isSuccess());
        assertEquals("2001", result.getResultCode());
    }

    @Test
    void shouldFailOnMissingFsuCode() {
        loginService.loggedIn = true;

        CommandResult result = handler.handle(createContext(null));

        assertFalse(result.isSuccess());
        assertEquals("2001", result.getResultCode());
    }

    @Test
    void shouldFailOnNotLoggedIn() {
        loginService.loggedIn = false;
        getLoginInfoService.successResult = true;

        CommandResult result = handler.handle(createContext("FSU-001"));

        assertFalse(result.isSuccess());
        assertEquals("1002", result.getResultCode());
    }

    // ==================== Service 错误 ====================

    @Test
    void shouldFailWhenServiceReturnsError() {
        loginService.loggedIn = true;
        getLoginInfoService.successResult = false;
        getLoginInfoService.resultCodeToReturn = "5001";

        CommandResult result = handler.handle(createContext("FSU-001"));

        assertFalse(result.isSuccess());
        assertEquals("5001", result.getResultCode());
    }

    // ==================== extractFsuCode ====================

    @Test
    void extractFsuCodeShouldReturnCorrectValue() {
        String fsuCode = GetLoginInfoCommandHandler.extractFsuCode("<FSUCode>FSU-001</FSUCode>");
        assertEquals("FSU-001", fsuCode);
    }

    @Test
    void extractFsuCodeShouldBeCaseInsensitive() {
        String fsuCode = GetLoginInfoCommandHandler.extractFsuCode("<fsucode>FSU-001</fsucode>");
        assertEquals("FSU-001", fsuCode);
    }

    @Test
    void extractFsuCodeShouldReturnNullForEmptyInput() {
        assertNull(GetLoginInfoCommandHandler.extractFsuCode(null));
        assertNull(GetLoginInfoCommandHandler.extractFsuCode(""));
    }

    @Test
    void extractFsuCodeShouldHandleWhitespace() {
        String fsuCode = GetLoginInfoCommandHandler.extractFsuCode(
                "<FSUCode>  FSU-001  </FSUCode>");
        assertEquals("FSU-001", fsuCode);
    }

    @Test
    void extractFsuCodeShouldReturnNullWhenNotFound() {
        String fsuCode = GetLoginInfoCommandHandler.extractFsuCode("<ResultCode>0</ResultCode>");
        assertNull(fsuCode);
    }

    // ==================== Helper ====================

    private CommandContext createContext(String fsuCode) {
        BInterfaceMessage msg = new BInterfaceMessage();
        if (fsuCode != null) {
            msg.setInfo("<FSUCode>" + fsuCode + "</FSUCode>");
        } else {
            msg.setInfo("<ResultCode>0</ResultCode>");
        }
        msg.setPkType(BInterfacePkType.GET_LOGININFO);
        return new CommandContext(BInterfacePkType.GET_LOGININFO, msg, null, null, null);
    }

    // ==================== Stubs ====================

    static class StubLoginService extends LoginService {
        boolean loggedIn = true;

        StubLoginService() {
            super(null, null, null);
        }

        @Override
        public boolean isLoggedIn(String fsuCode) {
            return loggedIn;
        }

        @Override
        public Optional<BInterfaceFsuStatusEntity> getStatus(String fsuCode) {
            return Optional.empty();
        }
    }

    static class StubGetLoginInfoService extends GetLoginInfoService {
        boolean successResult = true;
        String resultCodeToReturn = "0";

        StubGetLoginInfoService() {
            super(null, null);
        }

        @Override
        public GetLoginInfoResult execute(String fsuCode, String fsuServiceUrl) {
            if (successResult) {
                return GetLoginInfoResult.success(fsuCode, "LOGIN", "ONLINE",
                        "SESSION-FSU-001-20260513-A3B8",
                        "2026-05-13T10:30:00+08:00",
                        "2026-05-13T10:40:00+08:00");
            }
            return GetLoginInfoResult.fail(resultCodeToReturn, "服务错误", fsuCode);
        }
    }
}
