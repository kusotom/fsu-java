package com.dcim.platform.module.binterface.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 活动告警定时审计 Scheduler (BIF-P4-021, BIF-P4-023)。
 *
 * <p>默认 disabled（scheduler-enabled=false）。通过配置启用：
 * <pre>
 * b-interface.active-alarm-audit.scheduler-enabled=true
 * b-interface.active-alarm-audit.fsu-code=FSU-001
 * </pre></p>
 *
 * <p>Scheduler 只负责触发编排，不包含 diff/查询/SET 逻辑。
 * 所有业务逻辑由 {@link ActiveAlarmConsistencyAuditService} 负责。
 * 持久化由 {@link ActiveAlarmAuditRecordService} 负责。</p>
 *
 * <h3>安全约束</h3>
 * <ul>
 *   <li>默认关闭（scheduler-enabled=false）</li>
 *   <li>默认不访问真实 FSU（real-call-enabled=false）</li>
 *   <li>不执行 SET 类命令</li>
 *   <li>不修改 alarm_record</li>
 *   <li>缺失 fsuCode 时 skip 而非崩溃</li>
 *   <li>编排服务异常时 catch 而非崩溃</li>
 *   <li>每次审计结果通过 {@link ActiveAlarmAuditRecordService} 持久化</li>
 * </ul>
 */
@Component
public class ActiveAlarmAuditScheduler {

    private static final Logger log = LoggerFactory.getLogger(ActiveAlarmAuditScheduler.class);

    private final ActiveAlarmConsistencyAuditService auditService;
    private final ActiveAlarmAuditSchedulerProperties properties;
    private final ActiveAlarmAuditRecordService auditRecordService;

    /** 最近一次审计结果（内存缓存，便于快速查询）。 */
    private volatile ActiveAlarmConsistencyAuditResult lastResult;
    private volatile long lastRunTime;
    private volatile boolean lastSuccess;
    private volatile String lastError;

    public ActiveAlarmAuditScheduler(ActiveAlarmConsistencyAuditService auditService,
                                      ActiveAlarmAuditSchedulerProperties properties,
                                      ActiveAlarmAuditRecordService auditRecordService) {
        this.auditService = auditService;
        this.properties = properties;
        this.auditRecordService = auditRecordService;
    }

    /**
     * 定时审计入口。默认 disabled，仅当 scheduler-enabled=true 且 fsuCode 已配置时触发。
     */
    @Scheduled(fixedDelayString = "#{@activeAlarmAuditSchedulerProperties.fixedDelayMs}",
               initialDelayString = "#{@activeAlarmAuditSchedulerProperties.initialDelayMs}")
    public void scheduledAudit() {
        if (!properties.isSchedulerEnabled()) {
            return;
        }

        String fsuCode = properties.getFsuCode();
        if (fsuCode == null || fsuCode.trim().isEmpty()) {
            log.debug("活动告警审计 Scheduler 跳过: 缺少 fsuCode 配置");
            return;
        }

        log.info("活动告警审计调度开始: fsuCode={}", fsuCode);
        lastRunTime = System.currentTimeMillis();
        ActiveAlarmConsistencyAuditResult result = null;
        String errorMsg = null;

        try {
            result = auditService.auditByQueryingFsu(fsuCode, null);
            lastResult = result;
            lastSuccess = result.isSuccess();
            lastError = null;
            log.info("活动告警审计调度完成: {}", result);
        } catch (Exception e) {
            lastSuccess = false;
            lastError = e.getMessage();
            errorMsg = e.getMessage();
            log.error("活动告警审计调度异常: fsuCode={}", fsuCode, e);
        }

        // 持久化审计记录
        persistRecord(fsuCode, result, errorMsg);
    }

    private void persistRecord(String fsuCode, ActiveAlarmConsistencyAuditResult result, String errorMsg) {
        if (auditRecordService == null) return;
        try {
            auditRecordService.save(fsuCode, result, errorMsg);
        } catch (Exception e) {
            log.error("审计记录持久化失败: fsuCode={}", fsuCode, e);
        }
    }

    /**
     * dry-run 执行一次审计（不访问真实 FSU，使用本地快照数据）。
     */
    public ActiveAlarmConsistencyAuditResult runOnceDryRun(String suid) {
        log.info("活动告警 dry-run 审计开始: fsuCode={}", suid);
        lastRunTime = System.currentTimeMillis();
        try {
            ActiveAlarmConsistencyAuditResult result = auditService.auditWithProvidedSnapshot(suid, null);
            lastResult = result;
            lastSuccess = result.isSuccess();
            lastError = null;
            persistRecord(suid, result, null);
            return result;
        } catch (Exception e) {
            lastSuccess = false;
            lastError = e.getMessage();
            log.error("dry-run 审计异常: fsuCode={}", suid, e);
            return null;
        }
    }

    /**
     * 真实执行一次审计（需 realCallEnabled + allowedSuids 白名单）。
     *
     * @return 审计结果；如果安全门禁阻止则返回 null
     */
    public ActiveAlarmConsistencyAuditResult runOnceReal(String suid) {
        if (!properties.isRealCallAllowed(suid)) {
            log.warn("真实审计被安全门禁阻止: suid={}, realCallEnabled={}, allowedSuids={}",
                    suid, properties.isRealCallEnabled(), properties.getAllowedSuids());
            lastError = "真实调用被安全门禁阻止: realCallEnabled=" + properties.isRealCallEnabled();
            return null;
        }
        log.info("活动告警真实审计开始: fsuCode={}", suid);
        lastRunTime = System.currentTimeMillis();
        try {
            ActiveAlarmConsistencyAuditResult result = auditService.auditByQueryingFsu(suid, null);
            lastResult = result;
            lastSuccess = result != null && result.isSuccess();
            lastError = null;
            persistRecord(suid, result, null);
            return result;
        } catch (Exception e) {
            lastSuccess = false;
            lastError = e.getMessage();
            log.error("真实审计异常: fsuCode={}", suid, e);
            persistRecord(suid, null, e.getMessage());
            return null;
        }
    }

    public ActiveAlarmConsistencyAuditResult getLastResult() { return lastResult; }
    public long getLastRunTime() { return lastRunTime; }
    public boolean isLastSuccess() { return lastSuccess; }
    public String getLastError() { return lastError; }
}
