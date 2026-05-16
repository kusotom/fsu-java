package com.dcim.platform.binterface.command;

import com.dcim.platform.module.binterface.command.CommandContext;
import com.dcim.platform.module.binterface.command.CommandResult;
import com.dcim.platform.module.binterface.command.TimeCheckCommandHandler;
import com.dcim.platform.module.binterface.model.BInterfaceMessage;
import com.dcim.platform.module.binterface.model.BInterfacePkType;
import com.dcim.platform.module.binterface.service.LoginService;
import com.dcim.platform.module.binterface.service.TimeCheckResult;
import com.dcim.platform.module.binterface.service.TimeCheckService;
import com.dcim.platform.module.binterface.xml.XmlDataModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TimeCheckCommandHandler 单元测试。
 */
class TimeCheckCommandHandlerTest {

    private TimeCheckCommandHandler handler;
    private StubLoginService loginService;
    private StubTimeCheckService timeCheckService;

    @BeforeEach
    void setUp() {
        loginService = new StubLoginService();
        timeCheckService = new StubTimeCheckService();
        handler = new TimeCheckCommandHandler(loginService, timeCheckService);
    }

    @Test
    void shouldReturnTimeCheckPkType() {
        assertEquals(BInterfacePkType.TIME_CHECK, handler.getSupportedPkType());
    }

    // ==================== 成功 ====================

    @Test
    void shouldHandleSuccessfulTimeCheck() {
        loginService.loggedIn = true;
        timeCheckService.successResult = true;

        CommandResult result = handler.handle(createContext("FSU-001", "2026-05-13T10:42:00+08:00"));

        assertTrue(result.isSuccess());
        assertEquals("0", result.getResultCode());
        assertTrue(result.isImplemented());
        assertEquals(BInterfacePkType.TIME_CHECK, result.getPkType());
    }

    @Test
    void shouldIncludeFsuTimeInResponse() {
        loginService.loggedIn = true;
        timeCheckService.successResult = true;

        CommandResult result = handler.handle(createContext("FSU-001", "2026-05-13T10:42:00+08:00"));

        String infoXml = result.toInfoXml();
        assertNotNull(infoXml);
        assertTrue(infoXml.contains("<ResultCode>0</ResultCode>"));
        assertTrue(infoXml.contains("<FSUTime>"));
    }

    // ==================== pkType ====================

    @Test
    void shouldSupportTimeCheckPkType() {
        assertEquals(BInterfacePkType.TIME_CHECK, handler.getSupportedPkType());
    }

    // ==================== 校验失败 ====================

    @Test
    void shouldFailOnNullContext() {
        CommandResult result = handler.handle(null);

        assertFalse(result.isSuccess());
        assertEquals("5001", result.getResultCode());
        assertEquals(BInterfacePkType.TIME_CHECK, result.getPkType());
    }

    @Test
    void shouldFailOnNullSoapMessage() {
        CommandContext ctx = new CommandContext(BInterfacePkType.TIME_CHECK, null, null, null, null);
        CommandResult result = handler.handle(ctx);

        assertFalse(result.isSuccess());
        assertEquals("2001", result.getResultCode());
    }

    @Test
    void shouldFailOnMissingFsuCode() {
        loginService.loggedIn = true;

        CommandResult result = handler.handle(createContext(null, "2026-05-13T10:42:00+08:00"));

        assertFalse(result.isSuccess());
        assertEquals("2001", result.getResultCode());
    }

    @Test
    void shouldFailOnMissingStandardTime() {
        loginService.loggedIn = true;

        CommandResult result = handler.handle(createContext("FSU-001", null));

        assertFalse(result.isSuccess());
        assertEquals("2003", result.getResultCode());
    }

    @Test
    void shouldFailOnNotLoggedIn() {
        loginService.loggedIn = false;
        timeCheckService.successResult = true;

        CommandResult result = handler.handle(createContext("FSU-001", "2026-05-13T10:42:00+08:00"));

        assertFalse(result.isSuccess());
        assertEquals("1002", result.getResultCode());
    }

    // ==================== Service 错误 ====================

    @Test
    void shouldFailWhenServiceReturnsError() {
        loginService.loggedIn = true;
        timeCheckService.successResult = false;
        timeCheckService.resultCodeToReturn = "5001";

        CommandResult result = handler.handle(createContext("FSU-001", "2026-05-13T10:42:00+08:00"));

        assertFalse(result.isSuccess());
        assertEquals("5001", result.getResultCode());
    }

    @Test
    void shouldHandleServiceException() {
        loginService.loggedIn = true;
        timeCheckService.shouldThrow = true;

        CommandResult result = handler.handle(createContext("FSU-001", "2026-05-13T10:42:00+08:00"));

        assertFalse(result.isSuccess());
        assertEquals("5001", result.getResultCode());
    }

    // ==================== extractFsuCode ====================

    @Test
    void extractFsuCodeShouldReturnCorrectValue() {
        String fsuCode = TimeCheckCommandHandler.extractFsuCode("<FSUCode>FSU-001</FSUCode>");
        assertEquals("FSU-001", fsuCode);
    }

    @Test
    void extractFsuCodeShouldBeCaseInsensitive() {
        String fsuCode = TimeCheckCommandHandler.extractFsuCode("<fsucode>FSU-001</fsucode>");
        assertEquals("FSU-001", fsuCode);
    }

    @Test
    void extractFsuCodeShouldReturnNullForEmptyInput() {
        assertNull(TimeCheckCommandHandler.extractFsuCode(null));
        assertNull(TimeCheckCommandHandler.extractFsuCode(""));
    }

    // ==================== extractStandardTime ====================

    @Test
    void extractStandardTimeShouldReturnCorrectValue() {
        String time = TimeCheckCommandHandler.extractStandardTime(
                "<StandardTime>2026-05-13T10:42:00+08:00</StandardTime>");
        assertEquals("2026-05-13T10:42:00+08:00", time);
    }

    @Test
    void extractStandardTimeShouldBeCaseInsensitive() {
        String time = TimeCheckCommandHandler.extractStandardTime(
                "<standardtime>2026-05-13T10:42:00+08:00</standardtime>");
        assertEquals("2026-05-13T10:42:00+08:00", time);
    }

    @Test
    void extractStandardTimeShouldHandleWhitespace() {
        String time = TimeCheckCommandHandler.extractStandardTime(
                "<StandardTime>  2026-05-13T10:42:00+08:00  </StandardTime>");
        assertEquals("2026-05-13T10:42:00+08:00", time);
    }

    @Test
    void extractStandardTimeShouldReturnNullForEmptyInput() {
        assertNull(TimeCheckCommandHandler.extractStandardTime(null));
        assertNull(TimeCheckCommandHandler.extractStandardTime(""));
    }

    @Test
    void extractStandardTimeShouldReturnNullWhenNotFound() {
        assertNull(TimeCheckCommandHandler.extractStandardTime("<ResultCode>0</ResultCode>"));
    }

    // ==================== 辅助方法 ====================

    private CommandContext createContext(String fsuCode, String standardTime) {
        StringBuilder info = new StringBuilder();
        if (fsuCode != null) {
            info.append("<FSUCode>").append(fsuCode).append("</FSUCode>");
        }
        if (standardTime != null) {
            info.append("<StandardTime>").append(standardTime).append("</StandardTime>");
        }
        if (info.length() == 0) {
            info.append("<ResultCode>1</ResultCode>");
        }

        BInterfaceMessage msg = new BInterfaceMessage();
        msg.setInfo(info.toString());
        msg.setPkType(BInterfacePkType.TIME_CHECK);
        return new CommandContext(BInterfacePkType.TIME_CHECK, msg, new XmlDataModel(), null, null);
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
    }

    static class StubTimeCheckService extends TimeCheckService {
        boolean successResult = true;
        String resultCodeToReturn = "0";
        boolean shouldThrow = false;

        StubTimeCheckService() {
            super(null, null);
        }

        @Override
        public TimeCheckResult execute(String fsuCode, String fsuServiceUrl, String standardTime) {
            if (shouldThrow) {
                throw new RuntimeException("模拟 TimeCheckService 异常");
            }
            if (successResult) {
                return TimeCheckResult.success(fsuCode, "2026-05-13T10:42:01+08:00", standardTime);
            }
            return TimeCheckResult.fail(resultCodeToReturn, "服务错误", fsuCode);
        }
    }
}
