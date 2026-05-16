package com.dcim.platform.binterface;

import com.dcim.platform.module.binterface.model.BInterfacePkType;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * BInterfacePkType 命令码枚举验证测试。
 *
 * 验证枚举值完整性，确保包含协议定义的所有标准命令码，
 * 且新增命令码不会导致重复或遗漏。
 */
class BInterfacePkTypeTest {

    private static final Set<String> EXPECTED_CODES = new HashSet<>(Arrays.asList(
            "LOGIN", "HEARTBEAT", "GET_DATA", "SEND_DATA", "SEND_ALARM",
            "GET_HISTORY_DATA", "SET_POINT", "GET_THRESHOLD", "SET_THRESHOLD",
            "TIME_CHECK", "GET_FTP", "SET_FTP",
            "GET_LOGININFO", "GET_FSUINFO", "SET_DATA", "SET_FSUREBOOT",
            "GET_SUINFO", "GET_SUFTP", "SET_TIME", "SET_TIME_ACK", "GET_SPCONFIGOPTION", "GET_SPCONFIGOPTION_ACK", "GET_ACTIVEALARM", "SUREADY", "SUREADY_ACK", "UNKNOWN"
    ));

    @Test
    void shouldContainAllExpectedCommandCodes() {
        Set<String> actual = new HashSet<>();
        for (BInterfacePkType type : BInterfacePkType.values()) {
            actual.add(type.name());
        }
        assertEquals(EXPECTED_CODES, actual, "枚举值应与预期命令码完全一致");
    }

    @Test
    void shouldHaveCorrectCount() {
        assertEquals(26, BInterfacePkType.values().length, "BInterfacePkType 应有 26 个枚举值");
    }

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
        assertNotEquals(BInterfacePkType.SEND_DATA, BInterfacePkType.GET_DATA,
                "SEND_DATA(FSU→SC主动上报) 应与 GET_DATA(SC→FSU轮询) 不同");
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
