package com.dcim.platform.binterface.service;

import com.dcim.platform.module.binterface.service.SetThresholdSafetyDecision;
import com.dcim.platform.module.binterface.service.SetThresholdSafetyGate;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SetThresholdSafetyGate 单元测试。
 */
class SetThresholdSafetyGateTest {

    // ==================== enabled=false（默认） ====================

    @Test
    void shouldDenyWhenDisabledEvenWithConfirmation() {
        SetThresholdSafetyGate gate = new SetThresholdSafetyGate(false, true);

        SetThresholdSafetyDecision decision = gate.check("FSU-001", true);

        assertFalse(decision.isAllowed());
        assertEquals("3001", decision.getResultCode());
    }

    @Test
    void shouldDenyWhenDisabledWithoutConfirmation() {
        SetThresholdSafetyGate gate = new SetThresholdSafetyGate(false, true);

        SetThresholdSafetyDecision decision = gate.check("FSU-001", false);

        assertFalse(decision.isAllowed());
        assertEquals("3001", decision.getResultCode());
    }

    // ==================== enabled=true ====================

    @Test
    void shouldAllowWhenEnabledAndConfirmed() {
        SetThresholdSafetyGate gate = new SetThresholdSafetyGate(true, true);

        SetThresholdSafetyDecision decision = gate.check("FSU-001", true);

        assertTrue(decision.isAllowed());
        assertEquals("0", decision.getResultCode());
    }

    @Test
    void shouldDenyWhenEnabledButNotConfirmed() {
        SetThresholdSafetyGate gate = new SetThresholdSafetyGate(true, true);

        SetThresholdSafetyDecision decision = gate.check("FSU-001", false);

        assertFalse(decision.isAllowed());
        assertEquals("3002", decision.getResultCode());
    }

    // ==================== requireConfirmation=false ====================

    @Test
    void shouldAllowWhenConfirmationNotRequired() {
        SetThresholdSafetyGate gate = new SetThresholdSafetyGate(true, false);

        SetThresholdSafetyDecision decision = gate.check("FSU-001", false);

        assertTrue(decision.isAllowed());
        assertEquals("0", decision.getResultCode());
    }

    @Test
    void shouldAllowWhenConfirmationNotRequiredAndConfirmed() {
        SetThresholdSafetyGate gate = new SetThresholdSafetyGate(true, false);

        SetThresholdSafetyDecision decision = gate.check("FSU-001", true);

        assertTrue(decision.isAllowed());
        assertEquals("0", decision.getResultCode());
    }
}
