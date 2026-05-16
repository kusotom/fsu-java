package com.dcim.platform.binterface.command;

import com.dcim.platform.module.binterface.command.CommandContext;
import com.dcim.platform.module.binterface.command.CommandResult;
import com.dcim.platform.module.binterface.command.GetDataCommandHandler;
import com.dcim.platform.module.binterface.model.BInterfaceMessage;
import com.dcim.platform.module.binterface.model.BInterfacePkType;
import com.dcim.platform.module.binterface.entity.BInterfaceFsuStatusEntity;
import com.dcim.platform.module.binterface.service.GetDataResult;
import com.dcim.platform.module.binterface.service.GetDataService;
import com.dcim.platform.module.binterface.service.LoginService;
import com.dcim.platform.module.binterface.xml.XmlDataModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * GetDataCommandHandler 单元测试。
 */
class GetDataCommandHandlerTest {

    private GetDataCommandHandler handler;
    private StubLoginService loginService;
    private StubGetDataService getDataService;

    @BeforeEach
    void setUp() {
        loginService = new StubLoginService();
        getDataService = new StubGetDataService();
        handler = new GetDataCommandHandler(loginService, getDataService);
    }

    // ==================== 成功 ====================

    @Test
    void shouldHandleGetDataSuccessfully() {
        loginService.loggedIn = true;
        getDataService.successResult = true;

        CommandResult result = handler.handle(createContext("FSU-001", List.of("TEMP-001")));

        assertTrue(result.isSuccess());
        assertEquals("0", result.getResultCode());
        assertTrue(result.isImplemented());
        assertEquals(BInterfacePkType.GET_DATA, result.getPkType());
    }

    @Test
    void shouldReturnCountInResponse() {
        loginService.loggedIn = true;
        getDataService.successResult = true;

        CommandResult result = handler.handle(createContext("FSU-001", List.of("TEMP-001")));

        assertNotNull(result.getResponseInfoXml());
        assertTrue(result.getResponseInfoXml().contains("Count"));
    }

    @Test
    void shouldIncludeSignalsInXmlData() {
        loginService.loggedIn = true;
        getDataService.successResult = true;

        CommandResult result = handler.handle(createContext("FSU-001", List.of("TEMP-001", "HUMI-001")));

        String xmlData = result.toXmlDataXml();
        assertNotNull(xmlData);
        assertTrue(xmlData.contains("TEMP-001"));
        assertTrue(xmlData.contains("HUMI-001"));
    }

    // ==================== pkType ====================

    @Test
    void shouldSupportGetDataPkType() {
        assertEquals(BInterfacePkType.GET_DATA, handler.getSupportedPkType());
    }

    // ==================== 校验失败 ====================

    @Test
    void shouldFailOnNullContext() {
        CommandResult result = handler.handle(null);

        assertFalse(result.isSuccess());
        assertEquals("5001", result.getResultCode());
    }

    @Test
    void shouldFailOnMissingFsuCode() {
        loginService.loggedIn = true;

        CommandResult result = handler.handle(createContext(null, List.of("TEMP-001")));

        assertFalse(result.isSuccess());
        assertEquals("2001", result.getResultCode());
    }

    @Test
    void shouldFailOnNotLoggedIn() {
        loginService.loggedIn = false;
        getDataService.successResult = true;

        CommandResult result = handler.handle(createContext("FSU-001", List.of("TEMP-001")));

        assertFalse(result.isSuccess());
        assertEquals("1002", result.getResultCode());
    }

    @Test
    void shouldFailOnMissingSignalIds() {
        loginService.loggedIn = true;

        CommandResult result = handler.handle(createContext("FSU-001", List.of()));

        assertFalse(result.isSuccess());
        assertEquals("2003", result.getResultCode());
    }

    // ==================== Service 错误 ====================

    @Test
    void shouldFailWhenServiceReturnsError() {
        loginService.loggedIn = true;
        getDataService.successResult = false;
        getDataService.resultCodeToReturn = "5001";

        CommandResult result = handler.handle(createContext("FSU-001", List.of("TEMP-001")));

        assertFalse(result.isSuccess());
        assertEquals("5001", result.getResultCode());
    }

    // ==================== extractFsuCode ====================

    @Test
    void extractFsuCodeShouldReturnCorrectValue() {
        String fsuCode = GetDataCommandHandler.extractFsuCode("<FSUCode>FSU-001</FSUCode>");
        assertEquals("FSU-001", fsuCode);
    }

    @Test
    void extractFsuCodeShouldBeCaseInsensitive() {
        String fsuCode = GetDataCommandHandler.extractFsuCode("<fsucode>FSU-001</fsucode>");
        assertEquals("FSU-001", fsuCode);
    }

    @Test
    void extractFsuCodeShouldReturnNullForEmptyInput() {
        assertNull(GetDataCommandHandler.extractFsuCode(null));
        assertNull(GetDataCommandHandler.extractFsuCode(""));
    }

    @Test
    void extractFsuCodeShouldHandleWhitespace() {
        String fsuCode = GetDataCommandHandler.extractFsuCode(
                "<FSUCode>  FSU-001  </FSUCode>");
        assertEquals("FSU-001", fsuCode); // extractFsuCode 内部已 trim
    }

    @Test
    void extractFsuCodeShouldReturnNullWhenNotFound() {
        String fsuCode = GetDataCommandHandler.extractFsuCode("<ResultCode>0</ResultCode>");
        assertNull(fsuCode);
    }

    // ==================== Helper ====================

    private CommandContext createContext(String fsuCode, List<String> signalIds) {
        BInterfaceMessage msg = new BInterfaceMessage();
        if (fsuCode != null) {
            msg.setInfo("<FSUCode>" + fsuCode + "</FSUCode>");
        } else {
            msg.setInfo("<ResultCode>0</ResultCode>");
        }

        XmlDataModel xmlData = new XmlDataModel();
        xmlData.setRootName("SignalID");
        for (String id : signalIds) {
            LinkedHashMap<String, String> item = new LinkedHashMap<>();
            item.put("SignalID", id);
            xmlData.addItem(item);
        }

        return new CommandContext(BInterfacePkType.GET_DATA, msg, xmlData, null, null);
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

    static class StubGetDataService extends GetDataService {
        boolean successResult = true;
        String resultCodeToReturn = "0";

        StubGetDataService() {
            super(null, null);
        }

        @Override
        public GetDataResult execute(String fsuCode, String fsuServiceUrl, List<String> signalIds) {
            if (successResult) {
                return GetDataResult.success(fsuCode, List.of(
                        new GetDataResult.SignalValue("TEMP-001", "25.8", "1", "NORMAL", "2026-05-13T10:35:00+08:00"),
                        new GetDataResult.SignalValue("HUMI-001", "54.5", "1", "NORMAL", "2026-05-13T10:35:00+08:00")
                ));
            }
            return GetDataResult.fail(resultCodeToReturn, "服务错误", fsuCode);
        }
    }
}
