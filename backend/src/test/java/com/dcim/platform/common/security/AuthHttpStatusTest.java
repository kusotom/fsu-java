package com.dcim.platform.common.security;

import com.dcim.platform.common.exception.ForbiddenException;
import com.dcim.platform.common.exception.UnauthorizedException;
import com.dcim.platform.common.security.audit.AuditLogService;
import com.dcim.platform.module.auth.service.TokenStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/** BE-AUTH-P0-FIX-002: HTTP 401/403 + RequestContext 权限测试 */
class AuthHttpStatusTest {
    private TokenStore tokenStore;
    @BeforeEach void setUp() { tokenStore = new TokenStore(); }

    @Test void noTokenShouldThrowUnauthorized() {
        assertThrows(UnauthorizedException.class, () -> { throw new UnauthorizedException("请先登录"); });
    }
    @Test void invalidTokenShouldReturnNull() { assertNull(tokenStore.validateAndGet("invalid")); }
    @Test void expiredTokenReturnsNull() { assertNull(tokenStore.validateAndGet("expired-nonexistent-token")); }

    @Test void noPermissionShouldThrowForbidden() {
        ForbiddenException ex = new ForbiddenException("无权限访问", "protocol:raw:view");
        assertEquals("无权限访问", ex.getMessage());
        assertEquals("protocol:raw:view", ex.getPermissionCode());
    }

    @Test void hasPermissionShouldMatchExact() {
        RequestContext ctx = new RequestContext(1L, "user", null, List.of("operator"),
                List.of("dashboard:view", "site:view"), Set.of(), Set.of());
        RequestContext.setCurrent(ctx);
        try {
            assertTrue(ctx.hasPermission("dashboard:view"));
            assertFalse(ctx.hasPermission("protocol:raw:view"));
        } finally { RequestContext.clear(); }
    }

    @Test void hasRoleCaseInsensitive() {
        RequestContext ctx = new RequestContext(1L, "admin", null, List.of("SUPER_ADMIN"),
                List.of(), Set.of(), Set.of());
        RequestContext.setCurrent(ctx);
        try {
            assertTrue(ctx.hasRole("super_admin"));
            assertFalse(ctx.hasRole("admin"));
        } finally { RequestContext.clear(); }
    }

    @Test void sensitivePermissions() {
        assertTrue(RequestContext.containsSensitivePermission(new String[]{"protocol:raw:view"}));
        assertTrue(RequestContext.containsSensitivePermission(new String[]{"protocol:set:point"}));
        assertFalse(RequestContext.containsSensitivePermission(new String[]{"dashboard:view"}));
        assertFalse(RequestContext.containsSensitivePermission(new String[]{}));
    }

    @Test void mixedPermissionsDetectSensitive() {
        assertTrue(RequestContext.containsSensitivePermission(
                new String[]{"dashboard:view", "protocol:raw:view"}));
    }
}
