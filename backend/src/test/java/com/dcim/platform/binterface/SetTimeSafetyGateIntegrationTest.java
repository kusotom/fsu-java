package com.dcim.platform.binterface;

import com.dcim.platform.module.binterface.safety.SetCommandSafetyDecision;
import com.dcim.platform.module.binterface.safety.SetCommandSafetyGate;
import com.dcim.platform.module.binterface.safety.SetCommandSafetyProperties;
import com.dcim.platform.module.binterface.service.SetTimeResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SET_TIME 安全门禁集成测试 (BIF-P4-SAFE-002)。
 */
class SetTimeSafetyGateIntegrationTest {

    private SetCommandSafetyProperties props;
    private SetCommandSafetyGate gate;

    @BeforeEach
    void setUp() {
        props = new SetCommandSafetyProperties();
        gate = new SetCommandSafetyGate(props);
    }

    // ==================== 默认拒绝 ====================

    @Test
    void defaultGlobalDisabledShouldReject() {
        SetCommandSafetyDecision d = gate.evaluate("SET_TIME", "FSU-001", null, false, 1, false);
        assertFalse(d.isAllowed());
        assertEquals("GLOBAL_DISABLED", d.getReasonCode());
    }

    // ==================== 命令未启用 ====================

    @Test
    void commandDisabledShouldReject() {
        props.setEnabled(true);
        SetCommandSafetyDecision d = gate.evaluate("SET_TIME", "FSU-001", null, false, 1, false);
        assertFalse(d.isAllowed());
        assertEquals("COMMAND_DISABLED", d.getReasonCode());
    }

    // ==================== Scheduler 禁止 ====================

    @Test
    void schedulerTriggeredShouldReject() {
        props.setEnabled(true);
        putEnabled("SET_TIME");
        SetCommandSafetyDecision d = gate.evaluate("SET_TIME", "FSU-001", "token", true, 1, false);
        assertFalse(d.isAllowed());
        assertEquals("SCHEDULER_FORBIDDEN", d.getReasonCode());
    }

    // ==================== 批量禁止 ====================

    @Test
    void batchShouldReject() {
        props.setEnabled(true);
        putEnabled("SET_TIME");
        SetCommandSafetyDecision d = gate.evaluate("SET_TIME", null, "token", false, 5, false);
        assertFalse(d.isAllowed());
        assertEquals("BATCH_NOT_ALLOWED", d.getReasonCode());
    }

    // ==================== 确认令牌缺失 ====================

    @Test
    void missingTokenShouldReject() {
        props.setEnabled(true);
        putEnabled("SET_TIME");
        SetCommandSafetyDecision d = gate.evaluate("SET_TIME", "FSU-001", null, false, 1, false);
        assertFalse(d.isAllowed());
        assertEquals("CONFIRMATION_REQUIRED", d.getReasonCode());
    }

    // ==================== 真实调用禁止 ====================

    @Test
    void realCallNotAllowedShouldReject() {
        props.setEnabled(true);
        putEnabled("SET_TIME");
        SetCommandSafetyDecision d = gate.evaluate("SET_TIME", "FSU-001", "token", false, 1, true);
        assertFalse(d.isAllowed());
        assertEquals("REAL_CALL_NOT_ALLOWED", d.getReasonCode());
    }

    // ==================== dry-run 通过 ====================

    @Test
    void withTokenShouldAllowDryRun() {
        props.setEnabled(true);
        putEnabled("SET_TIME");
        SetCommandSafetyDecision d = gate.evaluate("SET_TIME", "FSU-001", "confirmed", false, 1, false);
        assertTrue(d.isAllowed());
        assertTrue(d.isDryRun());
        assertFalse(d.isRealCallAllowed());
    }

    // ==================== TIME_CHECK alias 走门禁 ====================

    @Test
    void timeCheckAliasShouldGoThroughGate() {
        SetCommandSafetyDecision d = gate.evaluate("TIME_CHECK", "FSU-001", null, false, 1, false);
        assertFalse(d.isAllowed());
        assertNotNull(d.getReasonCode());
        // TIME_CHECK → SET_TIME 归一化
        assertEquals("SET_TIME", d.getNormalizedCommandName());
    }

    // ==================== reasonCode 非空 ====================

    @Test
    void rejectedShouldHaveReasonCode() {
        SetCommandSafetyDecision d = gate.evaluate("SET_TIME", null, null, false, 100, true);
        assertNotNull(d.getReasonCode());
        assertFalse(d.getReasonCode().isEmpty());
    }

    // ==================== 辅助 ====================

    private void putEnabled(String name) {
        SetCommandSafetyProperties.CommandSafetyConfig c = new SetCommandSafetyProperties.CommandSafetyConfig();
        c.setEnabled(true);
        c.setAllowRealCall(false);
        c.setRequireConfirmation(true);
        c.setAuditRequired(true);
        props.getCommands().put(name, c);
    }
}
