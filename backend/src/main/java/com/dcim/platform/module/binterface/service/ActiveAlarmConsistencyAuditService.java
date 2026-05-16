package com.dcim.platform.module.binterface.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * GET_ACTIVEALARM + ActiveAlarmDiff 编排审计服务 (BIF-P4-020)。
 *
 * <p>编排 FSU 活动告警查询 + 本地 alarm_record 查询 + 差异核对。
 * 只读审计，不修改本地告警状态。</p>
 */
@Service
public class ActiveAlarmConsistencyAuditService {

    private static final Logger log = LoggerFactory.getLogger(ActiveAlarmConsistencyAuditService.class);

    private final GetActiveAlarmService getActiveAlarmService;
    private final LocalActiveAlarmSnapshotService snapshotService;
    private final ActiveAlarmDiffService diffService;

    public ActiveAlarmConsistencyAuditService(GetActiveAlarmService getActiveAlarmService,
                                               LocalActiveAlarmSnapshotService snapshotService,
                                               ActiveAlarmDiffService diffService) {
        this.getActiveAlarmService = getActiveAlarmService;
        this.snapshotService = snapshotService;
        this.diffService = diffService;
    }

    /**
     * 使用已获取的 FSU 快照做差异核对（不访问 FSU）。
     */
    public ActiveAlarmConsistencyAuditResult auditWithProvidedSnapshot(
            String suid, List<GetActiveAlarmResult.ActiveAlarmItem> fsuAlarms) {
        if (suid == null || suid.trim().isEmpty())
            return ActiveAlarmConsistencyAuditResult.invalid("缺少 SUID");

        try {
            ActiveAlarmDiffResult diff = diffService.diff(
                    suid, fsuAlarms, snapshotService.findLocalActiveSnapshots(suid));
            return ActiveAlarmConsistencyAuditResult.fromDiff(diff, false, false, null);
        } catch (Exception e) {
            log.error("差异核对失败: suid={}", suid, e);
            return ActiveAlarmConsistencyAuditResult.invalid("差异核对异常: " + e.getMessage());
        }
    }

    /**
     * 查询 FSU + 本地对比（通过 GetActiveAlarmService 访问 FSU）。
     *
     * <p>默认 Stub 不访问真实设备。real-call-enabled=true 时才走真实 HTTP。</p>
     */
    public ActiveAlarmConsistencyAuditResult auditByQueryingFsu(String suid, String serviceUrl) {
        if (suid == null || suid.trim().isEmpty())
            return ActiveAlarmConsistencyAuditResult.invalid("缺少 SUID");
        suid = suid.trim();

        // 1. 查询 FSU 活动告警
        GetActiveAlarmResult fsuResult;
        try {
            fsuResult = getActiveAlarmService.execute(suid, serviceUrl);
        } catch (Exception e) {
            log.error("GET_ACTIVEALARM 查询失败: suid={}", suid, e);
            return ActiveAlarmConsistencyAuditResult.fsuQueryFailed(suid, "5001",
                    "FSU 查询异常: " + e.getMessage());
        }

        if (!fsuResult.isSuccess()) {
            return ActiveAlarmConsistencyAuditResult.fsuQueryFailed(suid,
                    fsuResult.getResultCode(), "FSU 查询失败: " + fsuResult.getResultDesc());
        }

        // 2. 差异核对
        try {
            List<ActiveAlarmDiffService.LocalAlarmSnapshot> localSnapshots =
                    snapshotService.findLocalActiveSnapshots(suid);
            ActiveAlarmDiffResult diff = diffService.diff(suid, fsuResult.getActiveAlarms(), localSnapshots);
            return ActiveAlarmConsistencyAuditResult.fromDiff(diff, true, false, "0");
        } catch (Exception e) {
            log.error("差异核对失败: suid={}", suid, e);
            return ActiveAlarmConsistencyAuditResult.fsuQueryFailed(suid, "5001",
                    "差异核对异常: " + e.getMessage());
        }
    }
}
