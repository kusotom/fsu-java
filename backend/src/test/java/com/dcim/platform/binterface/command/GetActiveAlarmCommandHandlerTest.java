package com.dcim.platform.binterface.command;

import com.dcim.platform.module.binterface.command.CommandContext;
import com.dcim.platform.module.binterface.command.CommandResult;
import com.dcim.platform.module.binterface.command.GetActiveAlarmCommandHandler;
import com.dcim.platform.module.binterface.model.BInterfaceMessage;
import com.dcim.platform.module.binterface.model.BInterfacePkType;
import com.dcim.platform.module.binterface.service.GetActiveAlarmResult;
import com.dcim.platform.module.binterface.service.GetActiveAlarmService;
import com.dcim.platform.module.binterface.service.LoginService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * GetActiveAlarmCommandHandler 单元测试 (BIF-P4-FIX-001)。
 */
class GetActiveAlarmCommandHandlerTest {

    private GetActiveAlarmCommandHandler handler;

    @BeforeEach
    void setUp() {
        StubLoginService loginService = new StubLoginService();
        loginService.loggedIn = true;
        GetActiveAlarmService svc = new GetActiveAlarmService(null, null) {
            @Override
            public GetActiveAlarmResult execute(String fsuCode, String url) {
                var item = new GetActiveAlarmResult.ActiveAlarmItem(
                        "SN001", fsuCode, "DEV1", "SP01",
                        null, null, "46.1", "一级", "开始", "desc", null);
                return GetActiveAlarmResult.success(fsuCode, List.of(item), 1, 1, 0);
            }
        };
        handler = new GetActiveAlarmCommandHandler(loginService, svc);
    }

    @Test
    void shouldFailForMissingSuid() {
        CommandResult r = handler.handle(createContext("<Info></Info>"));
        assertFalse(r.isSuccess());
        assertEquals("2001", r.getResultCode());
        assertTrue(r.getResultDesc().contains("SUID"), "应提示缺少 SUID: " + r.getResultDesc());
    }

    @Test
    void shouldFailForNullContext() {
        CommandResult r = handler.handle(null);
        assertFalse(r.isSuccess());
    }

    @Test
    void shouldFailForNullInfo() {
        BInterfaceMessage msg = new BInterfaceMessage();
        msg.setInfo(null);
        msg.setPkType(BInterfacePkType.GET_ACTIVEALARM);
        CommandContext ctx = new CommandContext(BInterfacePkType.GET_ACTIVEALARM, msg, null, null, null);
        CommandResult r = handler.handle(ctx);
        assertFalse(r.isSuccess());
    }

    @Test
    void shouldHandleValidContext() {
        CommandResult r = handler.handle(createContext("<Info><SUID>FSU-001</SUID></Info>"));
        assertTrue(r.isSuccess());
        assertEquals("0", r.getResultCode());
        assertEquals(BInterfacePkType.GET_ACTIVEALARM, r.getPkType());
    }

    private CommandContext createContext(String infoXml) {
        BInterfaceMessage msg = new BInterfaceMessage();
        msg.setPkType(BInterfacePkType.GET_ACTIVEALARM);
        msg.setInfo(infoXml);
        return new CommandContext(BInterfacePkType.GET_ACTIVEALARM, msg, null, null, null);
    }

    static class StubLoginService extends LoginService {
        boolean loggedIn = false;
        StubLoginService() { super(null, null, null); }
        @Override public boolean isLoggedIn(String fsuCode) { return loggedIn; }
    }
}
