package com.dcim.platform.common.security;

import com.dcim.platform.common.security.audit.AuditLogRepository;
import com.dcim.platform.common.security.audit.AuditLogService;
import com.dcim.platform.module.auth.service.TokenStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * BE-AUTH-P0-001 + BE-AUTH-P0-FIX-001: 安全基础设施测试.
 */
class SecurityInfrastructureTest {

    private TokenStore tokenStore;

    @BeforeEach
    void setUp() {
        tokenStore = new TokenStore();
    }

    // ===== TokenStore =====

    @Test
    void createAndValidateToken() {
        var pair = tokenStore.createTokens(1L, "admin", "13800000000",
                Arrays.asList("admin"), Arrays.asList("dashboard:view"),
                Collections.emptySet(), Collections.emptySet());
        assertNotNull(pair.accessToken());
        var entry = tokenStore.validateAndGet(pair.accessToken());
        assertNotNull(entry);
        assertEquals(1L, entry.userId());
        assertEquals("admin", entry.username());
        assertTrue(entry.roles().contains("admin"));
    }

    @Test
    void invalidTokenReturnsNull() {
        assertNull(tokenStore.validateAndGet("nonexistent"));
    }

    @Test
    void invalidatedTokenReturnsNull() {
        var pair = tokenStore.createTokens(1L, "admin", "13800000000",
                Arrays.asList("admin"), Arrays.asList("dashboard:view"),
                Collections.emptySet(), Collections.emptySet());
        tokenStore.invalidate(pair.accessToken());
        assertNull(tokenStore.validateAndGet(pair.accessToken()));
    }

    // ===== RequestContext =====

    @Test
    void requestContextSetAndGet() {
        RequestContext ctx = new RequestContext(1L, "admin", null,
                List.of("admin"), List.of("dashboard:view"),
                Set.of(1L), Set.of("FSU-001"));
        RequestContext.setCurrent(ctx);
        assertEquals(1L, RequestContext.getCurrent().getUserId());
        assertEquals("admin", RequestContext.getCurrent().getUsername());
        assertTrue(RequestContext.getCurrent().hasRole("admin"));
        assertTrue(RequestContext.getCurrent().hasPermission("dashboard:view"));
        assertTrue(RequestContext.getCurrent().getStationScope().contains(1L));
        assertTrue(RequestContext.getCurrent().getFsuScope().contains("FSU-001"));
        RequestContext.clear();
        assertNull(RequestContext.getCurrent());
    }

    @Test
    void requestContextIsAdminLike() {
        RequestContext adminCtx = new RequestContext(1L, "admin", null,
                List.of("admin"), List.of(), Collections.emptySet(), Collections.emptySet());
        RequestContext.setCurrent(adminCtx);
        assertTrue(adminCtx.isAdminLike());
        assertFalse(adminCtx.isSuperAdmin());
        RequestContext.clear();
    }

    @Test
    void requestContextIsSuperAdmin() {
        RequestContext superCtx = new RequestContext(2L, "super", null,
                List.of("super_admin"), List.of(), Collections.emptySet(), Collections.emptySet());
        RequestContext.setCurrent(superCtx);
        assertTrue(superCtx.isAdminLike());
        assertTrue(superCtx.isSuperAdmin());
        RequestContext.clear();
    }

    @Test
    void sensitivePermissionsDetection() {
        assertTrue(RequestContext.containsSensitivePermission(
                new String[]{"protocol:raw:view"}));
        assertTrue(RequestContext.containsSensitivePermission(
                new String[]{"protocol:set:point"}));
        assertFalse(RequestContext.containsSensitivePermission(
                new String[]{"dashboard:view"}));
        assertFalse(RequestContext.containsSensitivePermission(
                new String[]{"dashboard:view", "site:view"}));
        assertTrue(RequestContext.containsSensitivePermission(
                new String[]{"dashboard:view", "protocol:raw:view"}));
    }

    // ===== AuthInterceptor =====

    @Test
    void authInterceptorUnauthenticated() {
        AuditLogService auditLog = new AuditLogService(null);
        AuthInterceptor interceptor = new AuthInterceptor(auditLog);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/test");
        // No RequestContext set — should throw UnauthorizedException for protected endpoints
        // (handler without @RequirePermission should pass)
        boolean result = interceptor.preHandle(request, new MockHttpServletResponse(),
                new Object() /* not a HandlerMethod */);
        assertTrue(result, "Non-handler should pass");
    }

    @Test
    void authInterceptorNullAnnotationPasses() {
        AuditLogService auditLog = new AuditLogService(null);
        AuthInterceptor interceptor = new AuthInterceptor(auditLog);
        RequestContext ctx = new RequestContext(1L, "viewer", null,
                List.of("viewer"), List.of(), Collections.emptySet(), Collections.emptySet());
        RequestContext.setCurrent(ctx);
        try {
            MockHttpServletRequest request = new MockHttpServletRequest();
            boolean result = interceptor.preHandle(request, new MockHttpServletResponse(),
                    new Object() /* not a HandlerMethod */);
            assertTrue(result);
        } finally {
            RequestContext.clear();
        }
    }
}
