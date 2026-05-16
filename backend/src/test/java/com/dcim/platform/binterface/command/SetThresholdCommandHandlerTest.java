package com.dcim.platform.binterface.command;

import com.dcim.platform.module.binterface.command.CommandContext;
import com.dcim.platform.module.binterface.command.CommandResult;
import com.dcim.platform.module.binterface.command.SetThresholdCommandHandler;
import com.dcim.platform.module.binterface.model.BInterfaceMessage;
import com.dcim.platform.module.binterface.model.BInterfacePkType;
import com.dcim.platform.module.binterface.entity.BInterfaceFsuStatusEntity;
import com.dcim.platform.module.binterface.service.*;
import com.dcim.platform.module.binterface.xml.XmlDataModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SetThresholdCommandHandler 单元测试。
 */
class SetThresholdCommandHandlerTest {

    private SetThresholdCommandHandler handler;
    private StubLoginService loginService;
    private StubSafetyGate safetyGate;
    private StubAuditService auditService;
    private StubSetThresholdService setThresholdService;

    @BeforeEach
    void setUp() {
        loginService = new StubLoginService();
        safetyGate = new StubSafetyGate();
        auditService = new StubAuditService();
        setThresholdService = new StubSetThresholdService();
        handler = new SetThresholdCommandHandler(loginService, safetyGate, auditService, setThresholdService);
    }

    // ==================== 成功（门禁通过） ====================

    @Test
    void shouldSetThresholdWhenSafetyPasses() {
        loginService.loggedIn = true;
        safetyGate.allowed = true;
        setThresholdService.successResult = true;

        CommandResult result = handler.handle(createContext("FSU-001", "TEMP-001", true));

        assertTrue(result.isSuccess());
        assertEquals("0", result.getResultCode());
        assertTrue(result.isImplemented());
        assertEquals(BInterfacePkType.SET_THRESHOLD, result.getPkType());
    }

    @Test
    void shouldRecordAuditOnSuccess() {
        loginService.loggedIn = true;
        safetyGate.allowed = true;
        setThresholdService.successResult = true;

        handler.handle(createContext("FSU-001", "TEMP-001", true));

        assertTrue(auditService.lastRecorded);
        assertNotNull(auditService.lastRecord);
    }

    // ==================== pkType ====================

    @Test
    void shouldSupportSetThresholdPkType() {
        assertEquals(BInterfacePkType.SET_THRESHOLD, handler.getSupportedPkType());
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
        safetyGate.allowed = true;

        CommandResult result = handler.handle(createContext(null, "TEMP-001", true));

        assertFalse(result.isSuccess());
        assertEquals("2001", result.getResultCode());
    }

    @Test
    void shouldFailOnNotLoggedIn() {
        loginService.loggedIn = false;
        safetyGate.allowed = true;

        CommandResult result = handler.handle(createContext("FSU-001", "TEMP-001", true));

        assertFalse(result.isSuccess());
        assertEquals("1002", result.getResultCode());
    }

    @Test
    void shouldFailOnMissingSignalId() {
        loginService.loggedIn = true;
        safetyGate.allowed = true;

        BInterfaceMessage msg = new BInterfaceMessage();
        msg.setInfo("<FSUCode>FSU-001</FSUCode>");
        XmlDataModel xmlData = new XmlDataModel();
        xmlData.setRootName("Signal");

        CommandResult result = handler.handle(
                new CommandContext(BInterfacePkType.SET_THRESHOLD, msg, xmlData, null, null));

        assertFalse(result.isSuccess());
        assertEquals("2003", result.getResultCode());
    }

    // ==================== 安全门禁拒绝 ====================

    @Test
    void shouldFailWhenSafetyGateDenies() {
        loginService.loggedIn = true;
        safetyGate.allowed = false;
        safetyGate.resultCodeToReturn = "3001";

        CommandResult result = handler.handle(createContext("FSU-001", "TEMP-001", true));

        assertFalse(result.isSuccess());
        assertEquals("3001", result.getResultCode());
    }

    @Test
    void shouldFailWhenNotConfirmed() {
        loginService.loggedIn = true;
        safetyGate.allowed = false;
        safetyGate.resultCodeToReturn = "3002";

        CommandResult result = handler.handle(createContext("FSU-001", "TEMP-001", false));

        assertFalse(result.isSuccess());
        assertEquals("3002", result.getResultCode());
    }

    // ==================== Service 错误 ====================

    @Test
    void shouldFailWhenServiceReturnsError() {
        loginService.loggedIn = true;
        safetyGate.allowed = true;
        setThresholdService.successResult = false;
        setThresholdService.resultCodeToReturn = "5001";

        CommandResult result = handler.handle(createContext("FSU-001", "TEMP-001", true));

        assertFalse(result.isSuccess());
        assertEquals("5001", result.getResultCode());
    }

    // ==================== extractFsuCode ====================

    @Test
    void extractFsuCodeShouldReturnCorrectValue() {
        String fsuCode = SetThresholdCommandHandler.extractFsuCode("<FSUCode>FSU-001</FSUCode>");
        assertEquals("FSU-001", fsuCode);
    }

    @Test
    void extractFsuCodeShouldReturnNullForEmptyInput() {
        assertNull(SetThresholdCommandHandler.extractFsuCode(null));
        assertNull(SetThresholdCommandHandler.extractFsuCode(""));
    }

    // ==================== Helper ====================

    private CommandContext createContext(String fsuCode, String signalId, boolean confirmed) {
        BInterfaceMessage msg = new BInterfaceMessage();
        if (fsuCode != null) {
            msg.setInfo("<FSUCode>" + fsuCode + "</FSUCode>");
        } else {
            msg.setInfo("<ResultCode>0</ResultCode>");
        }

        XmlDataModel xmlData = new XmlDataModel();
        xmlData.setRootName("Signal");
        if (signalId != null) {
            LinkedHashMap<String, String> item = new LinkedHashMap<>();
            item.put("SignalID", signalId);
            item.put("AlarmUpper", "65.0");
            item.put("AlarmLower", "-5.0");
            item.put("AlarmUpperUrgent", "75.0");
            item.put("AlarmLowerUrgent", "-10.0");
            xmlData.addItem(item);
        }

        CommandContext ctx = new CommandContext(BInterfacePkType.SET_THRESHOLD, msg, xmlData, null, null);
        ctx.setAttribute("confirmed", confirmed);
        return ctx;
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

    static class StubSafetyGate extends SetThresholdSafetyGate {
        boolean allowed = true;
        String resultCodeToReturn = "0";

        StubSafetyGate() {
            super(false, true);
        }

        @Override
        public SetThresholdSafetyDecision check(String fsuCode, boolean confirmed) {
            if (allowed) {
                return SetThresholdSafetyDecision.allow();
            }
            return SetThresholdSafetyDecision.deny(resultCodeToReturn, "门禁拒绝");
        }
    }

    static class StubAuditService extends SetThresholdAuditService {
        boolean lastRecorded = false;
        SetThresholdAuditRecord lastRecord;

        @Override
        public void record(SetThresholdAuditRecord record) {
            this.lastRecorded = true;
            this.lastRecord = record;
        }
    }

    static class StubSetThresholdService extends SetThresholdService {
        boolean successResult = true;
        String resultCodeToReturn = "0";

        StubSetThresholdService() {
            super(null, null);
        }

        @Override
        public SetThresholdResult execute(String fsuCode, String fsuServiceUrl,
                                          String signalId, String alarmUpper, String alarmLower,
                                          String alarmUpperUrgent, String alarmLowerUrgent) {
            if (successResult) {
                return SetThresholdResult.success(fsuCode, 1);
            }
            return SetThresholdResult.fail(resultCodeToReturn, "服务错误", fsuCode);
        }
    }
}
