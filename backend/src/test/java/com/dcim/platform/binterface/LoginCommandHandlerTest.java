package com.dcim.platform.binterface;

import com.dcim.platform.module.binterface.command.CommandContext;
import com.dcim.platform.module.binterface.command.CommandResult;
import com.dcim.platform.module.binterface.command.LoginCommandHandler;
import com.dcim.platform.module.binterface.model.BInterfaceMessage;
import com.dcim.platform.module.binterface.model.BInterfacePkType;
import com.dcim.platform.module.binterface.service.LoginResult;
import com.dcim.platform.module.binterface.service.LoginService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

/**
 * LoginCommandHandler 单元测试。
 *
 * 使用 LoginService 的简单实现，不依赖 Spring 上下文。
 */
class LoginCommandHandlerTest {

    private LoginCommandHandler handler;

    private StubLoginService loginService;

    @BeforeEach
    void setUp() {
        loginService = new StubLoginService();
        handler = new LoginCommandHandler(loginService);
    }

    @Test
    void shouldReturnLoginPkType() {
        assertEquals(BInterfacePkType.LOGIN, handler.getSupportedPkType());
    }

    // ==================== 成功登录 ====================

    @Test
    void shouldHandleSuccessfulLogin() {
        loginService.stubResult = LoginResult.success("FSU-001", "SESSION-FSU-001-TEST", Instant.now());

        CommandContext ctx = contextWithFsuCode("FSU-001");
        CommandResult result = handler.handle(ctx);

        assertTrue(result.isSuccess());
        assertEquals("1", result.getResultCode(), "Result=1 SUCCESS per 2016 EnumResult");
        assertEquals(BInterfacePkType.LOGIN, result.getPkType());
        assertTrue(result.isImplemented());

        // 验证响应 Info 包含必要字段
        String infoXml = result.toInfoXml();
        assertNotNull(infoXml);
        assertTrue(infoXml.contains("<Result>1</Result>"), "B接口2016 uses Result not ResultCode");
        assertTrue(infoXml.contains("<SessionID>"));
        assertTrue(infoXml.contains("<ExpireSeconds>"));
        assertTrue(infoXml.contains("<ServerTime>"));
    }

    @Test
    void shouldPassRemoteAddrToService() {
        loginService.stubResult = LoginResult.success("FSU-001", "SESSION-FSU-001-TEST", Instant.now());

        CommandContext ctx = contextWithFsuCode("FSU-001");
        ctx.setAttribute("remoteAddr", "192.168.1.100");
        handler.handle(ctx);

        assertEquals("192.168.1.100", loginService.lastRemoteAddr);
    }

    // ==================== 错误处理 ====================

    @Test
    void shouldReturnErrorOnNullContext() {
        CommandResult result = handler.handle(null);
        assertFalse(result.isSuccess());
        assertEquals("5001", result.getResultCode());
        assertEquals(BInterfacePkType.LOGIN, result.getPkType());
    }

    @Test
    void shouldReturnErrorOnNullSoapMessage() {
        CommandContext ctx = new CommandContext(BInterfacePkType.LOGIN, null, null, null, null);

        CommandResult result = handler.handle(ctx);
        assertFalse(result.isSuccess());
        assertEquals("2001", result.getResultCode());
    }

    @Test
    void shouldReturnErrorOnMissingFsuCode() {
        BInterfaceMessage msg = new BInterfaceMessage();
        msg.setInfo("<SomeOtherField>value</SomeOtherField>");
        msg.setPkType(BInterfacePkType.LOGIN);
        CommandContext ctx = new CommandContext(BInterfacePkType.LOGIN, msg, null, null, null);

        CommandResult result = handler.handle(ctx);
        assertFalse(result.isSuccess());
        assertEquals("2001", result.getResultCode());
    }

    @Test
    void shouldReturnErrorOnEmptyFsuCode() {
        BInterfaceMessage msg = new BInterfaceMessage();
        msg.setInfo("<FSUCode></FSUCode>");
        msg.setPkType(BInterfacePkType.LOGIN);
        CommandContext ctx = new CommandContext(BInterfacePkType.LOGIN, msg, null, null, null);

        CommandResult result = handler.handle(ctx);
        assertFalse(result.isSuccess());
        assertEquals("2001", result.getResultCode());
    }

    @Test
    void shouldReturnServiceErrorCode() {
        // 模拟服务返回业务错误（如未知 FSU）
        loginService.stubResult = LoginResult.fail("1002", "FSU 未注册: FSU-UNKNOWN");

        CommandContext ctx = contextWithFsuCode("FSU-UNKNOWN");
        CommandResult result = handler.handle(ctx);

        assertFalse(result.isSuccess());
        assertEquals("1002", result.getResultCode());
        assertTrue(result.getResultDesc().contains("FSU 未注册"));
    }

    @Test
    void shouldHandleServiceException() {
        loginService.shouldThrow = true;

        CommandContext ctx = contextWithFsuCode("FSU-001");
        CommandResult result = handler.handle(ctx);

        assertFalse(result.isSuccess());
        assertEquals("5001", result.getResultCode());
    }

    // ==================== extractFsuCode ====================

    @Test
    void extractFsuCodeShouldReturnCode() {
        String xml = "<Info><FSUCode>FSU-001</FSUCode></Info>";
        assertEquals("FSU-001", LoginCommandHandler.extractFsuCode(xml));
    }

    @Test
    void extractFsuCodeShouldBeCaseInsensitive() {
        assertEquals("FSU-001", LoginCommandHandler.extractFsuCode("<FsuCode>FSU-001</FsuCode>"));
        assertEquals("FSU-001", LoginCommandHandler.extractFsuCode("<FSUCODE>FSU-001</FSUCODE>"));
    }

    @Test
    void extractFsuCodeShouldHandleWhitespace() {
        assertEquals("FSU-001", LoginCommandHandler.extractFsuCode("<FSUCode>  FSU-001  </FSUCode>"));
    }

    @Test
    void extractFsuCodeShouldReturnNullForEmptyInput() {
        assertNull(LoginCommandHandler.extractFsuCode(null));
        assertNull(LoginCommandHandler.extractFsuCode(""));
    }

    @Test
    void extractFsuCodeShouldReturnNullForEmptyTag() {
        assertNull(LoginCommandHandler.extractFsuCode("<FSUCode>  </FSUCode>"));
    }

    // ==================== 辅助方法 ====================

    private CommandContext contextWithFsuCode(String fsuCode) {
        String infoXml = "<FSUCode>" + fsuCode + "</FSUCode>";
        BInterfaceMessage msg = new BInterfaceMessage();
        msg.setInfo(infoXml);
        msg.setPkType(BInterfacePkType.LOGIN);
        return new CommandContext(BInterfacePkType.LOGIN, msg, null, null, null);
    }

    // ==================== Stub LoginService ====================

    static class StubLoginService extends LoginService {
        LoginResult stubResult;
        boolean shouldThrow = false;
        String lastFsuCode;
        String lastRemoteAddr;

        StubLoginService() {
            super(null, null, null);
        }

        @Override
        public LoginResult login(String fsuCode, String remoteAddr) {
            lastFsuCode = fsuCode;
            lastRemoteAddr = remoteAddr;
            if (shouldThrow) {
                throw new RuntimeException("模拟服务异常");
            }
            return stubResult;
        }
    }
}
