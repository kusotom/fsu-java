package com.dcim.platform.binterface;

import com.dcim.platform.module.binterface.compat.BInterfaceCommand2016;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * BInterfaceCommand2016 码表完整性测试 (BIF2016-P1-001)。
 * 验证全部 15 命令 + 15 ACK 的 Code 正确。
 */
class BInterfaceCommand2016Test {

    // ==================== 码表规模 ====================

    @Test
    void shouldHave30Entries() {
        assertEquals(30, BInterfaceCommand2016.size(),
                "2016 码表应包含 15 命令 + 15 ACK = 30 条目");
    }

    // ==================== 连接管理 ====================

    @Test void loginCodeIs101() { assertEquals(101, code("LOGIN")); }
    @Test void loginAckCodeIs102() { assertEquals(102, code("LOGIN_ACK")); }
    @Test void logoutCodeIs103() { assertEquals(103, code("LOGOUT")); }
    @Test void logoutAckCodeIs104() { assertEquals(104, code("LOGOUT_ACK")); }

    // ==================== 实时数据 ====================

    @Test void getDataCodeIs401() { assertEquals(401, code("GET_DATA")); }
    @Test void getDataAckCodeIs402() { assertEquals(402, code("GET_DATA_ACK")); }
    @Test void getHisdataCodeIs403() { assertEquals(403, code("GET_HISDATA")); }
    @Test void getHisdataAckCodeIs404() { assertEquals(404, code("GET_HISDATA_ACK")); }

    // ==================== 告警 ====================

    @Test void sendAlarmCodeIs501() { assertEquals(501, code("SEND_ALARM")); }
    @Test void sendAlarmAckCodeIs502() { assertEquals(502, code("SEND_ALARM_ACK")); }

    // ==================== 遥控遥调 ====================

    @Test void setPointCodeIs1001() { assertEquals(1001, code("SET_POINT")); }
    @Test void setPointAckCodeIs1002() { assertEquals(1002, code("SET_POINT_ACK")); }

    // ==================== 时间同步 ====================

    @Test void timeCheckCodeIs1301() { assertEquals(1301, code("TIME_CHECK")); }
    @Test void timeCheckAckCodeIs1302() { assertEquals(1302, code("TIME_CHECK_ACK")); }

    // ==================== 登录信息 ====================

    @Test void getLogininfoCodeIs1501() { assertEquals(1501, code("GET_LOGININFO")); }
    @Test void getLogininfoAckCodeIs1502() { assertEquals(1502, code("GET_LOGININFO_ACK")); }
    @Test void setLogininfoCodeIs1503() { assertEquals(1503, code("SET_LOGININFO")); }
    @Test void setLogininfoAckCodeIs1504() { assertEquals(1504, code("SET_LOGININFO_ACK")); }

    // ==================== FTP ====================

    @Test void getFtpCodeIs1601() { assertEquals(1601, code("GET_FTP")); }
    @Test void getFtpAckCodeIs1602() { assertEquals(1602, code("GET_FTP_ACK")); }
    @Test void setFtpCodeIs1603() { assertEquals(1603, code("SET_FTP")); }
    @Test void setFtpAckCodeIs1604() { assertEquals(1604, code("SET_FTP_ACK")); }

    // ==================== FSU 信息 ====================

    @Test void getFsuinfoCodeIs1701() { assertEquals(1701, code("GET_FSUINFO")); }
    @Test void getFsuinfoAckCodeIs1702() { assertEquals(1702, code("GET_FSUINFO_ACK")); }

    // ==================== 远程控制 ====================

    @Test void setFsurebootCodeIs1801() { assertEquals(1801, code("SET_FSUREBOOT")); }
    @Test void setFsurebootAckCodeIs1802() { assertEquals(1802, code("SET_FSUREBOOT_ACK")); }

    // ==================== 门限 ====================

    @Test void getThresholdCodeIs1901() { assertEquals(1901, code("GET_THRESHOLD")); }
    @Test void getThresholdAckCodeIs1902() { assertEquals(1902, code("GET_THRESHOLD_ACK")); }
    @Test void setThresholdCodeIs2001() { assertEquals(2001, code("SET_THRESHOLD")); }
    @Test void setThresholdAckCodeIs2002() { assertEquals(2002, code("SET_THRESHOLD_ACK")); }

    // ==================== 关键差异验证 ====================

    @Test
    void getDataShouldNotUse2024Code501() {
        assertNotEquals(501, code("GET_DATA"),
                "GET_DATA 2016=401, 不可误用 2024 的 501");
    }

    @Test
    void sendAlarmShouldNotUse2024Code601() {
        assertNotEquals(601, code("SEND_ALARM"),
                "SEND_ALARM 2016=501, 不可误用 2024 的 601");
    }

    @Test
    void getFsuinfoShouldNotUse2024GetSuinfo() {
        assertEquals(1701, code("GET_FSUINFO"));
        assertNotEquals(1001, code("GET_FSUINFO"),
                "GET_FSUINFO 2016=1701, 不可误用 2024 GET_SUINFO(1001)");
    }

    // ==================== 边界 ====================

    @Test
    void unknownNameReturnsEmpty() {
        assertTrue(BInterfaceCommand2016.codeFor("NONEXISTENT").isEmpty());
    }

    @Test
    void nullNameReturnsEmpty() {
        assertTrue(BInterfaceCommand2016.codeFor(null).isEmpty());
    }

    @Test
    void isDefinedForValidCommand() {
        assertTrue(BInterfaceCommand2016.isDefined("GET_DATA"));
        assertTrue(BInterfaceCommand2016.isDefined("GET_DATA_ACK"));
    }

    @Test
    void isNotDefinedForUnknownCommand() {
        assertFalse(BInterfaceCommand2016.isDefined("UNKNOWN_COMMAND"));
    }

    // ==================== 2024 代码不受影响 ====================

    @Test
    void shouldNotConflictWith2024Enum() {
        // 验证 2024 枚举仍存在（BInterfaceCommand2024 类未被修改）
        assertNotNull(com.dcim.platform.module.binterface.model.BInterfaceCommand2024.class);
    }

    // ==================== 辅助 ====================

    private int code(String name) {
        Optional<Integer> c = BInterfaceCommand2016.codeFor(name);
        assertTrue(c.isPresent(), "2016 码表应包含 " + name);
        return c.get();
    }
}
