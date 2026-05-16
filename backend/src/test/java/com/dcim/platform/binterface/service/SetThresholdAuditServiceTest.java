package com.dcim.platform.binterface.service;

import com.dcim.platform.module.binterface.service.SetThresholdAuditRecord;
import com.dcim.platform.module.binterface.service.SetThresholdAuditService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SetThresholdAuditService 单元测试。
 */
class SetThresholdAuditServiceTest {

    private SetThresholdAuditService auditService;

    @BeforeEach
    void setUp() {
        auditService = new SetThresholdAuditService();
    }

    @Test
    void shouldCreateAuditBuilder() {
        SetThresholdAuditRecord.Builder builder = auditService.begin();

        assertNotNull(builder);
    }

    @Test
    void shouldRecordAuditEntry() {
        SetThresholdAuditRecord record = SetThresholdAuditRecord.builder()
                .operationId("test-001")
                .fsuCode("FSU-001")
                .signalId("TEMP-001")
                .alarmUpper("65.0")
                .alarmLower("-5.0")
                .confirmed(true)
                .operator("TEST")
                .requestTime(LocalDateTime.now())
                .completeTime(LocalDateTime.now())
                .resultCode("0")
                .resultDesc("OK")
                .success(true)
                .realCall(false)
                .build();

        auditService.record(record);

        assertEquals(1, auditService.getRecords().size());
        assertEquals("test-001", auditService.getRecords().get(0).getOperationId());
        assertEquals("FSU-001", auditService.getRecords().get(0).getFsuCode());
    }

    @Test
    void shouldRecordMultipleEntries() {
        auditService.record(SetThresholdAuditRecord.builder()
                .operationId("op-1").fsuCode("FSU-001").signalId("TEMP-001")
                .success(true).realCall(false).build());
        auditService.record(SetThresholdAuditRecord.builder()
                .operationId("op-2").fsuCode("FSU-002").signalId("HUMI-001")
                .success(false).realCall(false).build());

        assertEquals(2, auditService.getRecords().size());
    }

    @Test
    void recordShouldContainRequiredFields() {
        SetThresholdAuditRecord record = SetThresholdAuditRecord.builder()
                .operationId("test-op")
                .commandType("SET_THRESHOLD")
                .fsuCode("FSU-001")
                .signalId("TEMP-001")
                .alarmUpper("65.0")
                .alarmLower("-5.0")
                .alarmUpperUrgent("75.0")
                .alarmLowerUrgent("-10.0")
                .confirmed(true)
                .operator("SYSTEM")
                .requestTime(LocalDateTime.now())
                .completeTime(LocalDateTime.now())
                .resultCode("0")
                .resultDesc("OK")
                .success(true)
                .realCall(false)
                .build();

        assertEquals("SET_THRESHOLD", record.getCommandType());
        assertEquals("FSU-001", record.getFsuCode());
        assertEquals("TEMP-001", record.getSignalId());
        assertEquals("65.0", record.getAlarmUpper());
        assertEquals("-5.0", record.getAlarmLower());
        assertEquals("75.0", record.getAlarmUpperUrgent());
        assertEquals("-10.0", record.getAlarmLowerUrgent());
        assertTrue(record.isConfirmed());
        assertTrue(record.isSuccess());
        assertFalse(record.isRealCall());
    }

    @Test
    void shouldClearRecords() {
        auditService.record(SetThresholdAuditRecord.builder()
                .operationId("op-1").fsuCode("FSU-001").signalId("TEMP-001")
                .success(true).realCall(false).build());

        auditService.clear();
        assertTrue(auditService.getRecords().isEmpty());
    }

    @Test
    void shouldHandleNullFields() {
        SetThresholdAuditRecord record = SetThresholdAuditRecord.builder()
                .operationId("test-null")
                .fsuCode(null)
                .signalId(null)
                .build();

        assertNull(record.getFsuCode());
        assertNull(record.getSignalId());
        assertNotNull(record.getOperationId());
    }
}
