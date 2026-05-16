package com.dcim.platform.module.binterface.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 活动告警差异核对编排服务 (BIF-P4-019)。
 *
 * <p>编排本地 alarm_record 查询 + ActiveAlarmDiffService 差异核对。
 * 只读，不修改本地告警状态。</p>
 */
@Service
public class ActiveAlarmDiffAuditService {

    private static final Logger log = LoggerFactory.getLogger(ActiveAlarmDiffAuditService.class);

    private final LocalActiveAlarmSnapshotService snapshotService;
    private final ActiveAlarmDiffService diffService;

    public ActiveAlarmDiffAuditService(LocalActiveAlarmSnapshotService snapshotService,
                                        ActiveAlarmDiffService diffService) {
        this.snapshotService = snapshotService;
        this.diffService = diffService;
    }

    /**
     * 查询本地活动告警并与 FSU 快照差异核对。
     *
     * @param suid       FSU 编码
     * @param fsuAlarms  GET_ACTIVEALARM 返回的 FSU 活动告警快照
     * @return 差异结果
     */
    public ActiveAlarmDiffResult diffWithLocal(String suid,
                                                List<GetActiveAlarmResult.ActiveAlarmItem> fsuAlarms) {
        List<ActiveAlarmDiffService.LocalAlarmSnapshot> localSnapshots =
                snapshotService.findLocalActiveSnapshots(suid);
        return diffService.diff(suid, fsuAlarms, localSnapshots);
    }
}
