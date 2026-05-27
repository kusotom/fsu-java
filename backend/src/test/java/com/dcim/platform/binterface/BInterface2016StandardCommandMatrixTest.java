package com.dcim.platform.binterface;

import com.dcim.platform.module.binterface.command.CommandResult;
import com.dcim.platform.module.binterface.compat.BInterfaceCommand2016;
import com.dcim.platform.module.binterface.model.BInterfaceCommand2024;
import com.dcim.platform.module.binterface.model.BInterfacePkType;
import com.dcim.platform.module.binterface.model.PkTypeDescriptor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SPEC-TEST-P0-001: B接口2016 标准命令码矩阵测试。
 *
 * 依据:
 * - SPEC-2016-PKTYPE-001
 * - SPEC-2016-RESULT-001
 * - SPEC-2016-RESULT-002
 * - openspec/protocols/binterface-2016/matrices/command-code-matrix.md
 */
class BInterface2016StandardCommandMatrixTest {

    @Test
    void primaryCommandCodesShouldMatchSpecP0001Matrix() {
        assertAll(BInterface2016StandardTestSupport.PRIMARY_COMMANDS.stream()
                .flatMap(spec -> java.util.stream.Stream.of(
                        () -> assertEquals(spec.code(), codeFor(spec.name()),
                                spec.name() + " request Code must match B接口2016 matrix"),
                        () -> assertEquals(spec.ackCode(), codeFor(spec.ackName()),
                                spec.ackName() + " ACK Code must match B接口2016 matrix"))));
    }

    @Test
    void setCommandCodesShouldMatchSpecP0001MatrixEvenWhenSafetyDisabled() {
        assertAll(BInterface2016StandardTestSupport.SET_COMMANDS.stream()
                .flatMap(spec -> java.util.stream.Stream.of(
                        () -> assertEquals(spec.code(), codeFor(spec.name()),
                                spec.name() + " request Code must match B接口2016 matrix"),
                        () -> assertEquals(spec.ackCode(), codeFor(spec.ackName()),
                                spec.ackName() + " ACK Code must match B接口2016 matrix"))));
    }

    @Test
    void heartbeatShouldNotHaveStandalone2016Code() {
        assertTrue(BInterfaceCommand2016.codeFor("HEARTBEAT").isEmpty(),
                "SPEC-P0-001: 原文报文类型表不定义独立 HEARTBEAT 命令码");
        assertTrue(BInterfaceCommand2016.codeFor("HEARTBEAT_ACK").isEmpty(),
                "SPEC-P0-001: SC 心跳功能应归入 GET_FSUINFO 1701/1702");
    }

    @Test
    void standard2016CodesShouldNotUseKnown2024ConflictingCodes() {
        assertEquals(401, codeFor("GET_DATA"));
        assertEquals(501, BInterfaceCommand2024.findByName("GET_DATA").orElseThrow().getCode());
        assertNotEquals(BInterfaceCommand2024.findByName("GET_DATA").orElseThrow().getCode(),
                codeFor("GET_DATA"), "2016 GET_DATA must not use 2024 Code=501");

        assertEquals(501, codeFor("SEND_ALARM"));
        assertEquals(601, BInterfaceCommand2024.findByName("SEND_ALARM").orElseThrow().getCode());
        assertNotEquals(BInterfaceCommand2024.findByName("SEND_ALARM").orElseThrow().getCode(),
                codeFor("SEND_ALARM"), "2016 SEND_ALARM must not use 2024 Code=601");
    }

    @Test
    void parsedNameCodeShouldBeValidatedAgainst2016MatrixNot2024Matrix() {
        PkTypeDescriptor descriptor = PkTypeDescriptor.fromNameCode("GET_DATA", 401);

        assertEquals(PkTypeDescriptor.Format.NAME_CODE, descriptor.getFormat());
        assertEquals("GET_DATA", descriptor.getOriginalName());
        assertEquals(401, descriptor.getOriginalCode());
        assertTrue(descriptor.isNameCodeConsistent(),
                "2016 standard PK_Type <Name>GET_DATA</Name><Code>401</Code> must be treated as consistent");
        assertEquals(401, descriptor.getEffectiveCommandCode(),
                "2016 standard path must not normalize GET_DATA Code to 2024 Code=501");
    }

    @Test
    void commandResultFor2016StandardShouldUseResultNotResultCode() {
        CommandResult result = CommandResult.success(BInterfacePkType.GET_DATA);
        String infoXml = result.toInfoXml();

        assertTrue(infoXml.contains("<Result>1</Result>"),
                "B接口2016 standard success field is Result=1");
        assertFalse(infoXml.contains("ResultCode"),
                "ResultCode is not a standard field in the SPEC-P0-001 2016 docx baseline");
    }

    private int codeFor(String name) {
        return BInterfaceCommand2016.codeFor(name)
                .orElseThrow(() -> new AssertionError("Missing B接口2016 command code: " + name));
    }
}
