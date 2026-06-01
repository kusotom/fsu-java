package com.dcim.platform.common.security;
import org.junit.jupiter.api.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** BE-AUTH-P0-FIX-002: DataScopeService integration tests */
class DataScopeIntegrationTest {
    private DataScopeService dss;
    @BeforeEach void setUp() { dss = new DataScopeService(); RequestContext.clear(); }
    @AfterEach void tearDown() { RequestContext.clear(); }

    @Test void adminSeesAll() {
        RequestContext.setCurrent(new RequestContext(1L,"admin",null,List.of("admin"),List.of(),Set.of(),Set.of()));
        assertEquals(3, dss.filterByFsuScope(List.of("A","B","C"),s->s).size());
    }
    @Test void emptyScopeSeesNothing() {
        RequestContext.setCurrent(new RequestContext(2L,"v",null,List.of("viewer"),List.of(),Set.of(),Set.of()));
        assertTrue(dss.filterByFsuScope(List.of("A","B"),s->s).isEmpty());
    }
    @Test void specificScopeSeesOnlyAuthorized() {
        RequestContext.setCurrent(new RequestContext(3L,"ops",null,List.of("operator"),List.of(),Set.of(),Set.of("A","C")));
        List<String> r = dss.filterByFsuScope(List.of("A","B","C","D"),s->s);
        assertEquals(2, r.size()); assertTrue(r.contains("A")); assertFalse(r.contains("B"));
    }
    @Test void nullFsuCodeFilteredOut() {
        RequestContext.setCurrent(new RequestContext(3L,"ops",null,List.of("operator"),List.of(),Set.of(),Set.of("A")));
        List<String> r = dss.filterByFsuScope(Arrays.asList("A",null,"B"),s->s);
        assertEquals(1, r.size());
    }
    @Test void nullContextReturnsEmpty() {
        RequestContext.clear();
        assertTrue(dss.filterByFsuScope(List.of("A","B"),s->s).isEmpty());
    }
    @Test void nullContextGetAllowedReturnsEmpty() {
        RequestContext.clear();
        assertTrue(dss.getAllowedFsuCodes().isEmpty());
    }
    @Test void stationScopeAdminSeesAll() {
        RequestContext.setCurrent(new RequestContext(1L,"admin",null,List.of("admin"),List.of(),Set.of(),Set.of()));
        assertEquals(3, dss.filterByStationScope(List.of(1L,2L,3L),s->s).size());
    }
    @Test void stationScopeEmptySeesNothing() {
        RequestContext.setCurrent(new RequestContext(2L,"v",null,List.of("viewer"),List.of(),Set.of(),Set.of()));
        assertTrue(dss.filterByStationScope(List.of(1L,2L),s->s).isEmpty());
    }
    @Test void getAllowedFsuCodesAdminNull() {
        RequestContext.setCurrent(new RequestContext(1L,"admin",null,List.of("admin"),List.of(),Set.of(),Set.of()));
        assertNull(dss.getAllowedFsuCodes());
    }
    @Test void getAllowedFsuCodesNonAdminEmpty() {
        RequestContext.setCurrent(new RequestContext(4L,"v",null,List.of("viewer"),List.of(),Set.of(),Set.of()));
        assertTrue(dss.getAllowedFsuCodes().isEmpty());
    }
    @Test void getAllowedFsuCodesWithScope() {
        RequestContext.setCurrent(new RequestContext(5L,"ops",null,List.of("op"),List.of(),Set.of(),Set.of("X","Y")));
        Set<String> s = dss.getAllowedFsuCodes();
        assertEquals(2, s.size()); assertTrue(s.contains("X"));
    }
}
