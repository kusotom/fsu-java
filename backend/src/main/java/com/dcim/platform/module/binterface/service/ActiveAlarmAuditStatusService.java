package com.dcim.platform.module.binterface.service;

import com.dcim.platform.module.binterface.dto.ActiveAlarmAuditStatusResponse;
import com.dcim.platform.module.binterface.entity.ActiveAlarmAuditRecordEntity;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * 活动告警审计状态只读查询服务 (BIF-P4-022, BIF-P4-023)。
 *
 * <p>优先从 {@link ActiveAlarmAuditScheduler} 内存读取最近状态；
 * 内存为空时回退到数据库查询（BIF-P4-023 持久化）。
 * 纯只读，不触发审计、不访问 FSU、不修改 alarm_record。</p>
 */
@Service
public class ActiveAlarmAuditStatusService {

    private final ActiveAlarmAuditScheduler scheduler;
    private final ActiveAlarmAuditSchedulerProperties properties;
    private final ActiveAlarmAuditRecordService recordService;

    public ActiveAlarmAuditStatusService(ActiveAlarmAuditScheduler scheduler,
                                         ActiveAlarmAuditSchedulerProperties properties,
                                         ActiveAlarmAuditRecordService recordService) {
        this.scheduler = scheduler;
        this.properties = properties;
        this.recordService = recordService;
    }

    /**
     * 获取当前审计状态快照（不触发新审计）。
     *
     * <p>优先返回内存中的最近结果；如果内存为空则从数据库回退查询。</p>
     */
    public ActiveAlarmAuditStatusResponse getStatus() {
        ActiveAlarmAuditStatusResponse.Builder b = ActiveAlarmAuditStatusResponse.builder();

        // 配置摘要
        b.schedulerEnabled(properties.isSchedulerEnabled())
         .configuredFsuCode(properties.getFsuCode())
         .fixedDelayMs(properties.getFixedDelayMs())
         .initialDelayMs(properties.getInitialDelayMs());

        // 最近一次运行状态
        if (scheduler.getLastRunTime() > 0) {
            b.lastRunTime(scheduler.getLastRunTime())
             .lastSuccess(scheduler.isLastSuccess())
             .lastError(scheduler.getLastError())
             .dataSource("memory");

            ActiveAlarmConsistencyAuditResult r = scheduler.getLastResult();
            if (r != null) {
                b.realDeviceAccessed(r.isRealDeviceAccessed())
                 .fsuAlarmCount(r.getFsuCount())
                 .platformAlarmCount(r.getLocalCount())
                 .matchedCount(r.getMatchedCount())
                 .missingInPlatformCount(r.getFsuOnlyCount())
                 .extraInPlatformCount(r.getLocalOnlyCount())
                 .mismatchedCount(r.getMismatchCount())
                 .lastResult(r.toString());
            }
        } else {
            // 内存为空，从数据库回退查询 (BIF-P4-023)
            Optional<ActiveAlarmAuditRecordEntity> dbRecord = recordService.findLatest();
            if (dbRecord.isPresent()) {
                ActiveAlarmAuditRecordEntity e = dbRecord.get();
                b.lastRunTime(e.getRunAt() != null
                        ? e.getRunAt().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
                        : null)
                 .lastSuccess(e.isSuccess())
                 .lastError(e.getErrorMessage())
                 .dataSource("database")
                 .realDeviceAccessed(e.isRealDeviceAccessed())
                 .fsuAlarmCount(e.getFsuCount())
                 .platformAlarmCount(e.getLocalCount())
                 .matchedCount(e.getMatchedCount())
                 .missingInPlatformCount(e.getFsuOnlyCount())
                 .extraInPlatformCount(e.getLocalOnlyCount())
                 .mismatchedCount(e.getMismatchCount())
                 .lastResult(e.getSummary());
            } else {
                b.dataSource("none");
            }
        }

        return b.build();
    }
}
