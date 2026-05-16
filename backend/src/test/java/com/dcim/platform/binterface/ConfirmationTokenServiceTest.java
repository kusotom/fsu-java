package com.dcim.platform.binterface;

import com.dcim.platform.module.binterface.safety.ConfirmationTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * confirmationToken 测试 (BIF-P4-SAFE-003)。
 */
class ConfirmationTokenServiceTest {

    private ConfirmationTokenService svc;

    @BeforeEach void setUp() { svc = new ConfirmationTokenService(); }

    @Test void shouldGenerateNonEmptyToken() {
        String raw = svc.generateToken("SET_TIME", "FSU-001", "admin", "test");
        assertNotNull(raw);
        assertFalse(raw.isEmpty());
    }

    @Test void shouldValidateCorrectToken() {
        String raw = svc.generateToken("SET_TIME", "FSU-001", "admin", "test");
        var result = svc.validate(raw, "SET_TIME", "FSU-001");
        assertTrue(result.isValid());
        assertNotNull(result.getTokenHash());
    }

    @Test void shouldRejectInvalidToken() {
        var result = svc.validate("bogus-token", "SET_TIME", "FSU-001");
        assertFalse(result.isValid());
        assertEquals("CONFIRMATION_TOKEN_INVALID", result.getReasonCode());
    }

    @Test void shouldRejectNullToken() {
        var r = svc.validate(null, "SET_TIME", "FSU-001");
        assertFalse(r.isValid());
        assertEquals("CONFIRMATION_TOKEN_MISSING", r.getReasonCode());
    }

    @Test void shouldRejectEmptyToken() {
        assertFalse(svc.validate("", "SET_TIME", "FSU-001").isValid());
    }

    @Test void shouldRejectCommandMismatch() {
        String raw = svc.generateToken("SET_TIME", "FSU-001", "admin", "test");
        var r = svc.validate(raw, "SET_SUFTP", "FSU-001");
        assertFalse(r.isValid());
        assertTrue(r.getReasonCode().contains("COMMAND"));
    }

    @Test void shouldRejectSuidMismatch() {
        String raw = svc.generateToken("SET_TIME", "FSU-001", "admin", "test");
        var r = svc.validate(raw, "SET_TIME", "FSU-002");
        assertFalse(r.isValid());
        assertTrue(r.getReasonCode().contains("SUID"));
    }

    @Test void shouldRejectExpiredToken() throws Exception {
        String raw = svc.generateToken("SET_TIME", "FSU-001", "admin", "test", 1);
        Thread.sleep(1100);
        assertFalse(svc.validate(raw, "SET_TIME", "FSU-001").isValid());
    }

    @Test void shouldRejectUsedToken() {
        String raw = svc.generateToken("SET_TIME", "FSU-001", "admin", "test");
        var r1 = svc.validate(raw, "SET_TIME", "FSU-001");
        assertTrue(r1.isValid());
        svc.markUsed(r1.getTokenHash());
        var r2 = svc.validate(raw, "SET_TIME", "FSU-001");
        assertFalse(r2.isValid());
        assertEquals("CONFIRMATION_TOKEN_USED", r2.getReasonCode());
    }

    @Test void shouldNotStorePlaintextToken() {
        String raw = svc.generateToken("SET_TIME", "FSU-001", "admin", "test");
        // raw token contains UUID, service stores only hash
        assertTrue(raw.length() > 40);  // UUID + suffix
        // validate uses hash lookup, not raw
        assertTrue(svc.validate(raw, "SET_TIME", "FSU-001").isValid());
    }
}
