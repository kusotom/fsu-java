package com.dcim.platform.binterface;

import com.dcim.platform.module.binterface.entity.ActiveAlarmAuditRecordEntity;
import com.dcim.platform.module.binterface.repository.ActiveAlarmAuditRecordRepository;
import com.dcim.platform.module.binterface.service.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 活动告警审计结果持久化 Service 测试 (BIF-P4-023)。
 */
class ActiveAlarmAuditRecordServiceTest {

    private ActiveAlarmAuditRecordRepository repo;
    private ActiveAlarmAuditRecordService service;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        repo = mock(ActiveAlarmAuditRecordRepository.class);
        objectMapper = new ObjectMapper();
        service = new ActiveAlarmAuditRecordService(repo, objectMapper);
    }

    // ==================== save ====================

    @Test void saveSuccessResultShouldPersist() {
        ActiveAlarmConsistencyAuditResult result = ActiveAlarmConsistencyAuditResult.fromDiff(
                ActiveAlarmDiffResult.empty("SUID-T01"), true, false, "0");

        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ActiveAlarmAuditRecordEntity saved = service.save("FSU-001", result, null);
        assertNotNull(saved);
        assertEquals("FSU-001", saved.getFsuCode());
        assertEquals("SUID-T01", saved.getSuid());
        assertTrue(saved.isSuccess());
        assertEquals("0", saved.getQueryResultCode());
        assertFalse(saved.isRealDeviceAccessed());
        assertNotNull(saved.getRunAt());
        assertNotNull(saved.getResultJson());
        assertTrue(saved.getResultJson().contains("SUID-T01"));
        verify(repo, times(1)).save(any());
    }

    @Test void saveFailureResultShouldPersistWithError() {
        String errorMsg = "FSU 查询超时";
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ActiveAlarmAuditRecordEntity saved = service.save("FSU-002", null, errorMsg);
        assertNotNull(saved);
        assertEquals("FSU-002", saved.getFsuCode());
        assertFalse(saved.isSuccess());
        assertEquals(errorMsg, saved.getErrorMessage());
        assertNull(saved.getResultJson());
        verify(repo, times(1)).save(any());
    }

    @Test void saveWithResultJsonShouldIncludeAuditSummary() {
        ActiveAlarmConsistencyAuditResult result = ActiveAlarmConsistencyAuditResult.fromDiff(
                ActiveAlarmDiffResult.of("SUID-T02", 10, 8, 7, 2, 1, 0, List.of(), List.of()),
                true, false, "0");

        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ActiveAlarmAuditRecordEntity saved = service.save("FSU-003", result, null);
        assertNotNull(saved.getResultJson());
        assertTrue(saved.getResultJson().contains("\"fsuCount\":10"));
        assertTrue(saved.getResultJson().contains("\"localCount\":8"));
        assertTrue(saved.getResultJson().contains("\"matchedCount\":7"));
        assertTrue(saved.getResultJson().contains("\"hasInconsistency\":true"));
    }

    // ==================== findLatest ====================

    @Test void findLatestShouldReturnMostRecent() {
        ActiveAlarmAuditRecordEntity entity = new ActiveAlarmAuditRecordEntity();
        entity.setId(1L);
        entity.setFsuCode("FSU-001");
        entity.setSuccess(true);
        entity.setRunAt(java.time.LocalDateTime.now());
        when(repo.findTopByOrderByRunAtDesc()).thenReturn(Optional.of(entity));

        Optional<ActiveAlarmAuditRecordEntity> result = service.findLatest();
        assertTrue(result.isPresent());
        assertEquals("FSU-001", result.get().getFsuCode());
    }

    @Test void findLatestWhenEmptyShouldReturnEmpty() {
        when(repo.findTopByOrderByRunAtDesc()).thenReturn(Optional.empty());
        assertTrue(service.findLatest().isEmpty());
    }

    // ==================== findLatestByFsuCode ====================

    @Test void findLatestByFsuCodeShouldReturnFiltered() {
        ActiveAlarmAuditRecordEntity entity = new ActiveAlarmAuditRecordEntity();
        entity.setFsuCode("FSU-T99");
        when(repo.findTopByFsuCodeOrderByRunAtDesc("FSU-T99")).thenReturn(Optional.of(entity));

        Optional<ActiveAlarmAuditRecordEntity> result = service.findLatestByFsuCode("FSU-T99");
        assertTrue(result.isPresent());
        assertEquals("FSU-T99", result.get().getFsuCode());
    }

    @Test void findLatestByUnknownFsuCodeShouldReturnEmpty() {
        when(repo.findTopByFsuCodeOrderByRunAtDesc("UNKNOWN")).thenReturn(Optional.empty());
        assertTrue(service.findLatestByFsuCode("UNKNOWN").isEmpty());
    }

    // ==================== findHistory ====================

    @Test void findHistoryWithFsuCodeShouldReturnPaginated() {
        PageRequest pageable = PageRequest.of(0, 10);
        ActiveAlarmAuditRecordEntity entity = new ActiveAlarmAuditRecordEntity();
        entity.setFsuCode("FSU-001");
        when(repo.findByFsuCodeOrderByRunAtDesc("FSU-001", pageable))
                .thenReturn(new PageImpl<>(List.of(entity), pageable, 1));

        Page<ActiveAlarmAuditRecordEntity> result = service.findHistory("FSU-001", pageable);
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("FSU-001", result.getContent().get(0).getFsuCode());
    }

    @Test void findHistoryWithoutFsuCodeShouldReturnAllPaginated() {
        PageRequest pageable = PageRequest.of(0, 20);
        ActiveAlarmAuditRecordEntity e1 = new ActiveAlarmAuditRecordEntity();
        e1.setFsuCode("FSU-001");
        ActiveAlarmAuditRecordEntity e2 = new ActiveAlarmAuditRecordEntity();
        e2.setFsuCode("FSU-002");
        when(repo.findAllByOrderByRunAtDesc(pageable))
                .thenReturn(new PageImpl<>(List.of(e1, e2), pageable, 2));

        Page<ActiveAlarmAuditRecordEntity> result = service.findHistory(null, pageable);
        assertEquals(2, result.getTotalElements());
    }

    @Test void findHistoryWithBlankFsuCodeShouldReturnAll() {
        PageRequest pageable = PageRequest.of(0, 5);
        ActiveAlarmAuditRecordEntity entity = new ActiveAlarmAuditRecordEntity();
        when(repo.findAllByOrderByRunAtDesc(pageable))
                .thenReturn(new PageImpl<>(List.of(entity), pageable, 1));

        Page<ActiveAlarmAuditRecordEntity> result = service.findHistory("  ", pageable);
        assertEquals(1, result.getTotalElements());
    }

    // ==================== 安全边界 ====================

    @Test void saveShouldNotTriggerFsuQuery() {
        ActiveAlarmConsistencyAuditResult result = ActiveAlarmConsistencyAuditResult.fromDiff(
                ActiveAlarmDiffResult.empty("SUID"), false, false, null);
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.save("FSU-001", result, null);
        verify(repo, never()).findAll();
        // Service 只调用 save，不调用任何 FSU 查询方法
    }

    @Test void saveShouldNotModifyAlarmRecord() {
        ActiveAlarmConsistencyAuditResult result = ActiveAlarmConsistencyAuditResult.fromDiff(
                ActiveAlarmDiffResult.empty("SUID"), true, false, "0");
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.save("FSU-001", result, null);
        // Service 不依赖 AlarmRecordRepository
        verify(repo, times(1)).save(any());
    }
}
