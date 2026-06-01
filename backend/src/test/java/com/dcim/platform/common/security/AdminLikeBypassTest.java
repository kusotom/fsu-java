package com.dcim.platform.common.security;
import org.junit.jupiter.api.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** BE-AUTH-P0-FIX-002: adminLike sensitive permission bypass tests */
class AdminLikeBypassTest {
    @AfterEach void tearDown() { RequestContext.clear(); }

    @Test void superAdminIsAdminLike() {
        RequestContext ctx = new RequestContext(1L,"super",null,List.of("super_admin"),List.of(),Set.of(),Set.of());
        assertTrue(ctx.isAdminLike()); assertTrue(ctx.isSuperAdmin());
    }
    @Test void platformAdminNotSuperAdmin() {
        RequestContext ctx = new RequestContext(2L,"platform",null,List.of("platform_admin"),List.of(),Set.of(),Set.of());
        assertTrue(ctx.isAdminLike()); assertFalse(ctx.isSuperAdmin());
    }
    @Test void adminNotSuperAdmin() {
        RequestContext ctx = new RequestContext(3L,"admin",null,List.of("admin"),List.of(),Set.of(),Set.of());
        assertTrue(ctx.isAdminLike()); assertFalse(ctx.isSuperAdmin());
    }
    @Test void viewerNotAdminLike() {
        RequestContext ctx = new RequestContext(4L,"viewer",null,List.of("viewer"),List.of(),Set.of(),Set.of());
        assertFalse(ctx.isAdminLike()); assertFalse(ctx.isSuperAdmin());
    }
    @Test void platformAdminSensitivePermDenied() {
        RequestContext ctx = new RequestContext(2L,"platform",null,List.of("platform_admin"),List.of(),Set.of(),Set.of());
        assertFalse(ctx.isSuperAdmin());
        assertTrue(RequestContext.containsSensitivePermission(new String[]{"protocol:raw:view"}));
    }
    @Test void adminRawXmlSensitiveDenied() {
        RequestContext ctx = new RequestContext(3L,"admin",null,List.of("admin"),List.of("dashboard:view"),Set.of(),Set.of());
        assertTrue(RequestContext.containsSensitivePermission(new String[]{"protocol:raw:download"}));
        assertFalse(ctx.hasPermission("protocol:raw:download"));
    }
    @Test void adminNonSensitiveCanBypass() {
        RequestContext ctx = new RequestContext(3L,"admin",null,List.of("admin"),List.of(),Set.of(),Set.of());
        assertFalse(RequestContext.containsSensitivePermission(new String[]{"dashboard:view"}));
    }
    @Test void allSensitivePermissionsListed() {
        assertTrue(RequestContext.containsSensitivePermission(new String[]{"protocol:raw:view"}));
        assertTrue(RequestContext.containsSensitivePermission(new String[]{"protocol:raw:download"}));
        assertTrue(RequestContext.containsSensitivePermission(new String[]{"protocol:runonce:readonly"}));
        assertTrue(RequestContext.containsSensitivePermission(new String[]{"protocol:set:point"}));
        assertTrue(RequestContext.containsSensitivePermission(new String[]{"protocol:set:threshold"}));
        assertTrue(RequestContext.containsSensitivePermission(new String[]{"protocol:set:ftp"}));
        assertTrue(RequestContext.containsSensitivePermission(new String[]{"protocol:set:logininfo"}));
        assertTrue(RequestContext.containsSensitivePermission(new String[]{"protocol:set:fsureboot"}));
    }
    @Test void adminLikeNullScope() {
        RequestContext ctx = new RequestContext(1L,"admin",null,List.of("admin"),List.of(),Set.of(),Set.of());
        RequestContext.setCurrent(ctx);
        try { assertNull(new DataScopeService().getAllowedFsuCodes()); }
        finally { RequestContext.clear(); }
    }
    @Test void nonAdminEmptyScopeDefaultDeny() {
        RequestContext ctx = new RequestContext(4L,"viewer",null,List.of("viewer"),List.of(),Set.of(),Set.of());
        RequestContext.setCurrent(ctx);
        try {
            Set<String> allowed = new DataScopeService().getAllowedFsuCodes();
            assertNotNull(allowed); assertTrue(allowed.isEmpty());
        } finally { RequestContext.clear(); }
    }
}
