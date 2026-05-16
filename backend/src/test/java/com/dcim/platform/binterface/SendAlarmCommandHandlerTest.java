package com.dcim.platform.binterface;

import com.dcim.platform.module.binterface.command.CommandContext;
import com.dcim.platform.module.binterface.command.CommandResult;
import com.dcim.platform.module.binterface.command.SendAlarmCommandHandler;
import com.dcim.platform.module.binterface.model.BInterfaceMessage;
import com.dcim.platform.module.binterface.model.BInterfacePkType;
import com.dcim.platform.module.binterface.service.LoginService;
import com.dcim.platform.module.binterface.service.SendAlarmResult;
import com.dcim.platform.module.binterface.service.SendAlarmService;
import com.dcim.platform.module.binterface.xml.XmlDataModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SendAlarmCommandHandler 单元测试。
 *
 * 使用 LoginService 和 SendAlarmService 的简单实现，不依赖 Spring 上下文。
 */
class SendAlarmCommandHandlerTest {

    private SendAlarmCommandHandler handler;

    private StubLoginService loginService;
    private StubSendAlarmService sendAlarmService;

    @BeforeEach
    void setUp() {
        loginService = new StubLoginService();
        sendAlarmService = new StubSendAlarmService();
        handler = new SendAlarmCommandHandler(loginService, sendAlarmService);
    }

    @Test
    void shouldReturnSendAlarmPkType() {
        assertEquals(BInterfacePkType.SEND_ALARM, handler.getSupportedPkType());
    }

    // ==================== 成功 ====================

    @Test
    void shouldHandleSuccessfulSendAlarm() {
        loginService.loggedIn = true;
        sendAlarmService.returnResult = SendAlarmResult.success("FSU-001", 1, 0, 0, List.of(100L));

        CommandResult result = handler.handle(contextWithFsuCode("FSU-001", null));

        assertTrue(result.isSuccess());
        assertEquals("0", result.getResultCode());
        assertEquals(BInterfacePkType.SEND_ALARM, result.getPkType());
        assertTrue(result.isImplemented());
    }

    @Test
    void shouldIncludeAlarmIdInResponse() {
        loginService.loggedIn = true;
        sendAlarmService.returnResult = SendAlarmResult.success("FSU-001", 1, 0, 0, List.of(42L));

        CommandResult result = handler.handle(contextWithFsuCode("FSU-001", null));

        String infoXml = result.toInfoXml();
        assertNotNull(infoXml);
        assertTrue(infoXml.contains("<ResultCode>0</ResultCode>"));
        assertTrue(infoXml.contains("<AlarmID>42</AlarmID>"));
    }

    @Test
    void shouldPassAlarmTimeToService() {
        loginService.loggedIn = true;
        sendAlarmService.returnResult = SendAlarmResult.success("FSU-001", 1, 0, 0, List.of(100L));

        handler.handle(contextWithFsuCode("FSU-001", "2026-05-14T10:33:00+08:00"));

        assertEquals("2026-05-14T10:33:00+08:00", sendAlarmService.lastAlarmTime);
    }

    @Test
    void shouldHandleWithXmlDataInContext() {
        loginService.loggedIn = true;
        sendAlarmService.returnResult = SendAlarmResult.success("FSU-001", 1, 0, 0, List.of(100L));

        XmlDataModel xmlData = new XmlDataModel();
        xmlData.addItem(Map.of("SignalID", "TEMP-001", "AlarmCode", "TEMP-HIGH", "AlarmLevel", "WARN"));

        String infoXml = "<FSUCode>FSU-001</FSUCode>";
        BInterfaceMessage msg = new BInterfaceMessage();
        msg.setInfo(infoXml);
        msg.setPkType(BInterfacePkType.SEND_ALARM);
        CommandContext ctx = new CommandContext(BInterfacePkType.SEND_ALARM, msg, xmlData, null, null);

        CommandResult result = handler.handle(ctx);

        assertTrue(result.isSuccess());
        assertSame(xmlData, sendAlarmService.lastXmlData);
    }

    // ==================== 错误处理 ====================

    @Test
    void shouldFailOnNullContext() {
        CommandResult result = handler.handle(null);
        assertFalse(result.isSuccess());
        assertEquals("5001", result.getResultCode());
        assertEquals(BInterfacePkType.SEND_ALARM, result.getPkType());
    }

    @Test
    void shouldFailOnNullSoapMessage() {
        CommandContext ctx = new CommandContext(BInterfacePkType.SEND_ALARM, null, null, null, null);
        CommandResult result = handler.handle(ctx);
        assertFalse(result.isSuccess());
        assertEquals("2001", result.getResultCode());
    }

    @Test
    void shouldFailOnMissingFsuCode() {
        BInterfaceMessage msg = new BInterfaceMessage();
        msg.setInfo("<SomeOtherField>value</SomeOtherField>");
        msg.setPkType(BInterfacePkType.SEND_ALARM);
        CommandContext ctx = new CommandContext(BInterfacePkType.SEND_ALARM, msg, null, null, null);

        CommandResult result = handler.handle(ctx);
        assertFalse(result.isSuccess());
        assertEquals("2001", result.getResultCode());
    }

    @Test
    void shouldFailOnNotLoggedIn() {
        loginService.loggedIn = false;

        CommandResult result = handler.handle(contextWithFsuCode("FSU-001", null));
        assertFalse(result.isSuccess());
        assertEquals("1002", result.getResultCode());
    }

    @Test
    void shouldFailOnServiceError() {
        loginService.loggedIn = true;
        sendAlarmService.returnResult = SendAlarmResult.fail("2003", "无有效告警数据");

        CommandResult result = handler.handle(contextWithFsuCode("FSU-001", null));
        assertFalse(result.isSuccess());
        assertEquals("2003", result.getResultCode());
    }

    @Test
    void shouldHandleServiceException() {
        loginService.loggedIn = true;
        sendAlarmService.shouldThrow = true;

        CommandContext ctx = contextWithFsuCode("FSU-001", null);
        CommandResult result = handler.handle(ctx);

        assertFalse(result.isSuccess());
        assertEquals("5001", result.getResultCode());
    }

    @Test
    void shouldReturnXmlDataNullInResponse() {
        loginService.loggedIn = true;
        sendAlarmService.returnResult = SendAlarmResult.success("FSU-001", 1, 0, 0, List.of(100L));

        CommandResult result = handler.handle(contextWithFsuCode("FSU-001", null));

        assertNull(result.getResponseXmlData());
    }

    // ==================== extractAlarmTime ====================

    @Test
    void extractAlarmTimeShouldReturnValue() {
        assertEquals("2026-05-14T10:33:00+08:00",
                SendAlarmCommandHandler.extractAlarmTime("<AlarmTime>2026-05-14T10:33:00+08:00</AlarmTime>"));
    }

    @Test
    void extractAlarmTimeShouldBeCaseInsensitive() {
        assertEquals("2026-05-14T10:33:00+08:00",
                SendAlarmCommandHandler.extractAlarmTime("<alarmtime>2026-05-14T10:33:00+08:00</alarmtime>"));
        assertEquals("2026-05-14T10:33:00+08:00",
                SendAlarmCommandHandler.extractAlarmTime("<ALARMTIME>2026-05-14T10:33:00+08:00</ALARMTIME>"));
    }

    @Test
    void extractAlarmTimeShouldHandleWhitespace() {
        assertEquals("2026-05-14T10:33:00+08:00",
                SendAlarmCommandHandler.extractAlarmTime("<AlarmTime>  2026-05-14T10:33:00+08:00  </AlarmTime>"));
    }

    @Test
    void extractAlarmTimeShouldReturnNullForEmptyInput() {
        assertNull(SendAlarmCommandHandler.extractAlarmTime(null));
        assertNull(SendAlarmCommandHandler.extractAlarmTime(""));
    }

    @Test
    void extractAlarmTimeShouldReturnNullForEmptyTag() {
        assertNull(SendAlarmCommandHandler.extractAlarmTime("<AlarmTime>  </AlarmTime>"));
    }

    @Test
    void extractAlarmTimeShouldReturnNullWhenNotPresent() {
        assertNull(SendAlarmCommandHandler.extractAlarmTime("<FSUCode>FSU-001</FSUCode>"));
    }

    // ==================== extractFsuCode ====================

    @Test
    void extractFsuCodeShouldReturnValue() {
        assertEquals("FSU-001",
                SendAlarmCommandHandler.extractFsuCode("<FSUCode>FSU-001</FSUCode>"));
    }

    // ==================== 辅助方法 ====================

    private CommandContext contextWithFsuCode(String fsuCode, String alarmTime) {
        String infoXml;
        if (alarmTime != null) {
            infoXml = "<AlarmTime>" + alarmTime + "</AlarmTime>"
                    + "<FSUCode>" + fsuCode + "</FSUCode>";
        } else {
            infoXml = "<FSUCode>" + fsuCode + "</FSUCode>";
        }
        BInterfaceMessage msg = new BInterfaceMessage();
        msg.setInfo(infoXml);
        msg.setPkType(BInterfacePkType.SEND_ALARM);
        return new CommandContext(BInterfacePkType.SEND_ALARM, msg, null, null, null);
    }

    // ==================== Stub LoginService ====================

    static class StubLoginService extends LoginService {
        boolean loggedIn = false;

        StubLoginService() {
            super(null, null, null);
        }

        @Override
        public boolean isLoggedIn(String fsuCode) {
            return loggedIn;
        }
    }

    // ==================== Stub SendAlarmService ====================

    static class StubSendAlarmService extends SendAlarmService {
        SendAlarmResult returnResult;
        boolean shouldThrow = false;
        String lastAlarmTime;
        XmlDataModel lastXmlData;

        StubSendAlarmService() {
            super(null, null);
        }

        @Override
        public SendAlarmResult processAlarms(String fsuCode, String alarmTimeStr, XmlDataModel xmlData) {
            if (shouldThrow) {
                throw new RuntimeException("模拟 processAlarms 异常");
            }
            lastAlarmTime = alarmTimeStr;
            lastXmlData = xmlData;
            return returnResult;
        }
    }
}
