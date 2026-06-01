package com.dcim.platform.binterface;

import com.dcim.platform.module.binterface.command.CommandContext;
import com.dcim.platform.module.binterface.command.CommandResult;
import com.dcim.platform.module.binterface.command.SendDataCommandHandler;
import com.dcim.platform.module.binterface.model.BInterfaceMessage;
import com.dcim.platform.module.binterface.model.BInterfacePkType;
import com.dcim.platform.module.binterface.service.LoginService;
import com.dcim.platform.module.binterface.service.SendDataResult;
import com.dcim.platform.module.binterface.service.SendDataService;
import com.dcim.platform.module.binterface.xml.XmlDataModel;
import org.junit.jupiter.api.BeforeEach;

import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SendDataCommandHandler 单元测试。
 *
 * 使用 LoginService 和 SendDataService 的简单实现，不依赖 Spring 上下文。
 */
class SendDataCommandHandlerTest {

    private SendDataCommandHandler handler;

    private StubLoginService loginService;
    private StubSendDataService sendDataService;

    @BeforeEach
    void setUp() {
        loginService = new StubLoginService();
        sendDataService = new StubSendDataService();
        handler = new SendDataCommandHandler(loginService, sendDataService);
    }

    @Test
    void shouldReturnSendDataPkType() {
        assertEquals(BInterfacePkType.SEND_DATA, handler.getSupportedPkType());
    }

    // ==================== 成功 ====================

    @Test
    void shouldHandleSuccessfulSendData() {
        loginService.loggedIn = true;
        sendDataService.returnResult = SendDataResult.success("FSU-001", 5, 0);

        CommandResult result = handler.handle(contextWithFsuCode("FSU-001", null));

        assertTrue(result.isSuccess());
        assertEquals("1", result.getResultCode());
        assertEquals(BInterfacePkType.SEND_DATA, result.getPkType());
        assertTrue(result.isImplemented());
    }

    @Test
    void shouldIncludeCountInResponse() {
        loginService.loggedIn = true;
        sendDataService.returnResult = SendDataResult.success("FSU-001", 3, 0);

        CommandResult result = handler.handle(contextWithFsuCode("FSU-001", null));

        String infoXml = result.toInfoXml();
        assertNotNull(infoXml);
        assertTrue(infoXml.contains("<Result>1</Result>"));
        assertTrue(infoXml.contains("<Count>3</Count>"));
    }

    @Test
    void shouldPassCollectTimeToService() {
        loginService.loggedIn = true;
        sendDataService.returnResult = SendDataResult.success("FSU-001", 1, 0);

        handler.handle(contextWithFsuCode("FSU-001", "2026-05-14T10:30:00+08:00"));

        assertEquals("2026-05-14T10:30:00+08:00", sendDataService.lastCollectTime);
    }

    @Test
    void shouldHandleWithXmlDataInContext() {
        loginService.loggedIn = true;
        sendDataService.returnResult = SendDataResult.success("FSU-001", 1, 0);

        XmlDataModel xmlData = new XmlDataModel();
        xmlData.addItem(Map.of("SignalID", "TEMP-001", "Value", "25.5"));

        String infoXml = "<FSUCode>FSU-001</FSUCode>";
        BInterfaceMessage msg = new BInterfaceMessage();
        msg.setInfo(infoXml);
        msg.setPkType(BInterfacePkType.SEND_DATA);
        CommandContext ctx = new CommandContext(BInterfacePkType.SEND_DATA, msg, xmlData, null, null);

        CommandResult result = handler.handle(ctx);

        assertTrue(result.isSuccess());
        assertSame(xmlData, sendDataService.lastXmlData);
    }

    // ==================== 错误处理 ====================

    @Test
    void shouldFailOnNullContext() {
        CommandResult result = handler.handle(null);
        assertFalse(result.isSuccess());
        assertEquals("5001", result.getResultCode());
        assertEquals(BInterfacePkType.SEND_DATA, result.getPkType());
    }

    @Test
    void shouldFailOnNullSoapMessage() {
        CommandContext ctx = new CommandContext(BInterfacePkType.SEND_DATA, null, null, null, null);
        CommandResult result = handler.handle(ctx);
        assertFalse(result.isSuccess());
        assertEquals("2001", result.getResultCode());
    }

    @Test
    void shouldFailOnMissingFsuCode() {
        BInterfaceMessage msg = new BInterfaceMessage();
        msg.setInfo("<SomeOtherField>value</SomeOtherField>");
        msg.setPkType(BInterfacePkType.SEND_DATA);
        CommandContext ctx = new CommandContext(BInterfacePkType.SEND_DATA, msg, null, null, null);

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
        sendDataService.returnResult = SendDataResult.fail("2003", "无有效测点数据");

        CommandResult result = handler.handle(contextWithFsuCode("FSU-001", null));
        assertFalse(result.isSuccess());
        assertEquals("2003", result.getResultCode());
    }

    @Test
    void shouldHandleServiceException() {
        loginService.loggedIn = true;
        sendDataService.shouldThrow = true;

        CommandContext ctx = contextWithFsuCode("FSU-001", null);
        CommandResult result = handler.handle(ctx);

        assertFalse(result.isSuccess());
        assertEquals("5001", result.getResultCode());
    }

    @Test
    void shouldReturnXmlDataNullInResponse() {
        loginService.loggedIn = true;
        sendDataService.returnResult = SendDataResult.success("FSU-001", 1, 0);

        CommandResult result = handler.handle(contextWithFsuCode("FSU-001", null));

        assertNull(result.getResponseXmlData());
    }

    // ==================== extractCollectTime ====================

    @Test
    void extractCollectTimeShouldReturnValue() {
        assertEquals("2026-05-14T10:30:00+08:00",
                SendDataCommandHandler.extractCollectTime("<CollectTime>2026-05-14T10:30:00+08:00</CollectTime>"));
    }

    @Test
    void extractCollectTimeShouldBeCaseInsensitive() {
        assertEquals("2026-05-14T10:30:00+08:00",
                SendDataCommandHandler.extractCollectTime("<collecttime>2026-05-14T10:30:00+08:00</collecttime>"));
        assertEquals("2026-05-14T10:30:00+08:00",
                SendDataCommandHandler.extractCollectTime("<COLLECTTIME>2026-05-14T10:30:00+08:00</COLLECTTIME>"));
    }

    @Test
    void extractCollectTimeShouldHandleWhitespace() {
        assertEquals("2026-05-14T10:30:00+08:00",
                SendDataCommandHandler.extractCollectTime("<CollectTime>  2026-05-14T10:30:00+08:00  </CollectTime>"));
    }

    @Test
    void extractCollectTimeShouldReturnNullForEmptyInput() {
        assertNull(SendDataCommandHandler.extractCollectTime(null));
        assertNull(SendDataCommandHandler.extractCollectTime(""));
    }

    @Test
    void extractCollectTimeShouldReturnNullForEmptyTag() {
        assertNull(SendDataCommandHandler.extractCollectTime("<CollectTime>  </CollectTime>"));
    }

    @Test
    void extractCollectTimeShouldReturnNullWhenNotPresent() {
        assertNull(SendDataCommandHandler.extractCollectTime("<FSUCode>FSU-001</FSUCode>"));
    }

    // ==================== extractFsuCode ====================

    @Test
    void extractFsuCodeShouldReturnValue() {
        assertEquals("FSU-001",
                SendDataCommandHandler.extractFsuCode("<FSUCode>FSU-001</FSUCode>"));
    }

    // ==================== 辅助方法 ====================

    private CommandContext contextWithFsuCode(String fsuCode, String collectTime) {
        String infoXml = "<FSUCode>" + fsuCode + "</FSUCode>";
        if (collectTime != null) {
            infoXml = "<CollectTime>" + collectTime + "</CollectTime>"
                    + "<FSUCode>" + fsuCode + "</FSUCode>";
        }
        BInterfaceMessage msg = new BInterfaceMessage();
        msg.setInfo(infoXml);
        msg.setPkType(BInterfacePkType.SEND_DATA);
        return new CommandContext(BInterfacePkType.SEND_DATA, msg, null, null, null);
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

    // ==================== Stub SendDataService ====================

    static class StubSendDataService extends SendDataService {
        SendDataResult returnResult;
        boolean shouldThrow = false;
        String lastCollectTime;
        XmlDataModel lastXmlData;

        StubSendDataService() {
            super(null, null, null);
        }

        @Override
        public SendDataResult processData(String fsuCode, String collectTimeStr, XmlDataModel xmlData) {
            if (shouldThrow) {
                throw new RuntimeException("模拟 processData 异常");
            }
            lastCollectTime = collectTimeStr;
            lastXmlData = xmlData;
            return returnResult;
        }
    }
}
