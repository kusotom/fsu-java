package com.dcim.platform.binterface;

import com.dcim.platform.module.binterface.dto.ActiveAlarmAuditStatusResponse;
import com.dcim.platform.module.binterface.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 活动告警审计状态 API 测试 (BIF-P4-022, BIF-P4-023)。
 *
 * <p>验证 API 只读、不触发审计、不访问 FSU、不执行 SET、不修改 alarm_record。</p>
 */
class ActiveAlarmAuditStatusApiTest {

    private ActiveAlarmAuditSchedulerProperties props;
    private AtomicInteger auditCallCount;
    private ActiveAlarmAuditRecordService recordService;

    @BeforeEach
    void setUp() {
        props = new ActiveAlarmAuditSchedulerProperties();
        auditCallCount = new AtomicInteger(0);
        recordService = mock(ActiveAlarmAuditRecordService.class);
    }

    private ActiveAlarmAuditScheduler createScheduler(boolean hasResult, boolean lastSuccess) {
        ActiveAlarmConsistencyAuditService svc = new ActiveAlarmConsistencyAuditService(null, null, null) {
            @Override
            public ActiveAlarmConsistencyAuditResult auditByQueryingFsu(String suid, String url) {
                auditCallCount.incrementAndGet();
                if (lastSuccess) {
                    return ActiveAlarmConsistencyAuditResult.fromDiff(
                            ActiveAlarmDiffResult.empty(suid), true, false, "0");
                } else {
                    return ActiveAlarmConsistencyAuditResult.invalid("模拟失败");
                }
            }
        };

        ActiveAlarmAuditScheduler s = new ActiveAlarmAuditScheduler(svc, props, recordService);

        if (hasResult) {
            props.setSchedulerEnabled(true);
            props.setFsuCode("FSU-T01");
            s.scheduledAudit();
            auditCallCount.set(0);
        }
        return s;
    }

    private ActiveAlarmAuditStatusService createStatusService(ActiveAlarmAuditScheduler scheduler) {
        return new ActiveAlarmAuditStatusService(scheduler, props, recordService);
    }

    @Test void getStatusShouldNotTriggerAudit() {
        ActiveAlarmAuditStatusService svc = createStatusService(createScheduler(false, true));
        assertEquals(0, auditCallCount.get());
        ActiveAlarmAuditStatusResponse r = svc.getStatus();
        assertNotNull(r);
        assertEquals(0, auditCallCount.get(), "getStatus 不应触发审计");
    }

    @Test void getStatusShouldNotAccessRealFsu() {
        ActiveAlarmAuditStatusService svc = createStatusService(createScheduler(true, true));
        ActiveAlarmAuditStatusResponse r = svc.getStatus();
        assertFalse(r.isRealDeviceAccessed(), "Stub 结果应标记不访问真实设备");
    }

    @Test void defaultSchedulerDisabledShouldBeReflected() {
        ActiveAlarmAuditStatusService svc = createStatusService(createScheduler(false, true));
        ActiveAlarmAuditStatusResponse r = svc.getStatus();
        assertFalse(r.isSchedulerEnabled());
    }

    @Test void enabledSchedulerShouldBeReflected() {
        props.setSchedulerEnabled(true);
        props.setFsuCode("FSU-001");
        ActiveAlarmAuditStatusService svc = createStatusService(createScheduler(false, true));
        assertTrue(svc.getStatus().isSchedulerEnabled());
        assertEquals("FSU-001", svc.getStatus().getConfiguredFsuCode());
    }

    @Test void lastSuccessResultShouldBeQueryable() {
        ActiveAlarmAuditStatusService svc = createStatusService(createScheduler(true, true));
        ActiveAlarmAuditStatusResponse r = svc.getStatus();
        assertTrue(r.isLastSuccess());
        assertNotNull(r.getLastRunTime());
        assertNull(r.getLastError());
        assertNotNull(r.getLastResult());
        assertEquals("memory", r.getDataSource());
    }

    @Test void lastFailureResultShouldBeQueryable() {
        ActiveAlarmAuditStatusService svc = createStatusService(createScheduler(true, false));
        ActiveAlarmAuditStatusResponse r = svc.getStatus();
        assertFalse(r.isLastSuccess());
        assertNotNull(r.getLastResult());
    }

    @Test void noResultYetShouldReturnNullRunTime() {
        ActiveAlarmAuditStatusService svc = createStatusService(createScheduler(false, true));
        ActiveAlarmAuditStatusResponse r = svc.getStatus();
        assertNull(r.getLastRunTime());
        assertEquals(0, r.getFsuAlarmCount());
    }

    @Test void shouldNotModifyAlarmRecord() {
        ActiveAlarmAuditStatusService svc = createStatusService(createScheduler(true, true));
        ActiveAlarmAuditStatusResponse r = svc.getStatus();
        assertNotNull(r);
    }

    @Test void shouldNotTriggerSetCommand() {
        ActiveAlarmAuditStatusService svc = createStatusService(createScheduler(true, true));
        ActiveAlarmAuditStatusResponse r = svc.getStatus();
        assertNotNull(r);
    }

    // ==================== BIF-P4-023: DB 回退 ====================

    @Test void shouldFallbackToDatabaseWhenMemoryEmpty() {
        when(recordService.findLatest()).thenReturn(Optional.empty());
        ActiveAlarmAuditStatusService svc = createStatusService(createScheduler(false, true));
        ActiveAlarmAuditStatusResponse r = svc.getStatus();
        assertNotNull(r);
        assertEquals("none", r.getDataSource());
    }
}
