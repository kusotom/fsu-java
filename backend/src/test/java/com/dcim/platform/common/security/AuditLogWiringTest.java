package com.dcim.platform.common.security;
import com.dcim.platform.common.security.audit.AuditLogService;
import org.junit.jupiter.api.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** BE-AUTH-P0-FIX-002: audit log wiring — verify all audit methods are callable without throwing */
class AuditLogWiringTest {

    // AuditLogService with null repo: all methods should catch NPE and not throw
    private AuditLogService svc;

    @BeforeEach void setUp() { svc = new AuditLogService(null); RequestContext.clear(); }
    @AfterEach void tearDown() { RequestContext.clear(); }

    @Test void logPermissionDeniedDoesNotThrow() {
        assertDoesNotThrow(() -> svc.logPermissionDenied(1L,"admin","/api/test","protocol:raw:view","denied"));
    }
    @Test void logPermissionDeniedNullUserDoesNotThrow() {
        assertDoesNotThrow(() -> svc.logPermissionDenied(null,null,"/api/t","p","r"));
    }
    @Test void logRawXmlAccessDoesNotThrow() {
        RequestContext.setCurrent(new RequestContext(1L,"admin",null,List.of("admin"),List.of("protocol:raw:view"),Set.of(),Set.of()));
        try { assertDoesNotThrow(() -> svc.logRawXmlAccess(100L,"FSU-001")); }
        finally { RequestContext.clear(); }
    }
    @Test void logRawXmlDownloadDoesNotThrow() {
        RequestContext.setCurrent(new RequestContext(2L,"dbg",null,List.of("debugger"),List.of("protocol:raw:view","protocol:raw:download"),Set.of(),Set.of()));
        try { assertDoesNotThrow(() -> svc.logRawXmlDownload(200L,"FSU-002")); }
        finally { RequestContext.clear(); }
    }
    @Test void logRunOnceAllowedDoesNotThrow() {
        RequestContext.setCurrent(new RequestContext(3L,"ops",null,List.of("op"),List.of("protocol:runonce:readonly"),Set.of(),Set.of()));
        try { assertDoesNotThrow(() -> svc.logRunOnce("GET_FSUINFO","FSU-003",true,"OK")); }
        finally { RequestContext.clear(); }
    }
    @Test void logRunOnceDeniedDoesNotThrow() {
        RequestContext.setCurrent(new RequestContext(3L,"ops",null,List.of("op"),List.of("protocol:runonce:readonly"),Set.of(),Set.of()));
        try { assertDoesNotThrow(() -> svc.logRunOnce("GET_DATA","FSU-999",false,"不可达")); }
        finally { RequestContext.clear(); }
    }
    @Test void logSetCommandBlockedDoesNotThrow() {
        assertDoesNotThrow(() -> svc.logSetCommandBlocked("SET_POINT","FSU-001","GLOBAL_DISABLED: blocked"));
    }
    @Test void allMethodsChainableNoThrow() {
        RequestContext.setCurrent(new RequestContext(4L,"v",null,List.of("viewer"),List.of("protocol:raw:view"),Set.of(),Set.of()));
        try {
            assertDoesNotThrow(() -> svc.logRawXmlAccess(300L,"FSU-004"));
            assertDoesNotThrow(() -> svc.logPermissionDenied(4L,"v","/download","protocol:raw:download","无下载权限"));
            assertDoesNotThrow(() -> svc.logRunOnce("GET_DATA","X",true,"ok"));
            assertDoesNotThrow(() -> svc.logSetCommandBlocked("SET_THRESHOLD","Y","blocked"));
        } finally { RequestContext.clear(); }
    }
    @Test void nullContextAuditDoesNotThrow() {
        RequestContext.clear();
        assertDoesNotThrow(() -> svc.logRawXmlAccess(1L,"F"));
        assertDoesNotThrow(() -> svc.logRawXmlDownload(1L,"F"));
        assertDoesNotThrow(() -> svc.logRunOnce("C","F",true,"r"));
    }
    @Test void auditServiceMethodsExist() {
        assertNotNull(svc);
        // Verify all audit methods exist (compile-time check passes if this test compiles)
        assertDoesNotThrow(() -> svc.logPermissionDenied(1L,"u","/","p","r"));
        assertDoesNotThrow(() -> svc.logRawXmlAccess(1L,"f"));
        assertDoesNotThrow(() -> svc.logRawXmlDownload(1L,"f"));
        assertDoesNotThrow(() -> svc.logRunOnce("c","f",true,"r"));
        assertDoesNotThrow(() -> svc.logSetCommandBlocked("c","f","r"));
    }
}
