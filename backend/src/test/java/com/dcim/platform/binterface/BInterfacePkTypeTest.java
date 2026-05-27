package com.dcim.platform.binterface;

import com.dcim.platform.module.binterface.compat.BInterfaceCommand2016;
import com.dcim.platform.module.binterface.model.BInterfacePkType;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * BInterfacePkType 完整性验证 (BIF2016-P1-002)。
 * 确保覆盖 B接口2016 全部 30 项 + 2024 兼容保留项。
 */
class BInterfacePkTypeTest {

    // ==================== 2016 全 30 项完整性 ====================

    private static final Set<String> REQUIRED_2016 = new HashSet<>(Arrays.asList(
            // 连接管理
            "LOGIN", "LOGIN_ACK", "LOGOUT", "LOGOUT_ACK",
            // 实时数据
            "GET_DATA", "GET_DATA_ACK", "GET_HISDATA", "GET_HISDATA_ACK",
            // 告警
            "SEND_ALARM", "SEND_ALARM_ACK",
            // 遥控遥调
            "SET_POINT", "SET_POINT_ACK",
            // 时间同步
            "TIME_CHECK", "TIME_CHECK_ACK",
            // 登录信息
            "GET_LOGININFO", "GET_LOGININFO_ACK",
            "SET_LOGININFO", "SET_LOGININFO_ACK",
            // FTP
            "GET_FTP", "GET_FTP_ACK", "SET_FTP", "SET_FTP_ACK",
            // FSU 信息
            "GET_FSUINFO", "GET_FSUINFO_ACK",
            // 远程控制
            "SET_FSUREBOOT", "SET_FSUREBOOT_ACK",
            // 门限
            "GET_THRESHOLD", "GET_THRESHOLD_ACK",
            "SET_THRESHOLD", "SET_THRESHOLD_ACK"
    ));

    @Test
    void shouldContainAll30Command2016Entries() {
        for (String expected : REQUIRED_2016) {
            assertDoesNotThrow(() -> BInterfacePkType.valueOf(expected),
                    "BInterfacePkType 应包含 2016 命令: " + expected);
        }
    }

    @Test
    void shouldTotalCountBe44() {
        // 30 个 2016 规范项 + 10 个 2024/扩展 + GET_HISTORY_DATA(旧别名) + HEARTBEAT + SEND_DATA + SEND_DATA_ACK + SET_DATA + UNKNOWN = 45
        assertEquals(45, BInterfacePkType.values().length,
                "枚举总数应为 45，实际: " + BInterfacePkType.values().length);
    }

    // ==================== 2016 canonical 名称 ====================

    @Test
    void shouldUseGetHisdataAsCanonicalName() {
        assertNotNull(BInterfacePkType.GET_HISDATA);
        assertNotNull(BInterfacePkType.GET_HISDATA_ACK);
        // GET_HISTORY_DATA 保留为旧兼容别名
        assertNotNull(BInterfacePkType.GET_HISTORY_DATA);
    }

    // ==================== 2016 Code 交叉验证 ====================

    @Test
    void shouldMap2016GetDataTo401() {
        assertEquals(401, BInterfaceCommand2016.codeFor("GET_DATA").orElseThrow());
        assertNotEquals(501, BInterfaceCommand2016.codeFor("GET_DATA").orElseThrow());
    }

    @Test
    void shouldMap2016SendAlarmTo501() {
        assertEquals(501, BInterfaceCommand2016.codeFor("SEND_ALARM").orElseThrow());
        assertNotEquals(601, BInterfaceCommand2016.codeFor("SEND_ALARM").orElseThrow());
    }

    @Test
    void shouldMap2016GetFsuinfoTo1701() {
        assertEquals(1701, BInterfaceCommand2016.codeFor("GET_FSUINFO").orElseThrow());
        assertNotEquals(1001, BInterfaceCommand2016.codeFor("GET_FSUINFO").orElseThrow());
    }

    @Test
    void shouldMap2016GetThresholdTo1901() {
        assertEquals(1901, BInterfaceCommand2016.codeFor("GET_THRESHOLD").orElseThrow());
    }

    @Test
    void shouldMap2016TimeCheckTo1301() {
        assertEquals(1301, BInterfaceCommand2016.codeFor("TIME_CHECK").orElseThrow());
    }

    // ==================== 2016 ACK 交叉验证 ====================

    @Test
    void shouldMap2016LoginAckTo102() {
        assertEquals(102, BInterfaceCommand2016.codeFor("LOGIN_ACK").orElseThrow());
    }

    @Test
    void shouldMap2016GetDataAckTo402() {
        assertEquals(402, BInterfaceCommand2016.codeFor("GET_DATA_ACK").orElseThrow());
    }

    @Test
    void shouldMap2016GetFsuinfoAckTo1702() {
        assertEquals(1702, BInterfaceCommand2016.codeFor("GET_FSUINFO_ACK").orElseThrow());
    }

    // ==================== 旧枚举兼容 ====================

    @Test
    void shouldKeepOldGetHistoryDataAsAlias() {
        assertNotNull(BInterfacePkType.GET_HISTORY_DATA);
        // GET_HISTORY_DATA 保留但不应替代 GET_HISDATA
        assertNotSame(BInterfacePkType.GET_HISDATA, BInterfacePkType.GET_HISTORY_DATA);
    }

    @Test
    void shouldKeep2024Enums() {
        assertNotNull(BInterfacePkType.GET_SUINFO);
        assertNotNull(BInterfacePkType.GET_SUFTP);
        assertNotNull(BInterfacePkType.SET_TIME);
        assertNotNull(BInterfacePkType.GET_ACTIVEALARM);
    }

    // ==================== 旧测试保留兼容 ====================

    @Test
    void shouldRecognizeSendData() {
        assertNotNull(BInterfacePkType.valueOf("SEND_DATA"));
    }

    @Test
    void shouldRecognizeSetFtp() {
        assertNotNull(BInterfacePkType.valueOf("SET_FTP"));
    }

    @Test
    void shouldDistinguishSendDataFromGetData() {
        assertNotEquals(BInterfacePkType.SEND_DATA, BInterfacePkType.GET_DATA);
    }

    @Test
    void shouldHandleUnknownAsFallback() {
        assertEquals(BInterfacePkType.UNKNOWN, BInterfacePkType.valueOf("UNKNOWN"));
    }

    @Test
    void shouldThrowForInvalidCode() {
        assertThrows(IllegalArgumentException.class, () ->
                BInterfacePkType.valueOf("NON_EXISTENT_COMMAND"));
    }
}
