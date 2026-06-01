package com.dcim.platform.common.security;
import org.junit.jupiter.api.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** BE-AUTH-P0-FIX-002: getById scope check — empty scope default-deny */
class IdBasedScopeAccessTest {
    private DataScopeService dss;
    @BeforeEach void setUp() { dss = new DataScopeService(); RequestContext.clear(); }
    @AfterEach void tearDown() { RequestContext.clear(); }

    private boolean canAccessById(String fsuCode, Long fsuId) {
        RequestContext ctx = RequestContext.getCurrent();
        if (ctx == null) return false;
        if (ctx.isAdminLike()) return true;
        Set<String> a = dss.getAllowedFsuCodes();
        if (a != null && a.isEmpty()) return false;
        if (a == null) return true;
        if (fsuCode != null && a.contains(fsuCode)) return true;
        if (fsuId != null && a.contains(String.valueOf(fsuId))) return true;
        return false;
    }

    @Test void adminAccessesAny() {
        RequestContext.setCurrent(new RequestContext(1L,"admin",null,List.of("admin"),List.of(),Set.of(),Set.of()));
        assertTrue(canAccessById("ANY",999L));
    }
    @Test void superAdminAccessesAny() {
        RequestContext.setCurrent(new RequestContext(2L,"super",null,List.of("super_admin"),List.of(),Set.of(),Set.of()));
        assertTrue(canAccessById(null,null));
    }
    @Test void emptyScopeDenies() {
        RequestContext.setCurrent(new RequestContext(3L,"v",null,List.of("viewer"),List.of("realtime:view"),Set.of(),Set.of()));
        assertFalse(canAccessById("FSU-001",1L));
    }
    @Test void matchingScopeAllows() {
        RequestContext.setCurrent(new RequestContext(4L,"ops",null,List.of("op"),List.of("fsu:view"),Set.of(),Set.of("FSU-001","FSU-002")));
        assertTrue(canAccessById("FSU-001",1L));
    }
    @Test void nonMatchingDenies() {
        RequestContext.setCurrent(new RequestContext(5L,"ops",null,List.of("op"),List.of("fsu:view"),Set.of(),Set.of("FSU-001")));
        assertFalse(canAccessById("FSU-999",999L));
    }
    @Test void fsuIdMatchesAsString() {
        RequestContext.setCurrent(new RequestContext(6L,"ops",null,List.of("op"),List.of("fsu:view"),Set.of(),Set.of("1","2")));
        assertTrue(canAccessById(null,1L));
    }
    @Test void nullContextDenies() {
        RequestContext.clear();
        assertFalse(canAccessById("FSU-001",1L));
    }
    @Test void platformAdminBypasses() {
        RequestContext.setCurrent(new RequestContext(7L,"platform",null,List.of("platform_admin"),List.of(),Set.of(),Set.of()));
        assertTrue(canAccessById("ANY",999L));
    }
    @Test void filterAndGetByIdConsistent() {
        RequestContext.setCurrent(new RequestContext(8L,"ops",null,List.of("op"),List.of("fsu:view"),Set.of(),Set.of("A","B")));
        List<String> list = dss.filterByFsuScope(List.of("A","B","C"),s->s);
        assertEquals(2, list.size());
        assertTrue(canAccessById("A",1L));
        assertFalse(canAccessById("C",3L));
    }
}
