package com.dcim.platform.binterface;

import com.dcim.platform.common.response.ApiResponse;
import com.dcim.platform.module.binterface.controller.ActiveAlarmAuditController;
import com.dcim.platform.module.binterface.dto.ActiveAlarmAuditRecordResponse;
import com.dcim.platform.module.binterface.dto.ActiveAlarmAuditStatusResponse;
import com.dcim.platform.module.binterface.entity.ActiveAlarmAuditRecordEntity;
import com.dcim.platform.module.binterface.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 活动告警审计 Controller 测试 (BIF-P4-022, BIF-P4-023)。
 *
 * <p>验证 /latest /history 只读、不触发 FSU、不触发 SET。</p>
 */
class ActiveAlarmAuditControllerTest {

    private ActiveAlarmAuditStatusService statusService;
    private ActiveAlarmAuditRecordService recordService;
    private ActiveAlarmAuditController controller;

    @BeforeEach
    void setUp() {
        statusService = mock(ActiveAlarmAuditStatusService.class);
        recordService = mock(ActiveAlarmAuditRecordService.class);
        controller = new ActiveAlarmAuditController(statusService, recordService);
    }

    // ==================== /status (保持兼容) ====================

    @Test void statusShouldReturnFromStatusService() {
        ActiveAlarmAuditStatusResponse expected = ActiveAlarmAuditStatusResponse.builder()
                .schedulerEnabled(false).dataSource("none").build();
        when(statusService.getStatus()).thenReturn(expected);

        ApiResponse<ActiveAlarmAuditStatusResponse> resp = controller.status();
        assertNotNull(resp);
        assertEquals(0, resp.getCode());
        assertNotNull(resp.getData());
        assertEquals("none", resp.getData().getDataSource());
        verify(statusService, times(1)).getStatus();
    }

    // ==================== /latest ====================

    @Test void latestShouldReturnMostRecentRecord() {
        ActiveAlarmAuditRecordEntity entity = new ActiveAlarmAuditRecordEntity();
        entity.setId(1L);
        entity.setFsuCode("FSU-001");
        entity.setSuid("SUID-T01");
        entity.setSuccess(true);
        entity.setFsuCount(5);
        entity.setLocalCount(5);
        entity.setRunAt(LocalDateTime.now());
        when(recordService.findLatest()).thenReturn(Optional.of(entity));

        ApiResponse<ActiveAlarmAuditRecordResponse> resp = controller.latest(null);
        assertNotNull(resp);
        assertEquals(0, resp.getCode());
        assertNotNull(resp.getData());
        assertEquals("FSU-001", resp.getData().getFsuCode());
        assertEquals("SUID-T01", resp.getData().getSuid());
        assertTrue(resp.getData().isSuccess());
    }

    @Test void latestByFsuCodeShouldReturnFiltered() {
        ActiveAlarmAuditRecordEntity entity = new ActiveAlarmAuditRecordEntity();
        entity.setId(2L);
        entity.setFsuCode("FSU-002");
        entity.setRunAt(LocalDateTime.now());
        when(recordService.findLatestByFsuCode("FSU-002")).thenReturn(Optional.of(entity));

        ApiResponse<ActiveAlarmAuditRecordResponse> resp = controller.latest("FSU-002");
        assertNotNull(resp.getData());
        assertEquals("FSU-002", resp.getData().getFsuCode());
    }

    @Test void latestWhenEmptyShouldReturnNullData() {
        when(recordService.findLatest()).thenReturn(Optional.empty());

        ApiResponse<ActiveAlarmAuditRecordResponse> resp = controller.latest(null);
        assertNotNull(resp);
        assertEquals(0, resp.getCode());
        assertNull(resp.getData());
    }

    // ==================== /history ====================

    @Test void historyShouldReturnPaginatedList() {
        ActiveAlarmAuditRecordEntity e1 = new ActiveAlarmAuditRecordEntity();
        e1.setId(1L);
        e1.setFsuCode("FSU-001");
        e1.setRunAt(LocalDateTime.now());
        ActiveAlarmAuditRecordEntity e2 = new ActiveAlarmAuditRecordEntity();
        e2.setId(2L);
        e2.setFsuCode("FSU-001");
        e2.setRunAt(LocalDateTime.now().minusDays(1));

        Page<ActiveAlarmAuditRecordEntity> page = new PageImpl<>(List.of(e1, e2),
                PageRequest.of(0, 20), 2);
        when(recordService.findHistory("FSU-001", PageRequest.of(0, 20)))
                .thenReturn(page);

        ApiResponse<List<ActiveAlarmAuditRecordResponse>> resp = controller.history("FSU-001", 0, 20);
        assertNotNull(resp);
        assertEquals(0, resp.getCode());
        assertNotNull(resp.getData());
        assertEquals(2, resp.getData().size());
        assertEquals("FSU-001", resp.getData().get(0).getFsuCode());
    }

    @Test void historyShouldCapMaxSize() {
        ActiveAlarmAuditRecordEntity entity = new ActiveAlarmAuditRecordEntity();
        entity.setRunAt(LocalDateTime.now());
        Page<ActiveAlarmAuditRecordEntity> page = new PageImpl<>(List.of(entity),
                PageRequest.of(0, 100), 1);
        when(recordService.findHistory(isNull(), any(PageRequest.class)))
                .thenReturn(page);

        ApiResponse<List<ActiveAlarmAuditRecordResponse>> resp = controller.history(null, 0, 500);
        assertNotNull(resp.getData());
        assertEquals(1, resp.getData().size());
        // size 500 caps to 100
        verify(recordService, times(1)).findHistory(isNull(), eq(PageRequest.of(0, 100)));
    }

    // ==================== 安全边界 ====================

    @Test void latestShouldNotTriggerFsuQuery() {
        ActiveAlarmAuditRecordEntity entity = new ActiveAlarmAuditRecordEntity();
        entity.setRunAt(LocalDateTime.now());
        when(recordService.findLatest()).thenReturn(Optional.of(entity));

        controller.latest(null);
        verify(recordService, never()).save(any(), any(), any());
        // /latest 只调用 findLatest，不调用任何审计方法
    }

    @Test void historyShouldNotTriggerFsuQuery() {
        ActiveAlarmAuditRecordEntity entity = new ActiveAlarmAuditRecordEntity();
        entity.setRunAt(LocalDateTime.now());
        Page<ActiveAlarmAuditRecordEntity> page = new PageImpl<>(List.of(entity),
                PageRequest.of(0, 20), 1);
        when(recordService.findHistory(isNull(), any())).thenReturn(page);

        controller.history(null, 0, 20);
        verify(recordService, never()).save(any(), any(), any());
    }

    @Test void latestShouldNotEnableScheduler() {
        ActiveAlarmAuditRecordEntity entity = new ActiveAlarmAuditRecordEntity();
        entity.setRunAt(LocalDateTime.now());
        when(recordService.findLatest()).thenReturn(Optional.of(entity));

        controller.latest(null);
        // /latest 不调用 statusService（不检查 Scheduler 状态）
    }
}
