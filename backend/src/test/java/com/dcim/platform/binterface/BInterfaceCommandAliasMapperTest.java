package com.dcim.platform.binterface;

import com.dcim.platform.module.binterface.compat.BInterfaceCommandAliasMapper;
import com.dcim.platform.module.binterface.model.BInterfaceCommand2024;
import com.dcim.platform.module.binterface.model.BInterfacePkType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 命令别名映射器测试。
 *
 * <p>BIF-P4-010: 验证旧命名 → 2024 标准命令的映射正确性。</p>
 */
class BInterfaceCommandAliasMapperTest {

    // ==================== 同名命令直接映射 ====================

    @Test
    void loginShouldMapTo2024Login() {
        var result = BInterfaceCommandAliasMapper.to2024(BInterfacePkType.LOGIN);
        assertTrue(result.isPresent());
        assertEquals(BInterfaceCommand2024.LOGIN, result.get());
    }

    @Test
    void getDataShouldMapTo2024GetData() {
        var result = BInterfaceCommandAliasMapper.to2024(BInterfacePkType.GET_DATA);
        assertTrue(result.isPresent());
        assertEquals(BInterfaceCommand2024.GET_DATA, result.get());
    }

    @Test
    void sendAlarmShouldMapTo2024SendAlarm() {
        var result = BInterfaceCommandAliasMapper.to2024(BInterfacePkType.SEND_ALARM);
        assertTrue(result.isPresent());
        assertEquals(BInterfaceCommand2024.SEND_ALARM, result.get());
    }

    // ==================== 重命名命令映射 ====================

    @Test
    void heartbeatShouldMapToGetSuinfo() {
        var result = BInterfaceCommandAliasMapper.to2024(BInterfacePkType.HEARTBEAT);
        assertTrue(result.isPresent());
        assertEquals(BInterfaceCommand2024.GET_SUINFO, result.get());
        assertEquals(1001, result.get().getCode());
    }

    @Test
    void timeCheckShouldMapToSetTime() {
        var result = BInterfaceCommandAliasMapper.to2024("TIME_CHECK");
        assertTrue(result.isPresent());
        assertEquals(BInterfaceCommand2024.SET_TIME, result.get());
        assertEquals(901, result.get().getCode());
    }

    @Test
    void getFtpShouldMapToGetSuftp() {
        var result = BInterfaceCommandAliasMapper.to2024("GET_FTP");
        assertTrue(result.isPresent());
        assertEquals(BInterfaceCommand2024.GET_SUFTP, result.get());
        assertEquals(801, result.get().getCode());
    }

    @Test
    void setFtpShouldMapToSetSuftp() {
        var result = BInterfaceCommandAliasMapper.to2024("SET_FTP");
        assertTrue(result.isPresent());
        assertEquals(BInterfaceCommand2024.SET_SUFTP, result.get());
        assertTrue(result.get().isHighRisk());
    }

    @Test
    void setPointShouldMapToSetRmctrlcmd() {
        var result = BInterfaceCommandAliasMapper.to2024("SET_POINT");
        assertTrue(result.isPresent());
        assertEquals(BInterfaceCommand2024.SET_RMCTRLCMD, result.get());
        assertEquals(701, result.get().getCode());
    }

    @Test
    void getFsuinfoShouldMapToGetSuinfo() {
        var result = BInterfaceCommandAliasMapper.to2024("GET_FSUINFO");
        assertTrue(result.isPresent());
        assertEquals(BInterfaceCommand2024.GET_SUINFO, result.get());
    }

    @Test
    void setFsuRebootShouldMapToSetSureboot() {
        var result = BInterfaceCommandAliasMapper.to2024("SET_FSUREBOOT");
        assertTrue(result.isPresent());
        assertEquals(BInterfaceCommand2024.SET_SUREBOOT, result.get());
        assertEquals(1101, result.get().getCode());
        assertTrue(result.get().isHighRisk());
    }

    // ==================== 兼容命令 ====================

    @Test
    void getThresholdShouldBeCompat() {
        assertTrue(BInterfaceCommandAliasMapper.isCompatCommand("GET_THRESHOLD"),
                "GET_THRESHOLD 应为兼容命令 (2024无对应)");
    }

    @Test
    void setThresholdShouldBeCompat() {
        assertTrue(BInterfaceCommandAliasMapper.isCompatCommand("SET_THRESHOLD"),
                "SET_THRESHOLD 应为兼容命令 (2024无对应)");
    }

    @Test
    void getLogininfoShouldBeCompat() {
        assertTrue(BInterfaceCommandAliasMapper.isCompatCommand("GET_LOGININFO"),
                "GET_LOGININFO 应为兼容命令");
    }

    @Test
    void getDataShouldNotBeCompat() {
        assertFalse(BInterfaceCommandAliasMapper.isCompatCommand("GET_DATA"),
                "GET_DATA 在 2024 标准中，不应为 compat");
    }

    // ==================== null/边界 ====================

    @Test
    void nullPkTypeShouldReturnEmpty() {
        assertTrue(BInterfaceCommandAliasMapper.to2024((BInterfacePkType) null).isEmpty());
    }

    @Test
    void nullNameShouldReturnEmpty() {
        assertTrue(BInterfaceCommandAliasMapper.to2024((String) null).isEmpty());
    }

    @Test
    void nullShouldNotBeCompat() {
        assertFalse(BInterfaceCommandAliasMapper.isCompatCommand((String) null));
    }
}
