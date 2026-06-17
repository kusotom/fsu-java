package com.dcim.platform.binterface;

import com.dcim.platform.module.binterface.safety.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SET 命令审计与 confirmationToken 集成测试 (BIF-P4-SAFE-003)。
 */
class SetCommandAuditIntegrationTest {

    private SetCommandSafetyProperties props;
    private SetCommandSafetyGate gate;
    private SetCommandAuditService audit;
    private ConfirmationTokenService tokenSvc;

    @BeforeEach
    void setUp() {
        props = new SetCommandSafetyProperties();
        gate = new SetCommandSafetyGate(props, null);
        audit = new SetCommandAuditService();
        tokenSvc = new ConfirmationTokenService();
    }

    // ==================== 审计记录 ====================

    @Test
    void shouldAuditRejectedDecision() {
        SetCommandSafetyDecision d = gate.evaluate("SET_TIME", "FSU-001", null, false, 1, false);
        assertFalse(d.isAllowed());
        SetCommandAuditRecord r = audit.recordRejected(d, "FSU-001", "MANUAL", null);
        assertNotNull(r);
        assertEquals("SET_TIME", r.getCommandName());
        assertFalse(r.isSafetyAllowed());
        assertEquals("REJECTED", r.getAuditResult());
    }

    @Test
    void shouldAuditDryRunDecision() {
        props.setEnabled(true);
        putEnabled("SET_TIME");
        SetCommandSafetyDecision d = gate.evaluate("SET_TIME", "FSU-001", "confirmed", false, 1, false);
        assertTrue(d.isAllowed() && d.isDryRun());
        SetCommandAuditRecord r = audit.recordDryRun(d, "FSU-001", "MANUAL", null);
        assertNotNull(r);
        assertEquals("DRY_RUN", r.getAuditResult());
        assertTrue(r.isDryRun());
    }

    @Test
    void shouldAuditSchedulerForbidden() {
        props.setEnabled(true);
        putEnabled("SET_TIME");
        SetCommandSafetyDecision d = gate.evaluate("SET_TIME", "FSU-001", "token", true, 1, false);
        assertFalse(d.isAllowed());
        SetCommandAuditRecord r = audit.recordRejected(d, "FSU-001", "SCHEDULER", null);
        assertEquals("REJECTED", r.getAuditResult());
        assertTrue(r.isSchedulerTriggered());
    }

    @Test
    void shouldAuditBatchForbidden() {
        props.setEnabled(true);
        putEnabled("SET_TIME");
        SetCommandSafetyDecision d = gate.evaluate("SET_TIME", null, "token", false, 10, false);
        assertFalse(d.isAllowed());
        SetCommandAuditRecord r = audit.recordRejected(d, "FSU-001", "BATCH", null);
        assertEquals("REJECTED", r.getAuditResult());
    }

    @Test
    void auditRecordShouldNotContainPlaintextToken() {
        String raw = tokenSvc.generateToken("SET_TIME", "FSU-001", "admin", "test");
        var vr = tokenSvc.validate(raw, "SET_TIME", "FSU-001");
        assertTrue(vr.isValid());
        props.setEnabled(true);
        putEnabledWithReal("SET_TIME");
        SetCommandSafetyDecision d = gate.evaluate("SET_TIME", "FSU-001", "token", false, 1, true);
        SetCommandAuditRecord r = audit.recordAllowed(d, "FSU-001", "MANUAL", vr.getTokenHash());
        assertNotNull(r);
        // tokenHash is SHA-256 hex (64 chars), not the raw token
        String hash = r.getConfirmationTokenHash();
        assertNotNull(hash);
        assertNotEquals(raw, hash);
    }

    @Test
    void auditRecordShouldNotContainPassword() {
        SetCommandSafetyDecision d = gate.evaluate("SET_TIME", "FSU-001", null, false, 1, false);
        SetCommandAuditRecord r = audit.recordRejected(d, "FSU-001", "MANUAL", null);
        String s = r.toString();
        assertFalse(s.contains("password") || s.contains("Password") || s.contains("secret"));
    }

    @Test
    void auditFailureShouldNotThrow() {
        // audit with null decision should not throw
        assertDoesNotThrow(() -> audit.recordRejected(null, "FSU-001", "TEST", null));
    }

    // ==================== Token 验证 + Gate 联合 ====================

    @Test
    void gateShouldRejectWhenTokenMissing() {
        props.setEnabled(true);
        putEnabled("SET_TIME");
        SetCommandSafetyDecision d = gate.evaluate("SET_TIME", "FSU-001", null, false, 1, true);
        assertFalse(d.isAllowed());
        assertEquals("CONFIRMATION_REQUIRED", d.getReasonCode());
    }

    @Test
    void gateShouldPassDryRunWithToken() {
        props.setEnabled(true);
        putEnabled("SET_TIME");
        SetCommandSafetyDecision d = gate.evaluate("SET_TIME", "FSU-001", "confirmed", false, 1, false);
        assertTrue(d.isAllowed() && d.isDryRun());
    }

    // ==================== Token 安全 ====================

    @Test
    void tokenServiceShouldNotExposePlaintext() {
        String raw = tokenSvc.generateToken("SET_TIME", "FSU-001", "admin", "test");
        // verify the raw token is not stored
        var vr = tokenSvc.validate(raw, "SET_TIME", "FSU-001");
        assertTrue(vr.isValid());
        // the tokenHash in the result is not the raw token
        assertNotEquals(raw, vr.getTokenHash());
    }

    // ==================== 辅助 ====================

    private void putEnabled(String name) {
        SetCommandSafetyProperties.CommandSafetyConfig c = new SetCommandSafetyProperties.CommandSafetyConfig();
        c.setEnabled(true); c.setAllowRealCall(false); c.setRequireConfirmation(true); c.setAuditRequired(true);
        props.getCommands().put(name, c);
    }

    private void putEnabledWithReal(String name) {
        SetCommandSafetyProperties.CommandSafetyConfig c = new SetCommandSafetyProperties.CommandSafetyConfig();
        c.setEnabled(true); c.setAllowRealCall(true); c.setRequireConfirmation(true); c.setAuditRequired(true);
        props.getCommands().put(name, c);
    }
}
