package com.dcim.platform.binterface;

import com.dcim.platform.module.binterface.model.BInterfacePkType;
import com.dcim.platform.module.binterface.model.PkTypeDescriptor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PK_Type 双格式解析描述符测试 (BIF-P4-011)。
 */
class PkTypeDescriptorTest {

    // ==================== LEGACY_TEXT 格式 ====================

    @Test
    void shouldParseLegacyGetData() {
        PkTypeDescriptor d = PkTypeDescriptor.fromLegacyText("GET_DATA");
        assertEquals(PkTypeDescriptor.Format.LEGACY_TEXT, d.getFormat());
        assertEquals("GET_DATA", d.getOriginalText());
        assertEquals(BInterfacePkType.GET_DATA, d.getLegacyPkType());
        assertTrue(d.isValid());
    }

    @Test
    void shouldParseLegacyLogin() {
        PkTypeDescriptor d = PkTypeDescriptor.fromLegacyText("LOGIN");
        assertEquals(BInterfacePkType.LOGIN, d.getLegacyPkType());
        assertTrue(d.isNameCodeConsistent());
    }

    @Test
    void shouldMapLegacyToUnknownForBadName() {
        PkTypeDescriptor d = PkTypeDescriptor.fromLegacyText("SOME_FAKE_CMD");
        assertEquals(BInterfacePkType.UNKNOWN, d.getLegacyPkType());
    }

    @Test
    void shouldMarkLegacyEmptyAsInvalid() {
        PkTypeDescriptor d = PkTypeDescriptor.fromLegacyText("");
        assertEquals(PkTypeDescriptor.Format.INVALID, d.getFormat());
        assertFalse(d.isValid());
    }

    // ==================== 别名映射 (LEGACY) ====================

    @Test
    void timeCheckShouldMapToSetTime() {
        PkTypeDescriptor d = PkTypeDescriptor.fromLegacyText("TIME_CHECK");
        assertTrue(d.getNormalized2024().isPresent());
        assertEquals("SET_TIME", d.getNormalized2024().get().getName());
        assertEquals(901, d.getNormalized2024().get().getCode());
        assertTrue(d.isAliasApplied());
    }

    @Test
    void getFtpShouldMapToGetSuftp() {
        PkTypeDescriptor d = PkTypeDescriptor.fromLegacyText("GET_FTP");
        assertTrue(d.getNormalized2024().isPresent());
        assertEquals("GET_SUFTP", d.getNormalized2024().get().getName());
        assertEquals(801, d.getNormalized2024().get().getCode());
    }

    @Test
    void heartbeatShouldMapToGetSuinfo() {
        PkTypeDescriptor d = PkTypeDescriptor.fromLegacyText("HEARTBEAT");
        assertTrue(d.getNormalized2024().isPresent());
        assertEquals("GET_SUINFO", d.getNormalized2024().get().getName());
        assertEquals(1001, d.getNormalized2024().get().getCode());
    }

    @Test
    void setFsuRebootShouldMapToSetSureboot() {
        PkTypeDescriptor d = PkTypeDescriptor.fromLegacyText("SET_FSUREBOOT");
        assertTrue(d.getNormalized2024().isPresent());
        assertEquals("SET_SUREBOOT", d.getNormalized2024().get().getName());
        assertEquals(1101, d.getNormalized2024().get().getCode());
    }

    // ==================== 2024 NAME_CODE 格式 ====================

    @Test
    void shouldParseNameCodeGetData() {
        PkTypeDescriptor d = PkTypeDescriptor.fromNameCode("GET_DATA", 501);
        assertEquals(PkTypeDescriptor.Format.NAME_CODE, d.getFormat());
        assertTrue(d.isNameCodeConsistent());
        assertEquals(BInterfacePkType.GET_DATA, d.getLegacyPkType());
        assertEquals("GET_DATA", d.getOriginalName());
        assertEquals(501, d.getOriginalCode());
        assertEquals(501, d.getEffectiveCommandCode());
    }

    @Test
    void shouldDetectNameCodeMismatch() {
        PkTypeDescriptor d = PkTypeDescriptor.fromNameCode("GET_DATA", 999);
        assertFalse(d.isNameCodeConsistent());
        assertNotNull(d.getValidationMessage());
        assertTrue(d.getValidationMessage().contains("不一致"));
        assertFalse(d.getWarnings().isEmpty());
        // Name GET_DATA 已知但 Code 999 未知 → 不一致标记, 归一化回退到 Name
        assertTrue(d.getNormalized2024().isPresent());
        assertEquals("GET_DATA", d.getNormalized2024().get().getName());
    }

    @Test
    void shouldAcceptNameCodeLogin() {
        PkTypeDescriptor d = PkTypeDescriptor.fromNameCode("LOGIN", 101);
        assertTrue(d.isNameCodeConsistent());
        assertEquals(BInterfacePkType.LOGIN, d.getLegacyPkType());
    }

    // ==================== NAME_ONLY ====================

    @Test
    void shouldParseNameOnly() {
        PkTypeDescriptor d = PkTypeDescriptor.fromNameOnly("GET_DATA");
        assertEquals(PkTypeDescriptor.Format.NAME_ONLY, d.getFormat());
        assertEquals(BInterfacePkType.GET_DATA, d.getLegacyPkType());
    }

    @Test
    void unknownNameShouldHaveWarning() {
        PkTypeDescriptor d = PkTypeDescriptor.fromNameOnly("BOGUS_CMD");
        assertEquals(BInterfacePkType.UNKNOWN, d.getLegacyPkType());
        assertFalse(d.getWarnings().isEmpty());
    }

    // ==================== CODE_ONLY ====================

    @Test
    void shouldParseCodeOnly501() {
        PkTypeDescriptor d = PkTypeDescriptor.fromCodeOnly(501);
        assertEquals(PkTypeDescriptor.Format.CODE_ONLY, d.getFormat());
        assertEquals(501, d.getOriginalCode());
        assertEquals(BInterfacePkType.GET_DATA, d.getLegacyPkType());
        assertEquals("GET_DATA", d.getEffectiveCommandName());
        assertEquals(501, d.getEffectiveCommandCode());
    }

    @Test
    void codeOnly1001ShouldBeGetSuinfo() {
        PkTypeDescriptor d = PkTypeDescriptor.fromCodeOnly(1001);
        assertEquals("GET_SUINFO", d.getEffectiveCommandName());
    }

    @Test
    void unknownCodeShouldHaveWarning() {
        PkTypeDescriptor d = PkTypeDescriptor.fromCodeOnly(9999);
        assertFalse(d.getWarnings().isEmpty());
        assertTrue(d.getNormalized2024().isEmpty());
    }

    // ==================== 兼容命令 ====================

    @Test
    void getThresholdShouldBeCompat() {
        PkTypeDescriptor d = PkTypeDescriptor.fromLegacyText("GET_THRESHOLD");
        assertTrue(d.isCompatCommand());
        assertTrue(d.getNormalized2024().isEmpty()); // 2024 无对应
    }

    @Test
    void getDataShouldNotBeCompat() {
        PkTypeDescriptor d = PkTypeDescriptor.fromLegacyText("GET_DATA");
        assertFalse(d.isCompatCommand());
    }

    // ==================== null/边界 ====================

    @Test
    void nullTextShouldBeInvalid() {
        PkTypeDescriptor d = PkTypeDescriptor.fromLegacyText(null);
        assertEquals(PkTypeDescriptor.Format.INVALID, d.getFormat());
    }

    @Test
    void nullNameAndCodeShouldBeInvalid() {
        PkTypeDescriptor d = PkTypeDescriptor.fromNameCode(null, null);
        assertEquals(PkTypeDescriptor.Format.INVALID, d.getFormat());
    }
}
