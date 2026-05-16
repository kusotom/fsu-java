package com.dcim.platform.binterface;

import com.dcim.platform.module.binterface.command.*;
import com.dcim.platform.module.binterface.model.BInterfaceMessage;
import com.dcim.platform.module.binterface.model.BInterfacePkType;
import com.dcim.platform.module.binterface.xml.XmlDataParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * B接口 CommandDispatcher 错误处理测试。
 *
 * 覆盖：
 * <ol>
 *   <li>畸形 xmlData → 结构化失败</li>
 *   <li>Handler 异常 → 结构化失败 (不穿透)</li>
 *   <li>Handler 返回 null → 结构化失败</li>
 *   <li>Unregistered PK_Type → UnknownCommandHandler</li>
 *   <li>UNKNOWN 真实 Handler 行为</li>
 * </ol>
 */
class CommandDispatcherErrorTest {

    private CommandDispatcher dispatcher;

    private XmlDataParser xmlDataParser;

    @BeforeEach
    void setUp() {
        xmlDataParser = new XmlDataParser();
        List<CommandHandler> handlers = new ArrayList<>();
        handlers.add(createTestHandler(BInterfacePkType.LOGIN, false, false));
        handlers.add(createTestHandler(BInterfacePkType.HEARTBEAT, false, false));
        handlers.add(createTestHandler(BInterfacePkType.SEND_DATA, false, false));

        UnknownCommandHandler fallback = new UnknownCommandHandler();
        dispatcher = new CommandDispatcher(handlers, fallback, xmlDataParser);
    }

    @Test
    void shouldHandleMalformedXmlData() {
        // xmlData 包含畸形的 XML 内容
        BInterfaceMessage msg = createMessage(BInterfacePkType.LOGIN, "<unclosed>broken");
        CommandResult result = dispatcher.dispatch(msg);

        assertNotNull(result);
        assertFalse(result.isSuccess());
        assertTrue(result.getResultCode().equals("2001") || result.getResultCode().equals("5001"));
    }

    @Test
    void shouldHandleHandlerException() {
        CommandHandler throwingHandler = new CommandHandler() {
            @Override
            public BInterfacePkType getSupportedPkType() {
                return BInterfacePkType.SET_POINT;
            }

            @Override
            public CommandResult handle(CommandContext context) {
                throw new RuntimeException("模拟 Handler 异常");
            }
        };

        List<CommandHandler> handlers = new ArrayList<>();
        handlers.add(throwingHandler);
        CommandDispatcher testDispatcher = new CommandDispatcher(
                handlers, new UnknownCommandHandler(), xmlDataParser);

        BInterfaceMessage msg = createMessage(BInterfacePkType.SET_POINT, "");
        CommandResult result = testDispatcher.dispatch(msg);

        assertNotNull(result);
        assertFalse(result.isSuccess());
        assertEquals("5001", result.getResultCode());
    }

    @Test
    void shouldHandleHandlerReturningNull() {
        CommandHandler nullHandler = new CommandHandler() {
            @Override
            public BInterfacePkType getSupportedPkType() {
                return BInterfacePkType.GET_DATA;
            }

            @Override
            public CommandResult handle(CommandContext context) {
                return null;
            }
        };

        List<CommandHandler> handlers = new ArrayList<>();
        handlers.add(nullHandler);
        CommandDispatcher testDispatcher = new CommandDispatcher(
                handlers, new UnknownCommandHandler(), xmlDataParser);

        BInterfaceMessage msg = createMessage(BInterfacePkType.GET_DATA, "");
        CommandResult result = testDispatcher.dispatch(msg);

        assertNotNull(result);
        assertFalse(result.isSuccess());
        assertEquals("5001", result.getResultCode());
    }

    @Test
    void shouldHandleUnregisteredPkType() {
        // GET_HISTORY_DATA 未注册，应走 UNKNOWN 兜底
        BInterfaceMessage msg = createMessage(BInterfacePkType.GET_HISTORY_DATA, "");
        CommandResult result = dispatcher.dispatch(msg);

        assertNotNull(result);
        assertEquals(BInterfacePkType.UNKNOWN, result.getPkType());
        assertEquals("1002", result.getResultCode());
    }

    @Test
    void unknownHandlerShouldReturnErrorForNullPkType() {
        BInterfaceMessage msg = new BInterfaceMessage();
        msg.setPkType(null);
        msg.setXmlData("");

        CommandResult result = dispatcher.dispatch(msg);
        assertNotNull(result);
        assertEquals(BInterfacePkType.UNKNOWN, result.getPkType());
        assertEquals("1002", result.getResultCode());
    }

    @Test
    void shouldHandleGetFsuInfoAsUnknown() {
        // GET_FSUINFO 存在于枚举但未注册 Handler
        CommandResult result = dispatcher.dispatch(
                createMessage(BInterfacePkType.GET_FSUINFO, ""));
        assertEquals(BInterfacePkType.UNKNOWN, result.getPkType());
    }

    // ==================== 工具方法 ====================

    private BInterfaceMessage createMessage(BInterfacePkType pkType, String xmlDataContent) {
        BInterfaceMessage msg = new BInterfaceMessage();
        msg.setPkType(pkType);
        msg.setXmlData(xmlDataContent);
        return msg;
    }

    private CommandHandler createTestHandler(BInterfacePkType pkType,
                                              boolean throwException,
                                              boolean returnNull) {
        return new CommandHandler() {
            @Override
            public BInterfacePkType getSupportedPkType() {
                return pkType;
            }

            @Override
            public CommandResult handle(CommandContext context) {
                if (throwException) {
                    throw new RuntimeException("test exception");
                }
                if (returnNull) {
                    return null;
                }
                return CommandResult.success(pkType);
            }
        };
    }
}
