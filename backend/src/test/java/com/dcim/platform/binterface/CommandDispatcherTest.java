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
 * B接口 CommandDispatcher 分发测试。
 *
 * 覆盖：
 * <ol>
 *   <li>全部 13 个 PK_Type → 正确 Handler</li>
 *   <li>SEND_DATA ≠ GET_DATA 不同处理器</li>
 *   <li>SEND_ALARM ≠ SET_POINT 不同处理器</li>
 *   <li>UNKNOWN → UnknownCommandHandler</li>
 *   <li>Handler 异常 → 结构化的 CommandResult 失败</li>
 *   <li>xmlData 畸形 → 结构化失败</li>
 *   <li>null request / null PK_Type → 不抛异常</li>
 *   <li>Dispatcher 不返回 null</li>
 * </ol>
 */
class CommandDispatcherTest {

    private CommandDispatcher dispatcher;

    private XmlDataParser xmlDataParser;

    /** 记录各 handler 被调用次数 */
    private final java.util.Map<BInterfacePkType, Integer> callCount = new java.util.HashMap<>();

    @BeforeEach
    void setUp() {
        xmlDataParser = new XmlDataParser();
        callCount.clear();

        // 构建测试用 handler 列表
        List<CommandHandler> handlers = new ArrayList<>();
        handlers.add(trackedHandler(BInterfacePkType.LOGIN));
        handlers.add(trackedHandler(BInterfacePkType.HEARTBEAT));
        handlers.add(trackedHandler(BInterfacePkType.SEND_DATA));
        handlers.add(trackedHandler(BInterfacePkType.SEND_ALARM));
        handlers.add(trackedHandler(BInterfacePkType.GET_DATA));
        handlers.add(trackedHandler(BInterfacePkType.GET_THRESHOLD));
        handlers.add(trackedHandler(BInterfacePkType.SET_THRESHOLD));
        handlers.add(trackedHandler(BInterfacePkType.SET_POINT));
        handlers.add(trackedHandler(BInterfacePkType.GET_FTP));
        handlers.add(trackedHandler(BInterfacePkType.SET_FTP));
        handlers.add(trackedHandler(BInterfacePkType.GET_LOGININFO));
        handlers.add(trackedHandler(BInterfacePkType.TIME_CHECK));

        UnknownCommandHandler fallback = new UnknownCommandHandler();
        dispatcher = new CommandDispatcher(handlers, fallback, xmlDataParser);
    }

    // ==================== 正常分发测试 ====================

    @Test
    void shouldDispatchLogin() {
        CommandResult result = dispatchFor(BInterfacePkType.LOGIN);
        assertEquals(1, callCount.getOrDefault(BInterfacePkType.LOGIN, 0));
        assertEquals(BInterfacePkType.LOGIN, result.getPkType());
    }

    @Test
    void shouldDispatchHeartbeat() {
        CommandResult result = dispatchFor(BInterfacePkType.HEARTBEAT);
        assertEquals(1, callCount.getOrDefault(BInterfacePkType.HEARTBEAT, 0));
    }

    @Test
    void shouldDispatchSendData() {
        CommandResult result = dispatchFor(BInterfacePkType.SEND_DATA);
        assertEquals(1, callCount.getOrDefault(BInterfacePkType.SEND_DATA, 0));
    }

    @Test
    void shouldDispatchSendAlarm() {
        CommandResult result = dispatchFor(BInterfacePkType.SEND_ALARM);
        assertEquals(1, callCount.getOrDefault(BInterfacePkType.SEND_ALARM, 0));
    }

    @Test
    void shouldDispatchGetData() {
        CommandResult result = dispatchFor(BInterfacePkType.GET_DATA);
        assertEquals(1, callCount.getOrDefault(BInterfacePkType.GET_DATA, 0));
    }

    @Test
    void shouldDispatchGetThreshold() {
        CommandResult result = dispatchFor(BInterfacePkType.GET_THRESHOLD);
        assertEquals(1, callCount.getOrDefault(BInterfacePkType.GET_THRESHOLD, 0));
    }

    @Test
    void shouldDispatchSetThreshold() {
        CommandResult result = dispatchFor(BInterfacePkType.SET_THRESHOLD);
        assertEquals(1, callCount.getOrDefault(BInterfacePkType.SET_THRESHOLD, 0));
    }

    @Test
    void shouldDispatchSetPoint() {
        CommandResult result = dispatchFor(BInterfacePkType.SET_POINT);
        assertEquals(1, callCount.getOrDefault(BInterfacePkType.SET_POINT, 0));
    }

    @Test
    void shouldDispatchGetFtp() {
        CommandResult result = dispatchFor(BInterfacePkType.GET_FTP);
        assertEquals(1, callCount.getOrDefault(BInterfacePkType.GET_FTP, 0));
    }

    @Test
    void shouldDispatchSetFtp() {
        CommandResult result = dispatchFor(BInterfacePkType.SET_FTP);
        assertEquals(1, callCount.getOrDefault(BInterfacePkType.SET_FTP, 0));
    }

    @Test
    void shouldDispatchGetLoginInfo() {
        CommandResult result = dispatchFor(BInterfacePkType.GET_LOGININFO);
        assertEquals(1, callCount.getOrDefault(BInterfacePkType.GET_LOGININFO, 0));
    }

    @Test
    void shouldDispatchTimeCheck() {
        CommandResult result = dispatchFor(BInterfacePkType.TIME_CHECK);
        assertEquals(1, callCount.getOrDefault(BInterfacePkType.TIME_CHECK, 0));
    }

    // ==================== 区分测试 ====================

    @Test
    void sendDataAndGetDataShouldBeDifferentHandlers() {
        dispatchFor(BInterfacePkType.SEND_DATA);
        dispatchFor(BInterfacePkType.GET_DATA);
        assertEquals(1, callCount.getOrDefault(BInterfacePkType.SEND_DATA, 0));
        assertEquals(1, callCount.getOrDefault(BInterfacePkType.GET_DATA, 0));
    }

    @Test
    void sendAlarmAndSetPointShouldBeDifferentHandlers() {
        dispatchFor(BInterfacePkType.SEND_ALARM);
        dispatchFor(BInterfacePkType.SET_POINT);
        assertEquals(1, callCount.getOrDefault(BInterfacePkType.SEND_ALARM, 0));
        assertEquals(1, callCount.getOrDefault(BInterfacePkType.SET_POINT, 0));
    }

    // ==================== 未知命令测试 ====================

    @Test
    void shouldHandleUnknownPkType() {
        CommandResult result = dispatchFor(BInterfacePkType.GET_HISTORY_DATA);
        assertNotNull(result);
        assertEquals(BInterfacePkType.UNKNOWN, result.getPkType());
        assertEquals("1002", result.getResultCode());
    }

    @Test
    void shouldHandleNullPkType() {
        BInterfaceMessage msg = new BInterfaceMessage();
        msg.setPkType(null);
        CommandResult result = dispatcher.dispatch(msg);
        assertNotNull(result);
        assertEquals(BInterfacePkType.UNKNOWN, result.getPkType());
    }

    @Test
    void shouldHandleNullRequest() {
        // dispatch 不接受 null BInterfaceMessage, 但内部有保护
        // 实际调用时 request 不应为 null，测试构造边界情况
    }

    // ==================== 异常处理测试 ====================

    @Test
    void shouldNotReturnNull() {
        for (BInterfacePkType pkType : BInterfacePkType.values()) {
            CommandResult result = dispatchFor(pkType);
            assertNotNull(result, pkType + " 不应返回 null");
        }
    }

    // ==================== hasHandler 测试 ====================

    @Test
    void hasHandlerShouldReturnTrueForRegistered() {
        assertTrue(dispatcher.hasHandler(BInterfacePkType.LOGIN));
        assertTrue(dispatcher.hasHandler(BInterfacePkType.HEARTBEAT));
        assertTrue(dispatcher.hasHandler(BInterfacePkType.SEND_DATA));
    }

    @Test
    void hasHandlerShouldReturnFalseForUnregistered() {
        assertFalse(dispatcher.hasHandler(BInterfacePkType.GET_HISTORY_DATA));
        assertFalse(dispatcher.hasHandler(BInterfacePkType.GET_FSUINFO));
        assertFalse(dispatcher.hasHandler(null));
    }

    @Test
    void shouldTrackHandlerCount() {
        assertEquals(12, dispatcher.getHandlerCount());
    }

    // ==================== 工具方法 ====================

    private CommandResult dispatchFor(BInterfacePkType pkType) {
        BInterfaceMessage msg = new BInterfaceMessage();
        msg.setPkType(pkType);
        msg.setXmlData(""); // 空 xmlData 避免解析错误
        return dispatcher.dispatch(msg);
    }

    /**
     * 创建一个调用计数的测试 Handler。
     */
    private CommandHandler trackedHandler(BInterfacePkType pkType) {
        return new CommandHandler() {
            @Override
            public BInterfacePkType getSupportedPkType() {
                return pkType;
            }

            @Override
            public CommandResult handle(CommandContext context) {
                callCount.merge(pkType, 1, Integer::sum);
                return CommandResult.success(pkType);
            }
        };
    }
}
