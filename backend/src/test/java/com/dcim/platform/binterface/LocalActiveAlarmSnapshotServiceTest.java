package com.dcim.platform.binterface;

import com.dcim.platform.module.alarm.entity.AlarmRecordEntity;
import com.dcim.platform.module.alarm.repository.AlarmRecordRepository;
import com.dcim.platform.module.binterface.service.ActiveAlarmDiffService;
import com.dcim.platform.module.binterface.service.LocalActiveAlarmSnapshotService;
import com.dcim.platform.module.resource.entity.FsuDeviceEntity;
import com.dcim.platform.module.resource.repository.FsuDeviceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 本地 alarm_record 只读适配测试 (BIF-P4-019)。
 */
class LocalActiveAlarmSnapshotServiceTest {

    private AlarmRecordRepository alarmRepo;
    private FsuDeviceRepository fsuDeviceRepo;
    private LocalActiveAlarmSnapshotService svc;

    @BeforeEach
    void setUp() {
        alarmRepo = mock(AlarmRecordRepository.class);
        fsuDeviceRepo = mock(FsuDeviceRepository.class);
        svc = new LocalActiveAlarmSnapshotService(alarmRepo, fsuDeviceRepo);
    }

    @Test void shouldReturnEmptyForNullSuid() {
        assertTrue(svc.findLocalActiveSnapshots(null).isEmpty());
    }

    @Test void shouldReturnEmptyForUnknownFsu() {
        when(fsuDeviceRepo.findByFsuCode("UNKNOWN")).thenReturn(Optional.empty());
        assertTrue(svc.findLocalActiveSnapshots("UNKNOWN").isEmpty());
    }

    @Test void shouldReturnActiveOnly() {
        FsuDeviceEntity dev = new FsuDeviceEntity(); dev.setId(1L);
        when(fsuDeviceRepo.findByFsuCode("FSU-001")).thenReturn(Optional.of(dev));

        AlarmRecordEntity active = entity("ACTIVE", "一级", "46.1", "TEMP-001");
        AlarmRecordEntity cleared = entity("CLEARED", "一级", "46.1", "TEMP-001");
        when(alarmRepo.findByFsuId(1L)).thenReturn(List.of(active, cleared));

        List<ActiveAlarmDiffService.LocalAlarmSnapshot> snaps = svc.findLocalActiveSnapshots("FSU-001");
        assertEquals(1, snaps.size()); // only ACTIVE
    }

    @Test void shouldMapPointCodeToSpid() {
        FsuDeviceEntity dev = new FsuDeviceEntity(); dev.setId(1L);
        when(fsuDeviceRepo.findByFsuCode("FSU-001")).thenReturn(Optional.of(dev));

        AlarmRecordEntity e = entity("ACTIVE", "一级", "46.1", "TEMP-001");
        when(alarmRepo.findByFsuId(1L)).thenReturn(List.of(e));

        var snaps = svc.findLocalActiveSnapshots("FSU-001");
        assertEquals("TEMP-001", snaps.get(0).spid);
    }

    @Test void shouldMapAlarmValueToTriggerVal() {
        FsuDeviceEntity dev = new FsuDeviceEntity(); dev.setId(1L);
        when(fsuDeviceRepo.findByFsuCode("FSU-001")).thenReturn(Optional.of(dev));

        AlarmRecordEntity e = entity("ACTIVE", "二级", "62.0", "TEMP-001");
        when(alarmRepo.findByFsuId(1L)).thenReturn(List.of(e));

        var snaps = svc.findLocalActiveSnapshots("FSU-001");
        assertEquals("62.0", snaps.get(0).triggerVal);
    }

    @Test void shouldMapAlarmLevel() {
        FsuDeviceEntity dev = new FsuDeviceEntity(); dev.setId(1L);
        when(fsuDeviceRepo.findByFsuCode("FSU-001")).thenReturn(Optional.of(dev));

        AlarmRecordEntity e = entity("ACTIVE", "三级", "46.1", "HUMI-001");
        when(alarmRepo.findByFsuId(1L)).thenReturn(List.of(e));

        assertEquals("三级", svc.findLocalActiveSnapshots("FSU-001").get(0).alarmLevel);
    }

    @Test void serialNoShouldBeNull() {
        FsuDeviceEntity dev = new FsuDeviceEntity(); dev.setId(1L);
        when(fsuDeviceRepo.findByFsuCode("FSU-001")).thenReturn(Optional.of(dev));
        when(alarmRepo.findByFsuId(1L)).thenReturn(List.of(entity("ACTIVE", "一级", "46.1", "T-1")));
        assertNull(svc.findLocalActiveSnapshots("FSU-001").get(0).serialNo);
    }

    @Test void shouldNotCallSave() {
        FsuDeviceEntity dev = new FsuDeviceEntity(); dev.setId(1L);
        when(fsuDeviceRepo.findByFsuCode("FSU-001")).thenReturn(Optional.of(dev));
        when(alarmRepo.findByFsuId(1L)).thenReturn(List.of(entity("ACTIVE", "一级", "46.1", "T-1")));
        svc.findLocalActiveSnapshots("FSU-001");
        verify(alarmRepo, never()).save(any());
        verify(alarmRepo, never()).delete(any());
    }

    // ==================== 辅助 ====================

    private AlarmRecordEntity entity(String status, String level, String value, String pointCode) {
        AlarmRecordEntity e = new AlarmRecordEntity();
        e.setAlarmStatus(status);
        e.setAlarmLevel(level);
        e.setAlarmValue(value);
        e.setPointCode(pointCode);
        return e;
    }
}
