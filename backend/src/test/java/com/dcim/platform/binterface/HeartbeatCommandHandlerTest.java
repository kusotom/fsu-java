package com.dcim.platform.binterface;

import com.dcim.platform.module.binterface.command.CommandContext;
import com.dcim.platform.module.binterface.command.CommandResult;
import com.dcim.platform.module.binterface.command.HeartbeatCommandHandler;
import com.dcim.platform.module.binterface.model.BInterfaceMessage;
import com.dcim.platform.module.binterface.model.BInterfacePkType;
import com.dcim.platform.module.binterface.service.LoginService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * HeartbeatCommandHandler 单元测试。
 *
 * 使用 LoginService 的简单实现，不依赖 Spring 上下文。
 */
class HeartbeatCommandHandlerTest {

    private HeartbeatCommandHandler handler;

    private StubLoginService loginService;

    @BeforeEach
    void setUp() {
        loginService = new StubLoginService();
        handler = new HeartbeatCommandHandler(loginService);
    }

    @Test
    void shouldReturnHeartbeatPkType() {
        assertEquals(BInterfacePkType.HEARTBEAT, handler.getSupportedPkType());
    }

    // ==================== 成功心跳 ====================

    @Test
    void shouldHandleSuccessfulHeartbeat() {
        loginService.loggedIn = true;

        CommandContext ctx = contextWithFsuCode("FSU-001");
        CommandResult result = handler.handle(ctx);

        assertTrue(result.isSuccess());
        assertEquals("1", result.getResultCode());
        assertEquals(BInterfacePkType.HEARTBEAT, result.getPkType());
        assertTrue(result.isImplemented());
        assertTrue(loginService.lastSeenUpdated);
    }

    @Test
    void shouldUpdateLastSeenOnHeartbeat() {
        loginService.loggedIn = true;

        handler.handle(contextWithFsuCode("FSU-001"));

        assertTrue(loginService.lastSeenUpdated);
        assertEquals("FSU-001", loginService.lastSeenFsuCode);
    }

    @Test
    void shouldReturnServerTimeInResponse() {
        loginService.loggedIn = true;

        CommandResult result = handler.handle(contextWithFsuCode("FSU-001"));

        String infoXml = result.toInfoXml();
        assertNotNull(infoXml);
        assertTrue(infoXml.contains("<Result>1</Result>"));
        assertTrue(infoXml.contains("<ServerTime>"));
    }

    // ==================== 错误处理 ====================

    @Test
    void shouldFailOnNullContext() {
        CommandResult result = handler.handle(null);
        assertFalse(result.isSuccess());
        assertEquals("5001", result.getResultCode());
        assertEquals(BInterfacePkType.HEARTBEAT, result.getPkType());
    }

    @Test
    void shouldFailOnNullSoapMessage() {
        CommandContext ctx = new CommandContext(BInterfacePkType.HEARTBEAT, null, null, null, null);
        CommandResult result = handler.handle(ctx);
        assertFalse(result.isSuccess());
        assertEquals("2001", result.getResultCode());
    }

    @Test
    void shouldFailOnMissingFsuCode() {
        BInterfaceMessage msg = new BInterfaceMessage();
        msg.setInfo("<SomeOtherField>value</SomeOtherField>");
        msg.setPkType(BInterfacePkType.HEARTBEAT);
        CommandContext ctx = new CommandContext(BInterfacePkType.HEARTBEAT, msg, null, null, null);

        CommandResult result = handler.handle(ctx);
        assertFalse(result.isSuccess());
        assertEquals("2001", result.getResultCode());
    }

    @Test
    void shouldFailOnNotLoggedIn() {
        loginService.loggedIn = false;

        CommandResult result = handler.handle(contextWithFsuCode("FSU-001"));
        assertFalse(result.isSuccess());
        assertEquals("1002", result.getResultCode());
        assertFalse(loginService.lastSeenUpdated);
    }

    @Test
    void shouldHandleServiceException() {
        loginService.loggedIn = true;
        loginService.shouldThrowOnUpdate = true;

        CommandContext ctx = contextWithFsuCode("FSU-001");
        CommandResult result = handler.handle(ctx);

        assertFalse(result.isSuccess());
        assertEquals("5001", result.getResultCode());
    }

    // ==================== extractSessionId ====================

    @Test
    void extractSessionIdShouldReturnId() {
        assertEquals("SESSION-123",
                HeartbeatCommandHandler.extractSessionId("<SessionID>SESSION-123</SessionID>"));
    }

    @Test
    void extractSessionIdShouldBeCaseInsensitive() {
        assertEquals("SID-001",
                HeartbeatCommandHandler.extractSessionId("<sessionid>SID-001</sessionid>"));
        assertEquals("SID-001",
                HeartbeatCommandHandler.extractSessionId("<SESSIONID>SID-001</SESSIONID>"));
    }

    @Test
    void extractSessionIdShouldHandleWhitespace() {
        assertEquals("SID-001",
                HeartbeatCommandHandler.extractSessionId("<SessionID>  SID-001  </SessionID>"));
    }

    @Test
    void extractSessionIdShouldReturnNullForEmptyInput() {
        assertNull(HeartbeatCommandHandler.extractSessionId(null));
        assertNull(HeartbeatCommandHandler.extractSessionId(""));
    }

    @Test
    void extractSessionIdShouldReturnNullForEmptyTag() {
        assertNull(HeartbeatCommandHandler.extractSessionId("<SessionID>  </SessionID>"));
    }

    // ==================== 辅助方法 ====================

    private CommandContext contextWithFsuCode(String fsuCode) {
        String infoXml = "<FSUCode>" + fsuCode + "</FSUCode>";
        BInterfaceMessage msg = new BInterfaceMessage();
        msg.setInfo(infoXml);
        msg.setPkType(BInterfacePkType.HEARTBEAT);
        return new CommandContext(BInterfacePkType.HEARTBEAT, msg, null, null, null);
    }

    // ==================== Stub LoginService ====================

    static class StubLoginService extends LoginService {
        boolean loggedIn = false;
        boolean shouldThrowOnUpdate = false;
        boolean lastSeenUpdated = false;
        String lastSeenFsuCode;

        StubLoginService() {
            super(null, null, null);
        }

        @Override
        public boolean isLoggedIn(String fsuCode) {
            return loggedIn;
        }

        @Override
        public void updateLastSeen(String fsuCode) {
            if (shouldThrowOnUpdate) {
                throw new RuntimeException("模拟 updateLastSeen 异常");
            }
            lastSeenUpdated = true;
            lastSeenFsuCode = fsuCode;
        }
    }
}
