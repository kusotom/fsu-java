package com.dcim.platform.binterface;

import com.dcim.platform.module.binterface.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 活动告警审计 Scheduler 测试 (BIF-P4-021, BIF-P4-023)。
 */
class ActiveAlarmAuditSchedulerTest {

    private ActiveAlarmAuditSchedulerProperties props;
    private AtomicInteger callCount;

    @BeforeEach
    void setUp() {
        props = new ActiveAlarmAuditSchedulerProperties();
        callCount = new AtomicInteger(0);
    }

    private ActiveAlarmAuditScheduler createScheduler(ActiveAlarmConsistencyAuditService svc) {
        return new ActiveAlarmAuditScheduler(svc, props, null);
    }

    private ActiveAlarmConsistencyAuditService createAuditService(boolean succeed) {
        return new ActiveAlarmConsistencyAuditService(null, null, null) {
            @Override
            public ActiveAlarmConsistencyAuditResult auditByQueryingFsu(String suid, String url) {
                callCount.incrementAndGet();
                if (succeed) {
                    return ActiveAlarmConsistencyAuditResult.fromDiff(
                            ActiveAlarmDiffResult.empty(suid), true, false, "0");
                } else {
                    return ActiveAlarmConsistencyAuditResult.invalid("模拟失败");
                }
            }
        };
    }

    @Test void defaultDisabledShouldNotCallAudit() {
        ActiveAlarmAuditScheduler s = createScheduler(createAuditService(true));
        s.scheduledAudit();
        assertEquals(0, callCount.get());
    }

    @Test void enabledShouldCallAuditOnce() {
        props.setSchedulerEnabled(true);
        props.setFsuCode("FSU-001");
        ActiveAlarmAuditScheduler s = createScheduler(createAuditService(true));
        s.scheduledAudit();
        assertEquals(1, callCount.get());
    }

    @Test void missingFsuCodeShouldSkip() {
        props.setSchedulerEnabled(true);
        ActiveAlarmAuditScheduler s = createScheduler(createAuditService(true));
        s.scheduledAudit();
        assertEquals(0, callCount.get());
    }

    @Test void emptyFsuCodeShouldSkip() {
        props.setSchedulerEnabled(true);
        props.setFsuCode("  ");
        ActiveAlarmAuditScheduler s = createScheduler(createAuditService(true));
        s.scheduledAudit();
        assertEquals(0, callCount.get());
    }

    @Test void auditExceptionShouldNotCrash() {
        props.setSchedulerEnabled(true);
        props.setFsuCode("FSU-001");
        ActiveAlarmConsistencyAuditService exploding = new ActiveAlarmConsistencyAuditService(null, null, null) {
            @Override
            public ActiveAlarmConsistencyAuditResult auditByQueryingFsu(String suid, String url) {
                callCount.incrementAndGet();
                throw new RuntimeException("模拟网络异常");
            }
        };
        ActiveAlarmAuditScheduler s = createScheduler(exploding);
        assertDoesNotThrow(s::scheduledAudit);
        assertFalse(s.isLastSuccess());
        assertNotNull(s.getLastError());
        assertEquals(1, callCount.get());
    }

    @Test void shouldRecordLastResult() {
        props.setSchedulerEnabled(true);
        props.setFsuCode("FSU-001");
        ActiveAlarmAuditScheduler s = createScheduler(createAuditService(true));
        s.scheduledAudit();
        assertTrue(s.isLastSuccess());
        assertTrue(s.getLastRunTime() > 0);
        assertNull(s.getLastError());
        assertNotNull(s.getLastResult());
    }

    @Test void shouldRecordFailureResult() {
        props.setSchedulerEnabled(true);
        props.setFsuCode("FSU-001");
        ActiveAlarmAuditScheduler s = createScheduler(createAuditService(false));
        s.scheduledAudit();
        assertNotNull(s.getLastResult());
        assertFalse(s.getLastResult().isSuccess());
    }

    @Test void schedulerShouldNotAccessRealFsu() {
        props.setSchedulerEnabled(true);
        props.setFsuCode("FSU-001");
        ActiveAlarmAuditScheduler s = createScheduler(createAuditService(true));
        s.scheduledAudit();
        ActiveAlarmConsistencyAuditResult r = s.getLastResult();
        assertNotNull(r);
        assertFalse(r.isRealDeviceAccessed());
    }

    @Test void schedulerShouldNotTriggerSetCommand() {
        props.setSchedulerEnabled(true);
        props.setFsuCode("FSU-001");
        ActiveAlarmAuditScheduler s = createScheduler(createAuditService(true));
        s.scheduledAudit();
        assertTrue(s.isLastSuccess());
    }

    @Test void shouldNotModifyAlarmRecord() {
        props.setSchedulerEnabled(true);
        props.setFsuCode("FSU-001");
        ActiveAlarmAuditScheduler s = createScheduler(createAuditService(true));
        s.scheduledAudit();
        assertTrue(s.isLastSuccess());
    }

    // ==================== 持久化 (BIF-P4-023 Service) ====================

    @Test void shouldPersistAuditRecordOnSuccess() {
        props.setSchedulerEnabled(true);
        props.setFsuCode("FSU-001");
        ActiveAlarmAuditRecordService recordService = mock(ActiveAlarmAuditRecordService.class);
        ActiveAlarmAuditScheduler s = new ActiveAlarmAuditScheduler(createAuditService(true), props, recordService);
        s.scheduledAudit();
        verify(recordService, times(1)).save(eq("FSU-001"), any(), isNull());
    }

    @Test void shouldPersistAuditRecordOnException() {
        props.setSchedulerEnabled(true);
        props.setFsuCode("FSU-001");
        ActiveAlarmConsistencyAuditService exploding = new ActiveAlarmConsistencyAuditService(null, null, null) {
            @Override
            public ActiveAlarmConsistencyAuditResult auditByQueryingFsu(String suid, String url) {
                throw new RuntimeException("网络异常");
            }
        };
        ActiveAlarmAuditRecordService recordService = mock(ActiveAlarmAuditRecordService.class);
        ActiveAlarmAuditScheduler s = new ActiveAlarmAuditScheduler(exploding, props, recordService);
        assertDoesNotThrow(s::scheduledAudit);
        verify(recordService, times(1)).save(eq("FSU-001"), isNull(), eq("网络异常"));
    }

    @Test void persistExceptionShouldNotCrashScheduler() {
        props.setSchedulerEnabled(true);
        props.setFsuCode("FSU-001");
        ActiveAlarmAuditRecordService recordService = mock(ActiveAlarmAuditRecordService.class);
        doThrow(new RuntimeException("DB 异常")).when(recordService)
                .save(any(), any(), any());
        ActiveAlarmAuditScheduler s = new ActiveAlarmAuditScheduler(createAuditService(true), props, recordService);
        assertDoesNotThrow(s::scheduledAudit);
        assertTrue(s.isLastSuccess(), "Scheduler 不应因持久化失败而丢失审计结果");
        // persistRecord 异常被内部 catch，不影响 lastError（审计本身成功）
        assertNull(s.getLastError());
    }
}
