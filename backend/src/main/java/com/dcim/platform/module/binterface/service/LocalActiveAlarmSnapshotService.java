package com.dcim.platform.module.binterface.service;

import com.dcim.platform.module.alarm.entity.AlarmRecordEntity;
import com.dcim.platform.module.alarm.repository.AlarmRecordRepository;
import com.dcim.platform.module.resource.entity.FsuDeviceEntity;
import com.dcim.platform.module.resource.repository.FsuDeviceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 本地 alarm_record 只读快照适配服务 (BIF-P4-019)。
 *
 * <p>将 AlarmRecordEntity 转换为 ActiveAlarmDiffService.LocalAlarmSnapshot，
 * 供 ActiveAlarmDiffService 差异核对使用。只读，不改库。</p>
 */
@Service
public class LocalActiveAlarmSnapshotService {

    private static final Logger log = LoggerFactory.getLogger(LocalActiveAlarmSnapshotService.class);

    private final AlarmRecordRepository alarmRepo;
    private final FsuDeviceRepository fsuDeviceRepo;

    public LocalActiveAlarmSnapshotService(AlarmRecordRepository alarmRepo,
                                            FsuDeviceRepository fsuDeviceRepo) {
        this.alarmRepo = alarmRepo;
        this.fsuDeviceRepo = fsuDeviceRepo;
    }

    /**
     * 查询指定 FSU 的本地活动告警快照列表。
     *
     * @param suid FSU 编码 (fsuCode)
     * @return 快照列表，永不为 null
     */
    public List<ActiveAlarmDiffService.LocalAlarmSnapshot> findLocalActiveSnapshots(String suid) {
        if (suid == null || suid.trim().isEmpty()) return List.of();

        Optional<FsuDeviceEntity> devOpt = fsuDeviceRepo.findByFsuCode(suid.trim());
        if (devOpt.isEmpty()) {
            log.debug("FSU 不存在: suid={}", suid);
            return List.of();
        }

        Long fsuId = devOpt.get().getId();
        List<AlarmRecordEntity> entities = alarmRepo.findByFsuId(fsuId);
        List<AlarmRecordEntity> activeOnly = entities.stream()
                .filter(e -> "ACTIVE".equals(e.getAlarmStatus()))
                .toList();

        List<ActiveAlarmDiffService.LocalAlarmSnapshot> snapshots = new ArrayList<>();
        for (AlarmRecordEntity e : activeOnly) {
            snapshots.add(toSnapshot(e, suid));
        }
        return snapshots;
    }

    /**
     * AlarmRecordEntity → LocalAlarmSnapshot 映射 (LANDING-001 增强)。
     *
     * <p>serialNo/deviceId 从实体读取（可空），用于精确匹配。
     * pointCode → spid, alarmStatus → alarmFlag, alarmValue → triggerVal。</p>
     */
    public ActiveAlarmDiffService.LocalAlarmSnapshot toSnapshot(AlarmRecordEntity e, String suid) {
        return ActiveAlarmDiffService.LocalAlarmSnapshot.of(
                e.getSerialNo(),                // serialNo (LANDING-001)
                e.getDeviceId(),                // deviceId (LANDING-001)
                e.getPointCode(),               // spid
                e.getAlarmLevel(),              // alarmLevel
                e.getAlarmStatus(),             // alarmFlag (ACTIVE/CLEARED)
                e.getAlarmValue(),              // triggerVal
                e.getAlarmDesc()                // alarmDesc
        );
    }
}
