package com.dcim.platform.binterface;

import com.dcim.platform.module.binterface.compat.BInterfaceFieldAliasMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 字段别名映射器测试。
 *
 * <p>BIF-P4-010: 验证旧字段名 → 2024 标准字段名的映射。</p>
 */
class BInterfaceFieldAliasMapperTest {

    // ==================== FSUCode → SUID ====================

    @Test
    void fsuCodeShouldMapToSuid() {
        assertEquals("SUID", BInterfaceFieldAliasMapper.toStandard("FSUCode"));
    }

    @Test
    void fsuCodeLowerCaseShouldMapToSuid() {
        assertEquals("SUID", BInterfaceFieldAliasMapper.toStandard("FsuCode"));
    }

    @Test
    void fsuCodeSnakeCaseShouldMapToSuid() {
        assertEquals("SUID", BInterfaceFieldAliasMapper.toStandard("fsu_code"));
    }

    @Test
    void suCodeShouldMapToSuid() {
        assertEquals("SUID", BInterfaceFieldAliasMapper.toStandard("SUCode"));
    }

    @Test
    void fsuIdShouldMapToSuid() {
        assertEquals("SUID", BInterfaceFieldAliasMapper.toStandard("FSUID"));
    }

    // ==================== SignalID → SPID ====================

    @Test
    void signalIdShouldMapToSpid() {
        assertEquals("SPID", BInterfaceFieldAliasMapper.toStandard("SignalID"));
    }

    @Test
    void signalIdCamelCaseShouldMapToSpid() {
        assertEquals("SPID", BInterfaceFieldAliasMapper.toStandard("SignalId"));
    }

    @Test
    void signalIdSnakeCaseShouldMapToSpid() {
        assertEquals("SPID", BInterfaceFieldAliasMapper.toStandard("signal_id"));
    }

    @Test
    void pointIdShouldMapToSpid() {
        assertEquals("SPID", BInterfaceFieldAliasMapper.toStandard("PointID"));
    }

    @Test
    void pointCodeShouldMapToSpid() {
        assertEquals("SPID", BInterfaceFieldAliasMapper.toStandard("PointCode"));
    }

    // ==================== DeviceID (同名字段) ====================

    @Test
    void deviceIdShouldStayDeviceId() {
        assertEquals("DeviceID", BInterfaceFieldAliasMapper.toStandard("DeviceID"));
    }

    // ==================== FSU IP/Port ====================

    @Test
    void fsuPortShouldMapToSuPort() {
        assertEquals("SUPort", BInterfaceFieldAliasMapper.toStandard("FSUPort"));
    }

    @Test
    void fsuIpShouldMapToSuIp() {
        assertEquals("SUIP", BInterfaceFieldAliasMapper.toStandard("FSUIP"));
    }

    // ==================== 无映射字段原样返回 ====================

    @Test
    void unknownFieldShouldReturnAsIs() {
        assertEquals("SomeUnknownField", BInterfaceFieldAliasMapper.toStandard("SomeUnknownField"));
    }

    @Test
    void standardFieldShouldReturnAsIs() {
        assertEquals("SUID", BInterfaceFieldAliasMapper.toStandard("SUID"));
    }

    // ==================== 反向映射 ====================

    @Test
    void suidShouldMapBackToFsuCode() {
        var result = BInterfaceFieldAliasMapper.toLegacy("SUID");
        assertTrue(result.isPresent());
        assertEquals("FSUCode", result.get());
    }

    @Test
    void spidShouldMapBackToSignalId() {
        var result = BInterfaceFieldAliasMapper.toLegacy("SPID");
        assertTrue(result.isPresent());
        assertEquals("SignalID", result.get());
    }

    // ==================== isAlias 判定 ====================

    @Test
    void fsuCodeShouldBeAlias() {
        assertTrue(BInterfaceFieldAliasMapper.isAlias("FSUCode"));
    }

    @Test
    void signalIdShouldBeAlias() {
        assertTrue(BInterfaceFieldAliasMapper.isAlias("SignalID"));
    }

    @Test
    void standardSuidShouldNotBeAlias() {
        assertFalse(BInterfaceFieldAliasMapper.isAlias("SUID"));
    }

    @Test
    void unknownFieldShouldNotBeAlias() {
        assertFalse(BInterfaceFieldAliasMapper.isAlias("RandomField"));
    }

    // ==================== null ====================

    @Test
    void nullShouldReturnNull() {
        assertNull(BInterfaceFieldAliasMapper.toStandard(null));
    }

    @Test
    void nullShouldNotBeAlias() {
        assertFalse(BInterfaceFieldAliasMapper.isAlias(null));
    }
}
