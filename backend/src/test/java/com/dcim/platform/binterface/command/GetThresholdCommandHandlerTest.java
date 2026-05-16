package com.dcim.platform.binterface.command;

import com.dcim.platform.module.binterface.command.CommandContext;
import com.dcim.platform.module.binterface.command.CommandResult;
import com.dcim.platform.module.binterface.command.GetThresholdCommandHandler;
import com.dcim.platform.module.binterface.model.BInterfaceMessage;
import com.dcim.platform.module.binterface.model.BInterfacePkType;
import com.dcim.platform.module.binterface.entity.BInterfaceFsuStatusEntity;
import com.dcim.platform.module.binterface.service.GetThresholdResult;
import com.dcim.platform.module.binterface.service.GetThresholdService;
import com.dcim.platform.module.binterface.service.LoginService;
import com.dcim.platform.module.binterface.xml.XmlDataModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * GetThresholdCommandHandler 单元测试。
 */
class GetThresholdCommandHandlerTest {

    private GetThresholdCommandHandler handler;
    private StubLoginService loginService;
    private StubGetThresholdService getThresholdService;

    @BeforeEach
    void setUp() {
        loginService = new StubLoginService();
        getThresholdService = new StubGetThresholdService();
        handler = new GetThresholdCommandHandler(loginService, getThresholdService);
    }

    // ==================== 成功 ====================

    @Test
    void shouldHandleGetThresholdSuccessfully() {
        loginService.loggedIn = true;
        getThresholdService.successResult = true;

        CommandResult result = handler.handle(createContext("FSU-001", List.of("TEMP-001")));

        assertTrue(result.isSuccess());
        assertEquals("0", result.getResultCode());
        assertTrue(result.isImplemented());
        assertEquals(BInterfacePkType.GET_THRESHOLD, result.getPkType());
    }

    @Test
    void shouldReturnCountInResponse() {
        loginService.loggedIn = true;
        getThresholdService.successResult = true;

        CommandResult result = handler.handle(createContext("FSU-001", List.of("TEMP-001")));

        assertNotNull(result.getResponseInfoXml());
        assertTrue(result.getResponseInfoXml().contains("Count"));
    }

    @Test
    void shouldIncludeThresholdsInXmlData() {
        loginService.loggedIn = true;
        getThresholdService.successResult = true;

        CommandResult result = handler.handle(createContext("FSU-001", List.of("TEMP-001", "HUMI-001")));

        String xmlData = result.toXmlDataXml();
        assertNotNull(xmlData);
        assertTrue(xmlData.contains("TEMP-001"));
        assertTrue(xmlData.contains("HUMI-001"));
    }

    // ==================== pkType ====================

    @Test
    void shouldSupportGetThresholdPkType() {
        assertEquals(BInterfacePkType.GET_THRESHOLD, handler.getSupportedPkType());
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
        getThresholdService.successResult = true;

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
        getThresholdService.successResult = false;
        getThresholdService.resultCodeToReturn = "5001";

        CommandResult result = handler.handle(createContext("FSU-001", List.of("TEMP-001")));

        assertFalse(result.isSuccess());
        assertEquals("5001", result.getResultCode());
    }

    // ==================== extractFsuCode ====================

    @Test
    void extractFsuCodeShouldReturnCorrectValue() {
        String fsuCode = GetThresholdCommandHandler.extractFsuCode("<FSUCode>FSU-001</FSUCode>");
        assertEquals("FSU-001", fsuCode);
    }

    @Test
    void extractFsuCodeShouldBeCaseInsensitive() {
        String fsuCode = GetThresholdCommandHandler.extractFsuCode("<fsucode>FSU-001</fsucode>");
        assertEquals("FSU-001", fsuCode);
    }

    @Test
    void extractFsuCodeShouldReturnNullForEmptyInput() {
        assertNull(GetThresholdCommandHandler.extractFsuCode(null));
        assertNull(GetThresholdCommandHandler.extractFsuCode(""));
    }

    @Test
    void extractFsuCodeShouldHandleWhitespace() {
        String fsuCode = GetThresholdCommandHandler.extractFsuCode(
                "<FSUCode>  FSU-001  </FSUCode>");
        assertEquals("FSU-001", fsuCode);
    }

    @Test
    void extractFsuCodeShouldReturnNullWhenNotFound() {
        String fsuCode = GetThresholdCommandHandler.extractFsuCode("<ResultCode>0</ResultCode>");
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

        return new CommandContext(BInterfacePkType.GET_THRESHOLD, msg, xmlData, null, null);
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

    static class StubGetThresholdService extends GetThresholdService {
        boolean successResult = true;
        String resultCodeToReturn = "0";

        StubGetThresholdService() {
            super(null, null);
        }

        @Override
        public GetThresholdResult execute(String fsuCode, String fsuServiceUrl, List<String> signalIds) {
            if (successResult) {
                return GetThresholdResult.success(fsuCode, List.of(
                        new GetThresholdResult.ThresholdValue("TEMP-001", "60.0", "-5.0", "70.0", "-10.0"),
                        new GetThresholdResult.ThresholdValue("HUMI-001", "90.0", "10.0", "95.0", "5.0")
                ));
            }
            return GetThresholdResult.fail(resultCodeToReturn, "服务错误", fsuCode);
        }
    }
}
