package com.dcim.platform.binterface;

import com.dcim.platform.module.binterface.model.BInterfaceCommand2024;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * B接口 2024 命令枚举测试。
 *
 * <p>BIF-P4-010: 覆盖全部 44 条命令的 Name/Code/方向/类别/高风险标记。</p>
 */
class BInterfaceCommand2024Test {

    // ==================== 命令总数 ====================

    @Test
    void shouldHave44Commands() {
        assertEquals(44, BInterfaceCommand2024.commandCount(),
                "2024 标准应有 44 条命令 (22组 × 2)");
    }

    // ==================== Name + Code 双向查找 ====================

    @Test
    void shouldFindByName() {
        assertTrue(BInterfaceCommand2024.findByName("LOGIN").isPresent());
        assertTrue(BInterfaceCommand2024.findByName("GET_DATA_ACK").isPresent());
        assertTrue(BInterfaceCommand2024.findByName("SET_SUREBOOT").isPresent());
    }

    @Test
    void shouldFindByCode() {
        assertEquals("LOGIN", BInterfaceCommand2024.findByCode(101).get().getName());
        assertEquals("GET_DATA", BInterfaceCommand2024.findByCode(501).get().getName());
        assertEquals("GET_SUINFO", BInterfaceCommand2024.findByCode(1001).get().getName());
        assertEquals("SET_SUREBOOT", BInterfaceCommand2024.findByCode(1101).get().getName());
    }

    @Test
    void shouldReturnEmptyForUnknownCode() {
        assertTrue(BInterfaceCommand2024.findByCode(9999).isEmpty());
    }

    // ==================== 每个请求命令都有 ACK ====================

    @Test
    void eachRequestShouldHaveAck() {
        for (BInterfaceCommand2024 cmd : BInterfaceCommand2024.values()) {
            if (!cmd.isAck()) {
                String ackName = cmd.getName() + "_ACK";
                assertTrue(BInterfaceCommand2024.findByName(ackName).isPresent(),
                        "请求命令 " + cmd.getName() + " 应有对应的 ACK: " + ackName);
            }
        }
    }

    @Test
    void eachAckShouldHaveRequest() {
        for (BInterfaceCommand2024 cmd : BInterfaceCommand2024.values()) {
            if (cmd.isAck()) {
                String reqName = cmd.getName().replace("_ACK", "");
                assertTrue(BInterfaceCommand2024.findByName(reqName).isPresent(),
                        "ACK 命令 " + cmd.getName() + " 应有对应的请求: " + reqName);
            }
        }
    }

    // ==================== 关键 Code 值验证 ====================

    @Test
    void getDataCodeShouldBe501() { assertEquals(501, BInterfaceCommand2024.GET_DATA.getCode()); }
    @Test
    void getDataAckCodeShouldBe502() { assertEquals(502, BInterfaceCommand2024.GET_DATA_ACK.getCode()); }
    @Test
    void getSuinfoCodeShouldBe1001() { assertEquals(1001, BInterfaceCommand2024.GET_SUINFO.getCode()); }
    @Test
    void getSuinfoAckCodeShouldBe1002() { assertEquals(1002, BInterfaceCommand2024.GET_SUINFO_ACK.getCode()); }
    @Test
    void setTimeCodeShouldBe901() { assertEquals(901, BInterfaceCommand2024.SET_TIME.getCode()); }
    @Test
    void setTimeAckCodeShouldBe902() { assertEquals(902, BInterfaceCommand2024.SET_TIME_ACK.getCode()); }
    @Test
    void getSuftpCodeShouldBe801() { assertEquals(801, BInterfaceCommand2024.GET_SUFTP.getCode()); }
    @Test
    void getSuftpAckCodeShouldBe802() { assertEquals(802, BInterfaceCommand2024.GET_SUFTP_ACK.getCode()); }
    @Test
    void setRmctrlcmdCodeShouldBe701() { assertEquals(701, BInterfaceCommand2024.SET_RMCTRLCMD.getCode()); }
    @Test
    void setSurebootCodeShouldBe1101() { assertEquals(1101, BInterfaceCommand2024.SET_SUREBOOT.getCode()); }
    @Test
    void loginCodeShouldBe101() { assertEquals(101, BInterfaceCommand2024.LOGIN.getCode()); }
    @Test
    void sendAlarmCodeShouldBe601() { assertEquals(601, BInterfaceCommand2024.SEND_ALARM.getCode()); }

    // ==================== 方向验证 ====================

    @Test
    void loginShouldBeFsuToSc() {
        assertEquals(BInterfaceCommand2024.Direction.FSU_TO_SC, BInterfaceCommand2024.LOGIN.getDirection());
    }
    @Test
    void getDataShouldBeScToFsu() {
        assertEquals(BInterfaceCommand2024.Direction.SC_TO_FSU, BInterfaceCommand2024.GET_DATA.getDirection());
    }
    @Test
    void sendAlarmShouldBeFsuToSc() {
        assertEquals(BInterfaceCommand2024.Direction.FSU_TO_SC, BInterfaceCommand2024.SEND_ALARM.getDirection());
    }

    // ==================== 高风险命令标记 ====================

    @Test
    void shouldMarkHighRiskCommands() {
        assertTrue(BInterfaceCommand2024.SET_RMCTRLCMD.isHighRisk(), "SET_RMCTRLCMD 应为高风险");
        assertTrue(BInterfaceCommand2024.SET_SUFTP.isHighRisk(), "SET_SUFTP 应为高风险");
        assertTrue(BInterfaceCommand2024.SET_SUREBOOT.isHighRisk(), "SET_SUREBOOT 应为高风险");
        assertTrue(BInterfaceCommand2024.SET_SCHEMECONFIG.isHighRisk(), "SET_SCHEMECONFIG 应为高风险");
    }

    @Test
    void shouldNotMarkReadCommandsHighRisk() {
        assertFalse(BInterfaceCommand2024.GET_DATA.isHighRisk(), "GET_DATA 不应为高风险");
        assertFalse(BInterfaceCommand2024.GET_SUINFO.isHighRisk(), "GET_SUINFO 不应为高风险");
        assertFalse(BInterfaceCommand2024.GET_SUFTP.isHighRisk(), "GET_SUFTP 不应为高风险");
        assertFalse(BInterfaceCommand2024.SET_TIME.isHighRisk(), "SET_TIME 不应为高风险");
    }

    @Test
    void getHighRiskCommandsShouldReturnAll() {
        Set<BInterfaceCommand2024> highRisk = BInterfaceCommand2024.getHighRiskCommands();
        assertTrue(highRisk.contains(BInterfaceCommand2024.SET_RMCTRLCMD));
        assertTrue(highRisk.contains(BInterfaceCommand2024.SET_SUREBOOT));
        assertTrue(highRisk.contains(BInterfaceCommand2024.SET_SUFTP));
    }

    // ==================== ACK 判定 ====================

    @Test
    void loginAckShouldBeAck() { assertTrue(BInterfaceCommand2024.LOGIN_ACK.isAck()); }
    @Test
    void getDataAckShouldBeAck() { assertTrue(BInterfaceCommand2024.GET_DATA_ACK.isAck()); }
    @Test
    void loginShouldNotBeAck() { assertFalse(BInterfaceCommand2024.LOGIN.isAck()); }
    @Test
    void getDataShouldNotBeAck() { assertFalse(BInterfaceCommand2024.GET_DATA.isAck()); }

    // ==================== 类别验证 ====================

    @Test
    void getDataShouldBeRealtime() {
        assertEquals(BInterfaceCommand2024.Category.REALTIME_DATA, BInterfaceCommand2024.GET_DATA.getCategory());
    }
    @Test
    void sendAlarmShouldBeAlarm() {
        assertEquals(BInterfaceCommand2024.Category.ALARM, BInterfaceCommand2024.SEND_ALARM.getCategory());
    }
    @Test
    void setRmctrlcmdShouldBeControl() {
        assertEquals(BInterfaceCommand2024.Category.CONTROL, BInterfaceCommand2024.SET_RMCTRLCMD.getCategory());
    }
    @Test
    void getSuftpShouldBeFtp() {
        assertEquals(BInterfaceCommand2024.Category.FTP, BInterfaceCommand2024.GET_SUFTP.getCategory());
    }

    // ==================== Name 不能重复 ====================

    @Test
    void namesShouldBeUnique() {
        long distinct = java.util.Arrays.stream(BInterfaceCommand2024.values())
                .map(BInterfaceCommand2024::getName).distinct().count();
        assertEquals(BInterfaceCommand2024.commandCount(), distinct, "所有命令 Name 必须唯一");
    }

    // ==================== Code 不能重复 ====================

    @Test
    void codesShouldBeUnique() {
        long distinct = java.util.Arrays.stream(BInterfaceCommand2024.values())
                .mapToInt(BInterfaceCommand2024::getCode).distinct().count();
        assertEquals(BInterfaceCommand2024.commandCount(), distinct, "所有命令 Code 必须唯一");
    }
}
