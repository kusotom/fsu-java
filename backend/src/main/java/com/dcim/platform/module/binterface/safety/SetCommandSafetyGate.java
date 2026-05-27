package com.dcim.platform.module.binterface.safety;

import com.dcim.platform.module.binterface.compat.BInterfaceCommandAliasMapper;
import com.dcim.platform.module.binterface.model.BInterfaceCommand2024;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * SET 类命令统一安全门禁 (BIF-P4-SAFE-001)。
 *
 * <p>在所有 SET 类命令执行前调用，判定是否允许、是否需要 dry-run、是否需要确认。
 * 默认全部拒绝，仅显式授权才通过。</p>
 */
@Component
public class SetCommandSafetyGate {

    private static final Logger log = LoggerFactory.getLogger(SetCommandSafetyGate.class);

    /** 2024 标准 SET 命令 Code 集合。 */
    static final Set<Integer> SET_COMMAND_CODES = Set.of(
            105, 205, 307, 403, 701, 803, 901, 1101);

    /** 高风险命令 Code。 */
    static final Set<Integer> HIGH_RISK_CODES = Set.of(403, 701, 803, 1101);

    /**
     * BIF2016-SECURITY-001: B接口2016 标准 SET / 控制 / 配置类命令，全部默认高风险禁用。
     * @see openspec/protocols/binterface-2016/09-security-boundary.md
     */
    static final Set<String> HIGH_RISK_SET_COMMANDS_2016 = Set.of(
            "SET_POINT", "SET_LOGININFO", "SET_FTP", "SET_FSUREBOOT", "SET_THRESHOLD");

    private final SetCommandSafetyProperties properties;

    public SetCommandSafetyGate(SetCommandSafetyProperties properties) {
        this.properties = properties;
    }

    /**
     * 判定 SET 命令是否可以执行。
     *
     * @param commandName         原始命令名
     * @param suid                FSU 编码
     * @param confirmationToken   确认令牌（null 表示未提供）
     * @param schedulerTriggered  是否由 Scheduler 触发
     * @param targetCount         目标数量（>1 表示批量）
     * @param realCallRequested   是否请求真实执行
     * @return 安全判定结果
     */
    public SetCommandSafetyDecision evaluate(String commandName, String suid,
                                              String confirmationToken,
                                              boolean schedulerTriggered,
                                              int targetCount,
                                              boolean realCallRequested) {
        if (commandName == null || commandName.trim().isEmpty()) {
            return SetCommandSafetyDecision.builder()
                    .allowed(false).reasonCode("UNKNOWN_SET_COMMAND")
                    .reasonMessage("命令名为空").addError("命令名为空").build();
        }

        // 1. 归一化命令名: 先查旧→新映射, 再查2024标准名
        String trimmed = commandName.trim();
        Optional<BInterfaceCommand2024> normOpt = BInterfaceCommandAliasMapper.to2024(trimmed);
        if (normOpt.isEmpty()) normOpt = BInterfaceCommand2024.findByName(trimmed);
        String normalized = normOpt.map(BInterfaceCommand2024::getName).orElse(trimmed);
        boolean is2024Set = normOpt.map(c -> SET_COMMAND_CODES.contains(c.getCode())).orElse(false);
        boolean isHighRisk = normOpt.map(c -> HIGH_RISK_CODES.contains(c.getCode())).orElse(false);
        boolean isCompatSet = !is2024Set && isLegacySetCommand(trimmed);
        // BIF2016-SECURITY-001: compat / legacy SET commands are also high risk
        if (isCompatSet) isHighRisk = true;

        // BIF2016-SECURITY-001: unknown SET_* commands must not default-allow
        boolean isUnknownSet = !is2024Set && !isCompatSet && trimmed.toUpperCase().startsWith("SET_");
        if (isUnknownSet) {
            return SetCommandSafetyDecision.builder()
                    .allowed(false).commandName(commandName).normalizedCommandName(normalized)
                    .suid(suid).highRisk(true)
                    .reasonCode("UNKNOWN_SET_COMMAND")
                    .reasonMessage("未知 SET 类命令，默认禁止: " + commandName)
                    .addError("未知 SET 类命令不允许执行").build();
        }

        if (!is2024Set && !isCompatSet) {
            return SetCommandSafetyDecision.builder()
                    .allowed(false).commandName(commandName).normalizedCommandName(normalized)
                    .suid(suid).reasonCode("UNKNOWN_SET_COMMAND")
                    .reasonMessage("非 SET 类命令或未知命令: " + commandName)
                    .addError("命令不在 SET 门禁范围内").build();
        }

        // 2. 获取配置
        SetCommandSafetyProperties.CommandSafetyConfig cfg = properties.forCommand(normalized);
        boolean globalEnabled = properties.isEnabled();

        // 3. 逐项检查
        SetCommandSafetyDecision.Builder b = SetCommandSafetyDecision.builder()
                .commandName(commandName).normalizedCommandName(normalized)
                .suid(suid).highRisk(isHighRisk)
                .confirmationRequired(cfg.isRequireConfirmation())
                .confirmationProvided(confirmationToken != null && !confirmationToken.isEmpty())
                .auditRequired(cfg.isAuditRequired())
                .schedulerForbidden(properties.isSchedulerForbidden());

        // 全局关闭
        if (!globalEnabled) {
            return b.allowed(false).dryRun(true).realCallAllowed(false)
                    .reasonCode("GLOBAL_DISABLED").reasonMessage("全局 SET 命令安全门禁未启用")
                    .addError("b-interface.set-command-safety.enabled=false").build();
        }

        // 命令关闭
        if (!cfg.isEnabled()) {
            return b.allowed(false).dryRun(true).realCallAllowed(false)
                    .reasonCode("COMMAND_DISABLED").reasonMessage("命令 " + normalized + " 未启用")
                    .addError("b-interface.set-command-safety.commands." + normalized + ".enabled=false").build();
        }

        // Scheduler 禁止
        if (schedulerTriggered && properties.isSchedulerForbidden()) {
            return b.allowed(false).dryRun(false).realCallAllowed(false)
                    .reasonCode("SCHEDULER_FORBIDDEN").reasonMessage("Scheduler 不允许执行 SET 类命令")
                    .addError("SET 类命令禁止由定时调度触发").build();
        }

        // 批量禁止
        if (targetCount > 1 && properties.isSingleFsuOnly()) {
            return b.allowed(false).dryRun(false).realCallAllowed(false)
                    .reasonCode("BATCH_NOT_ALLOWED").reasonMessage("不允许批量执行 SET 类命令 (targetCount=" + targetCount + ")")
                    .addError("SET 类命令仅允许单台 FSU 执行").build();
        }

        // 确认令牌
        if (cfg.isRequireConfirmation() && (confirmationToken == null || confirmationToken.isEmpty())) {
            return b.allowed(false).dryRun(false).realCallAllowed(false)
                    .reasonCode("CONFIRMATION_REQUIRED").reasonMessage("需要人工确认令牌")
                    .addError("require-confirmation=true, 未提供 confirmationToken").build();
        }

        // 真实调用检查
        if (!cfg.isAllowRealCall()) {
            if (realCallRequested) {
                return b.allowed(false).dryRun(true).realCallAllowed(false)
                        .reasonCode("REAL_CALL_NOT_ALLOWED").reasonMessage("命令 " + normalized + " 不允许真实调用")
                        .addError("allow-real-call=false, 只能 dry-run").build();
            }
            return b.allowed(true).dryRun(true).realCallAllowed(false).build();
        }

        // 通过
        return b.allowed(true)
                .dryRun(!realCallRequested && properties.isDryRunDefault())
                .realCallAllowed(true)
                .reasonCode("ALLOWED").reasonMessage("安全门禁通过").build();
    }

    /** 检查是否为旧兼容 SET 命令名。 */
    private boolean isLegacySetCommand(String name) {
        return LEGACY_SET_NAMES.contains(name.toUpperCase());
    }

    private static final Set<String> LEGACY_SET_NAMES = Set.of(
            "SET_THRESHOLD", "SET_POINT", "SET_FTP", "SET_FSUREBOOT", "SET_LOGININFO", "TIME_CHECK");
}
