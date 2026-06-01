package com.dcim.platform.binterface;

import com.dcim.platform.common.response.ApiResponse;
import com.dcim.platform.common.security.DataScopeService;
import com.dcim.platform.common.security.RequestContext;
import com.dcim.platform.common.security.audit.AuditLogService;
import com.dcim.platform.common.security.audit.AuditLogRepository;
import com.dcim.platform.module.binterface.controller.BInterfaceMessageLogController;
import com.dcim.platform.module.binterface.log.BInterfaceMessageLogEntity;
import com.dcim.platform.module.binterface.log.BInterfaceMessageLogService;
import com.dcim.platform.module.binterface.repository.BInterfaceMessageLogRepository;
import com.dcim.platform.module.binterface.service.BInterfaceMessageLogQueryService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * BInterfaceMessageLogController 单元测试。
 *
 * 使用内存 stub 替代 JPA Repository，不依赖数据库和 Spring MVC。
 */
class BInterfaceMessageLogControllerTest {

    private BInterfaceMessageLogController controller;
    private BInterfaceMessageLogService logService;
    private StubQueryMessageLogRepository repository;
    private AuditLogService auditLogService;

    @BeforeEach
    void setUp() {
        repository = new StubQueryMessageLogRepository();
        auditLogService = new AuditLogService(null); // 审计仓库为null，测试中审计写入会静默失败
        DataScopeService dataScopeService = new DataScopeService();
        // BE-AUTH-P0-FIX-002: 设置 admin context 使 raw scope 检查通过
        RequestContext.setCurrent(new RequestContext(1L, "admin", null,
                java.util.List.of("super_admin"), java.util.List.of("protocol:raw:view", "protocol:raw:download"),
                java.util.Set.of(), java.util.Set.of()));
        BInterfaceMessageLogQueryService queryService = new BInterfaceMessageLogQueryService(repository, dataScopeService);
        logService = new BInterfaceMessageLogService(repository);
        controller = new BInterfaceMessageLogController(queryService, logService, auditLogService, dataScopeService);
    }

    @AfterEach
    void tearDown() {
        RequestContext.clear();
    }

    // ==================== 旧接口兼容 ====================

    @Test
    void shouldKeepBackwardCompatibleList() {
        logService.saveInbound("LOGIN", "FSU-A", "<msg1/>");
        logService.saveInbound("HEARTBEAT", "FSU-B", "<msg2/>");

        ApiResponse<?> response = controller.list();

        assertEquals(0, response.getCode());
        assertNotNull(response.getData());
    }

    @Test
    void shouldKeepBackwardCompatibleGetById() {
        var saved = logService.saveInbound("LOGIN", "FSU-A", "<msg1/>");

        ApiResponse<?> response = controller.getById(saved.getId());

        assertEquals(0, response.getCode());
        assertNotNull(response.getData());
    }

    // ==================== 分页查询 ====================

    @Test
    void shouldReturnPaginatedResults() {
        logService.saveInbound("LOGIN", "FSU-A", "<msg1/>");
        logService.saveInbound("HEARTBEAT", "FSU-B", "<msg2/>");

        ApiResponse<Page<BInterfaceMessageLogEntity>> response = controller.query(null, null, null, null, 0, 20);

        assertEquals(0, response.getCode());
        Page<?> page = response.getData();
        assertNotNull(page);
        assertEquals(2, page.getTotalElements());
    }

    @Test
    void shouldFilterByCommand() {
        logService.saveInbound("LOGIN", "FSU-A", "<msg1/>");
        logService.saveInbound("HEARTBEAT", "FSU-B", "<msg2/>");
        logService.saveInbound("LOGIN", "FSU-C", "<msg3/>");

        ApiResponse<Page<BInterfaceMessageLogEntity>> response = controller.query(null, "LOGIN", null, null, 0, 10);

        assertEquals(0, response.getCode());
        assertEquals(2, response.getData().getTotalElements());
    }

    @Test
    void shouldFilterByFsuCode() {
        logService.saveInbound("LOGIN", "FSU-A", "<msg1/>");
        logService.saveInbound("HEARTBEAT", "FSU-B", "<msg2/>");

        ApiResponse<Page<BInterfaceMessageLogEntity>> response = controller.query(null, null, "FSU-A", null, 0, 10);

        assertEquals(0, response.getCode());
        assertEquals(1, response.getData().getTotalElements());
    }

    @Test
    void shouldFilterByDirection() {
        logService.saveInbound("LOGIN", "FSU-A", "<msg1/>");
        logService.saveInbound("HEARTBEAT", "FSU-B", "<msg2/>");

        ApiResponse<Page<BInterfaceMessageLogEntity>> response = controller.query("INBOUND", null, null, null, 0, 10);

        assertEquals(0, response.getCode());
        assertEquals(2, response.getData().getTotalElements());
    }

    @Test
    void shouldFilterByMessageType() {
        logService.saveInbound("LOGIN", "FSU-A", "<msg1/>");
        logService.saveInbound("HEARTBEAT", "FSU-B", "<msg2/>");

        ApiResponse<Page<BInterfaceMessageLogEntity>> response = controller.query(null, null, null, "SOAP", 0, 10);

        assertEquals(0, response.getCode());
        assertEquals(2, response.getData().getTotalElements());
    }

    @Test
    void shouldDefaultPageParams() {
        for (int i = 0; i < 10; i++) {
            logService.saveInbound("LOGIN", "FSU-001", "<msg/>");
        }

        // 不传 page/size，应使用默认值
        ApiResponse<Page<BInterfaceMessageLogEntity>> response = controller.query(null, null, null, null, null, null);

        assertEquals(0, response.getCode());
        assertNotNull(response.getData());
    }

    // ==================== 清理 ====================

    @Test
    void shouldDeleteByCleanupOlderThanDays() {
        logService.saveInbound("LOGIN", "FSU-A", "<msg1/>");
        logService.saveInbound("HEARTBEAT", "FSU-B", "<msg2/>");

        ApiResponse<Long> response = controller.cleanupByDays(30);

        assertEquals(0, response.getCode());
        assertNotNull(response.getData());
    }

    @Test
    void shouldRejectCleanupInvalidDays() {
        ApiResponse<Long> response = controller.cleanupByDays(0);

        assertNotEquals(0, response.getCode());
        assertTrue(response.getMessage().contains("days"));
    }

    // ==================== Stub ====================

    static class StubQueryMessageLogRepository extends
            BInterfaceMessageLogServiceTest.StubMessageLogRepository {
        // Inherits all stub methods from the parent test
    }
}
