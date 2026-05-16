package com.dcim.platform.binterface;

import com.dcim.platform.module.alarm.entity.AlarmRecordEntity;
import com.dcim.platform.module.alarm.repository.AlarmRecordRepository;
import com.dcim.platform.module.binterface.service.*;
import com.dcim.platform.module.resource.entity.FsuDeviceEntity;
import com.dcim.platform.module.resource.repository.FsuDeviceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * GET_ACTIVEALARM + diff 编排审计测试 (BIF-P4-020)。
 */
class ActiveAlarmConsistencyAuditServiceTest {

    private ActiveAlarmConsistencyAuditService svc;
    private AlarmRecordRepository alarmRepo;
    private FsuDeviceRepository fsuDeviceRepo;

    @BeforeEach
    void setUp() {
        alarmRepo = mock(AlarmRecordRepository.class);
        fsuDeviceRepo = mock(FsuDeviceRepository.class);
        LocalActiveAlarmSnapshotService snapSvc = new LocalActiveAlarmSnapshotService(alarmRepo, fsuDeviceRepo);
        ActiveAlarmDiffService diffSvc = new ActiveAlarmDiffService();

        // Stub GetActiveAlarmService: returns 1 active alarm
        GetActiveAlarmService fsuSvc = new GetActiveAlarmService(null, null) {
            @Override
            public GetActiveAlarmResult execute(String fsuCode, String url) {
                var alarm = fsuAlarm("SN001", "DEV1", "SP01", "一级", "开始", "46.1");
                return GetActiveAlarmResult.success(fsuCode, List.of(alarm), 1, 1, 0);
            }
        };

        svc = new ActiveAlarmConsistencyAuditService(fsuSvc, snapSvc, diffSvc);
    }

    // ==================== null/empty ====================

    @Test void nullSuidShouldFail() {
        assertFalse(svc.auditWithProvidedSnapshot(null, List.of()).isSuccess());
    }

    // ==================== auditWithProvidedSnapshot ====================

    @Test void providedSnapshotBothEmptyShouldSucceed() {
        var r = svc.auditWithProvidedSnapshot("FSU-001", List.of());
        assertTrue(r.isSuccess());
        assertEquals(0, r.getFsuCount());
        assertFalse(r.isFsuQueryExecuted());
    }

    @Test void providedSnapshotFsuOnlyShouldDetect() {
        setupDevice("FSU-001", 1L);
        var fsu = fsuAlarm("SN001", "DEV1", "SP01", "一级", "开始", "46.1");
        var r = svc.auditWithProvidedSnapshot("FSU-001", List.of(fsu));
        assertEquals(1, r.getFsuOnlyCount());
        assertTrue(r.hasInconsistency());
    }

    @Test void providedSnapshotLocalOnlyShouldDetect() {
        setupDevice("FSU-001", 1L);
        var e = entity("ACTIVE", "一级", "46.1", "SP01");
        when(alarmRepo.findByFsuId(1L)).thenReturn(List.of(e));
        var r = svc.auditWithProvidedSnapshot("FSU-001", List.of());
        assertEquals(1, r.getLocalOnlyCount());
    }

    @Test void providedSnapshotMatchedShouldDetect() {
        setupDevice("FSU-001", 1L);
        var e = entity("ACTIVE", "一级", "46.1", "SP01");
        e.setAlarmDesc("desc");
        when(alarmRepo.findByFsuId(1L)).thenReturn(List.of(e));
        var fsu = fsuAlarm(null, null, "SP01", "一级", "ACTIVE", "46.1");
        var r = svc.auditWithProvidedSnapshot("FSU-001", List.of(fsu));
        assertEquals(1, r.getMatchedCount());
        assertFalse(r.hasInconsistency());
    }

    // ==================== auditByQueryingFsu ====================

    @Test void auditByQueryingFsuShouldSucceed() {
        setupDevice("FSU-001", 1L);
        var e = entity("ACTIVE", "一级", "46.1", "SP01");
        when(alarmRepo.findByFsuId(1L)).thenReturn(List.of(e));
        var r = svc.auditByQueryingFsu("FSU-001", null);
        assertTrue(r.isSuccess());
        assertTrue(r.isFsuQueryExecuted());
        assertFalse(r.isRealDeviceAccessed());
    }

    // ==================== 统计正确性 ====================

    @Test void countsShouldBeCorrect() {
        setupDevice("FSU-001", 1L);
        when(alarmRepo.findByFsuId(1L)).thenReturn(List.of());

        var fsu1 = fsuAlarm("SN001", "DEV1", "SP01", "一级", "开始", "46.1");
        var fsu2 = fsuAlarm("SN002", "DEV2", "SP02", "二级", "开始", "62.0");
        var r = svc.auditWithProvidedSnapshot("FSU-001", List.of(fsu1, fsu2));

        assertEquals(2, r.getFsuCount());
        assertEquals(0, r.getLocalCount());
        assertEquals(2, r.getFsuOnlyCount());
        assertEquals(0, r.getMatchedCount());
        assertEquals(0, r.getLocalOnlyCount());
    }

    // ==================== 不修改 ====================

    @Test void shouldNotCallSave() {
        setupDevice("FSU-001", 1L);
        when(alarmRepo.findByFsuId(1L)).thenReturn(List.of());
        svc.auditWithProvidedSnapshot("FSU-001", List.of());
        verify(alarmRepo, never()).save(any());
        verify(alarmRepo, never()).delete(any());
    }

    @Test void shouldNotCallSaveOnAuditByQueryingFsu() {
        setupDevice("FSU-001", 1L);
        when(alarmRepo.findByFsuId(1L)).thenReturn(List.of());
        svc.auditByQueryingFsu("FSU-001", null);
        verify(alarmRepo, never()).save(any());
        verify(alarmRepo, never()).delete(any());
    }

    // ==================== toString ====================

    @Test void toStringShouldNotContainSensitiveData() {
        setupDevice("FSU-001", 1L);
        var r = svc.auditWithProvidedSnapshot("FSU-001", List.of());
        String s = r.toString();
        assertFalse(s.contains("password"));
        assertFalse(s.contains("secret"));
    }

    // ==================== 辅助 ====================

    private void setupDevice(String fsuCode, Long id) {
        FsuDeviceEntity dev = new FsuDeviceEntity();
        dev.setId(id); dev.setFsuCode(fsuCode);
        when(fsuDeviceRepo.findByFsuCode(fsuCode)).thenReturn(Optional.of(dev));
    }

    private GetActiveAlarmResult.ActiveAlarmItem fsuAlarm(String sn, String devId, String spid,
                                                            String level, String flag, String val) {
        return new GetActiveAlarmResult.ActiveAlarmItem(
                sn, null, devId, spid, LocalDateTime.now(), null, val, level, flag, "desc", null);
    }

    private AlarmRecordEntity entity(String status, String level, String value, String pointCode) {
        AlarmRecordEntity e = new AlarmRecordEntity();
        e.setAlarmStatus(status); e.setAlarmLevel(level);
        e.setAlarmValue(value); e.setPointCode(pointCode);
        return e;
    }
}
