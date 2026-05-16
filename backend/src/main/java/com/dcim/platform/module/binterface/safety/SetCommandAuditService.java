package com.dcim.platform.module.binterface.safety;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * SET 命令审计服务 (BIF-P4-SAFE-003)。
 *
 * <p>内存实现，不持久化。生产环境需替换为数据库实现。</p>
 */
@Service
public class SetCommandAuditService {

    private static final Logger log = LoggerFactory.getLogger(SetCommandAuditService.class);
    private final List<SetCommandAuditRecord> records = new CopyOnWriteArrayList<>();

    /** 记录被拒绝的决策。 */
    public SetCommandAuditRecord recordRejected(SetCommandSafetyDecision decision, String suid,
                                                  String source, String confirmationTokenHash) {
        return record(decision, suid, source, confirmationTokenHash, "REJECTED", "拒绝");
    }

    /** 记录 dry-run。 */
    public SetCommandAuditRecord recordDryRun(SetCommandSafetyDecision decision, String suid,
                                                String source, String confirmationTokenHash) {
        return record(decision, suid, source, confirmationTokenHash, "DRY_RUN", "dry-run");
    }

    /** 记录通过但未真实执行。 */
    public SetCommandAuditRecord recordAllowed(SetCommandSafetyDecision decision, String suid,
                                                 String source, String confirmationTokenHash) {
        return record(decision, suid, source, confirmationTokenHash, "ALLOWED", "门禁通过");
    }

    private SetCommandAuditRecord record(SetCommandSafetyDecision d, String suid,
                                          String source, String tokenHash, String result, String summary) {
        try {
            SetCommandAuditRecord r = SetCommandAuditRecord.builder()
                    .commandName(d.getCommandName())
                    .normalizedCommandName(d.getNormalizedCommandName())
                    .suid(suid != null ? suid : d.getSuid())
                    .requestedBy("SYSTEM")
                    .source(source != null ? source : "UNKNOWN")
                    .operationSummary(summary)
                    .safetyAllowed(d.isAllowed())
                    .safetyReasonCode(d.getReasonCode())
                    .safetyReasonMessage(d.getReasonMessage())
                    .confirmationRequired(d.isConfirmationRequired())
                    .confirmationProvided(d.isConfirmationProvided())
                    .confirmationTokenHash(tokenHash)
                    .dryRun(d.isDryRun())
                    .schedulerTriggered(d.isSchedulerForbidden()
                            && "SCHEDULER_FORBIDDEN".equals(d.getReasonCode()))
                    .auditResult(result)
                    .build();
            records.add(r);
            log.info("SET 审计: {}", r);
            return r;
        } catch (Exception e) {
            log.warn("审计记录写入失败（不影响业务）: {}", e.getMessage());
            return null;
        }
    }

    /** 获取所有记录（只读）。 */
    public List<SetCommandAuditRecord> getAllRecords() {
        return Collections.unmodifiableList(new ArrayList<>(records));
    }

    /** 清除记录（测试用）。 */
    public void clear() { records.clear(); }
    public int count() { return records.size(); }
}
