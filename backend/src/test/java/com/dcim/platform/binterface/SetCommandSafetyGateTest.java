package com.dcim.platform.binterface;

import com.dcim.platform.module.binterface.safety.SetCommandSafetyDecision;
import com.dcim.platform.module.binterface.safety.SetCommandSafetyGate;
import com.dcim.platform.module.binterface.safety.SetCommandSafetyProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SET 类命令安全门禁测试 (BIF-P4-SAFE-001)。
 */
class SetCommandSafetyGateTest {

    private SetCommandSafetyProperties props;
    private SetCommandSafetyGate gate;

    @BeforeEach
    void setUp() {
        props = new SetCommandSafetyProperties();
        gate = new SetCommandSafetyGate(props);
    }

    // ==================== 默认全 false ====================

    @Test void globalEnabledDefaultFalse() { assertFalse(props.isEnabled()); }
    @Test void allowRealCallDefaultFalse() { assertFalse(props.isAllowRealCall()); }
    @Test void requireConfirmationDefaultTrue() { assertTrue(props.isRequireConfirmation()); }
    @Test void auditRequiredDefaultTrue() { assertTrue(props.isAuditRequired()); }
    @Test void schedulerForbiddenDefaultTrue() { assertTrue(props.isSchedulerForbidden()); }
    @Test void dryRunDefaultTrue() { assertTrue(props.isDryRunDefault()); }

    // ==================== 全局关闭时全部拒绝 ====================

    @Test void globalDisabledShouldRejectSetTime() {
        SetCommandSafetyDecision d = gate.evaluate("SET_TIME", "FSU-001", null, false, 1, false);
        assertFalse(d.isAllowed());
        assertEquals("GLOBAL_DISABLED", d.getReasonCode());
    }

    @Test void globalDisabledShouldRejectSetRmctrlcmd() {
        assertFalse(gate.evaluate("SET_RMCTRLCMD", null, null, false, 1, false).isAllowed());
    }

    @Test void globalDisabledShouldRejectSetSureboot() {
        assertFalse(gate.evaluate("SET_SUREBOOT", null, null, false, 1, false).isAllowed());
    }

    @Test void globalDisabledShouldRejectSetSuftp() {
        assertFalse(gate.evaluate("SET_SUFTP", null, null, false, 1, false).isAllowed());
    }

    @Test void globalDisabledShouldRejectSetSpconfigoption() {
        assertFalse(gate.evaluate("SET_SPCONFIGOPTION", null, null, false, 1, false).isAllowed());
    }

    @Test void globalDisabledShouldRejectSetScip() {
        assertFalse(gate.evaluate("SET_SCIP", null, null, false, 1, false).isAllowed());
    }

    @Test void globalDisabledShouldRejectSetSchemeconfig() {
        assertFalse(gate.evaluate("SET_SCHEMECONFIG", null, null, false, 1, false).isAllowed());
    }

    @Test void globalDisabledShouldRejectSetFactoryconfig() {
        assertFalse(gate.evaluate("SET_FACTORYCONFIG", null, null, false, 1, false).isAllowed());
    }

    // ==================== 旧兼容命名 ====================

    @Test void setThresholdAliasShouldBeRejected() {
        SetCommandSafetyDecision d = gate.evaluate("SET_THRESHOLD", null, null, false, 1, false);
        assertFalse(d.isAllowed());
    }

    @Test void setPointAliasShouldBeRejected() {
        assertFalse(gate.evaluate("SET_POINT", null, null, false, 1, false).isAllowed());
    }

    @Test void setFtpAliasShouldBeRejected() {
        assertFalse(gate.evaluate("SET_FTP", null, null, false, 1, false).isAllowed());
    }

    @Test void setFsuRebootAliasShouldBeRejected() {
        assertFalse(gate.evaluate("SET_FSUREBOOT", null, null, false, 1, false).isAllowed());
    }

    @Test void timeCheckAliasShouldBeRejected() {
        assertFalse(gate.evaluate("TIME_CHECK", null, null, false, 1, false).isAllowed());
    }

    // ==================== Scheduler 禁止 ====================

    @Test void schedulerShouldBeForbiddenEvenWhenEnabled() {
        props.setEnabled(true);
        props.getCommands().put("SET_TIME", enabledConfig());
        SetCommandSafetyDecision d = gate.evaluate("SET_TIME", "FSU-001", "token", true, 1, false);
        assertFalse(d.isAllowed());
        assertEquals("SCHEDULER_FORBIDDEN", d.getReasonCode());
    }

    // ==================== 批量禁止 ====================

    @Test void batchShouldBeForbidden() {
        props.setEnabled(true);
        props.getCommands().put("SET_TIME", enabledConfig());
        SetCommandSafetyDecision d = gate.evaluate("SET_TIME", null, "token", false, 5, false);
        assertFalse(d.isAllowed());
        assertEquals("BATCH_NOT_ALLOWED", d.getReasonCode());
    }

    // ==================== 确认令牌缺失 ====================

    @Test void missingTokenShouldBeRejected() {
        props.setEnabled(true);
        props.getCommands().put("SET_TIME", enabledConfig());
        SetCommandSafetyDecision d = gate.evaluate("SET_TIME", "FSU-001", null, false, 1, false);
        assertFalse(d.isAllowed());
        assertEquals("CONFIRMATION_REQUIRED", d.getReasonCode());
    }

    @Test void withTokenAndEnabledShouldAllowDryRun() {
        props.setEnabled(true);
        props.getCommands().put("SET_TIME", enabledConfig());
        SetCommandSafetyDecision d = gate.evaluate("SET_TIME", "FSU-001", "confirmed", false, 1, false);
        assertTrue(d.isAllowed());
        assertTrue(d.isDryRun());
        assertFalse(d.isRealCallAllowed());
    }

    // ==================== 真实调用禁止 ====================

    @Test void realCallShouldBeRejectedWhenNotAllowed() {
        props.setEnabled(true);
        props.getCommands().put("SET_TIME", enabledConfig());
        SetCommandSafetyDecision d = gate.evaluate("SET_TIME", "FSU-001", "confirmed", false, 1, true);
        assertFalse(d.isAllowed());
        assertEquals("REAL_CALL_NOT_ALLOWED", d.getReasonCode());
    }

    // ==================== UNKNOWN 命令 ====================

    @Test void unknownCommandShouldBeRejected() {
        SetCommandSafetyDecision d = gate.evaluate("GET_DATA", null, null, false, 1, false);
        assertFalse(d.isAllowed());
        assertEquals("UNKNOWN_SET_COMMAND", d.getReasonCode());
    }

    @Test void nullCommandShouldBeRejected() {
        assertEquals("UNKNOWN_SET_COMMAND", gate.evaluate(null, null, null, false, 1, false).getReasonCode());
    }

    // ==================== 高风险标记 ====================

    @Test void setRmctrlcmdShouldBeHighRisk() {
        SetCommandSafetyDecision d = gate.evaluate("SET_RMCTRLCMD", null, null, false, 1, false);
        assertTrue(d.isHighRisk());
    }

    @Test void setSurebootShouldBeHighRisk() {
        assertTrue(gate.evaluate("SET_SUREBOOT", null, null, false, 1, false).isHighRisk());
    }

    @Test void setTimeShouldNotBeHighRisk() {
        assertFalse(gate.evaluate("SET_TIME", null, null, false, 1, false).isHighRisk());
    }

    // ==================== reasonCode 非空 ====================

    @Test void rejectedDecisionShouldHaveReasonCode() {
        SetCommandSafetyDecision d = gate.evaluate("SET_TIME", null, null, false, 1, false);
        assertNotNull(d.getReasonCode());
        assertFalse(d.getReasonCode().isEmpty());
    }

    @Test void rejectedDecisionShouldHaveErrors() {
        SetCommandSafetyDecision d = gate.evaluate("SET_TIME", null, null, false, 1, false);
        assertFalse(d.getErrors().isEmpty());
    }

    @Test void decisionShouldNeverBeNull() {
        assertNotNull(gate.evaluate("SET_TIME", null, null, false, 100, true));
    }

    // ==================== 辅助 ====================

    private SetCommandSafetyProperties.CommandSafetyConfig enabledConfig() {
        SetCommandSafetyProperties.CommandSafetyConfig c = new SetCommandSafetyProperties.CommandSafetyConfig();
        c.setEnabled(true);
        c.setAllowRealCall(false);
        c.setRequireConfirmation(true);
        c.setAuditRequired(true);
        return c;
    }
}
