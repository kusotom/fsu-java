package com.dcim.platform.common.security;
import com.dcim.platform.module.binterface.log.BInterfaceMessageLogEntity;
import org.junit.jupiter.api.*;
import java.time.LocalDateTime;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** BE-AUTH-P0-FIX-002: raw XML permission + scope integration tests */
class RawXmlPermissionScopeTest {
    private DataScopeService dss;
    @BeforeEach void setUp() { dss = new DataScopeService(); RequestContext.clear(); }
    @AfterEach void tearDown() { RequestContext.clear(); }

    private BInterfaceMessageLogEntity entity(Long id, String fsuCode) {
        BInterfaceMessageLogEntity e = new BInterfaceMessageLogEntity();
        e.setId(id); e.setFsuCode(fsuCode); e.setDirection("OUTBOUND");
        e.setCommand("GET_DATA"); e.setCreatedAt(LocalDateTime.now());
        return e;
    }
    private boolean canAccess(BInterfaceMessageLogEntity e) {
        RequestContext ctx = RequestContext.getCurrent();
        if (ctx == null) return false;
        if (ctx.isSuperAdmin()) return true;
        if (e.getFsuCode() == null) return false;
        if (ctx.isAdminLike()) return true;
        Set<String> a = dss.getAllowedFsuCodes();
        if (a == null) return true;
        if (a.isEmpty()) return false;
        return a.contains(e.getFsuCode());
    }

    @Test void nullContextDenies() { RequestContext.clear(); assertFalse(canAccess(entity(1L,"FSU-001"))); }
    @Test void emptyScopeDenies() {
        RequestContext.setCurrent(new RequestContext(1L,"v",null,List.of("viewer"),List.of("protocol:raw:view"),Set.of(),Set.of()));
        assertFalse(canAccess(entity(2L,"FSU-001")));
    }
    @Test void matchingScopeAllows() {
        RequestContext.setCurrent(new RequestContext(2L,"ops",null,List.of("op"),List.of("protocol:raw:view"),Set.of(),Set.of("FSU-001","FSU-002")));
        assertTrue(canAccess(entity(3L,"FSU-001")));
    }
    @Test void nonMatchingScopeDenies() {
        RequestContext.setCurrent(new RequestContext(3L,"ops",null,List.of("op"),List.of("protocol:raw:view"),Set.of(),Set.of("FSU-001")));
        assertFalse(canAccess(entity(4L,"FSU-999")));
    }
    @Test void nullFsuCodeDeniesForNonSuperAdmin() {
        RequestContext.setCurrent(new RequestContext(4L,"ops",null,List.of("op"),List.of("protocol:raw:view"),Set.of(),Set.of("FSU-001")));
        assertFalse(canAccess(entity(5L,null)));
    }
    @Test void nullFsuCodeAllowsForSuperAdmin() {
        RequestContext.setCurrent(new RequestContext(5L,"super",null,List.of("super_admin"),List.of("protocol:raw:view"),Set.of(),Set.of()));
        assertTrue(canAccess(entity(6L,null)));
    }
    @Test void superAdminBypassesAll() {
        RequestContext.setCurrent(new RequestContext(6L,"super",null,List.of("super_admin"),List.of(),Set.of(),Set.of()));
        assertTrue(canAccess(entity(7L,"ANY")));
        assertTrue(canAccess(entity(8L,null)));
    }
    @Test void platformAdminWithFsuCodeAllowed() {
        RequestContext.setCurrent(new RequestContext(7L,"platform",null,List.of("platform_admin"),List.of("protocol:raw:view"),Set.of(),Set.of()));
        assertTrue(canAccess(entity(9L,"FSU-001")));
    }
    @Test void downloadRequiresBothPermissions() {
        RequestContext.setCurrent(new RequestContext(8L,"half",null,List.of("dbg"),List.of("protocol:raw:view" /* no download */),Set.of(),Set.of("FSU-001")));
        assertTrue(RequestContext.getCurrent().hasPermission("protocol:raw:view"));
        assertFalse(RequestContext.getCurrent().hasPermission("protocol:raw:download"));
    }
    @Test void bothPermissionsPresentPasses() {
        RequestContext.setCurrent(new RequestContext(9L,"full",null,List.of("dbg"),List.of("protocol:raw:view","protocol:raw:download"),Set.of(),Set.of("FSU-001")));
        assertTrue(RequestContext.getCurrent().hasPermission("protocol:raw:view"));
        assertTrue(RequestContext.getCurrent().hasPermission("protocol:raw:download"));
    }
    @Test void viewerWithoutRawViewDeniedByEmptyScope() {
        RequestContext.setCurrent(new RequestContext(10L,"v",null,List.of("viewer"),List.of("dashboard:view"),Set.of(),Set.of()));
        assertFalse(canAccess(entity(10L,"FSU-001")));
    }
}
