package com.dcim.platform.module.binterface.safety;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * SET 类命令统一安全配置属性 (BIF-P4-SAFE-001)。
 *
 * <p>绑定 application.yml 中 b-interface.set-command-safety 下的配置。
 * 所有开关默认关闭。</p>
 */
@Component
@ConfigurationProperties(prefix = "b-interface.set-command-safety")
public class SetCommandSafetyProperties {

    /** 全局开关：false 时所有 SET 命令禁止真实执行 */
    private boolean enabled = false;

    /** 全局是否允许真实调用 */
    private boolean allowRealCall = false;

    /** 全局是否要求人工确认 */
    private boolean requireConfirmation = true;

    /** 全局是否要求审计 */
    private boolean auditRequired = true;

    /** 是否仅允许单台 FSU */
    private boolean singleFsuOnly = true;

    /** 是否禁止 Scheduler 触发 */
    private boolean schedulerForbidden = true;

    /** 默认 dry-run */
    private boolean dryRunDefault = true;

    /** 逐命令覆盖配置 */
    private Map<String, CommandSafetyConfig> commands = new HashMap<>();

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean v) { this.enabled = v; }
    public boolean isAllowRealCall() { return allowRealCall; }
    public void setAllowRealCall(boolean v) { this.allowRealCall = v; }
    public boolean isRequireConfirmation() { return requireConfirmation; }
    public void setRequireConfirmation(boolean v) { this.requireConfirmation = v; }
    public boolean isAuditRequired() { return auditRequired; }
    public void setAuditRequired(boolean v) { this.auditRequired = v; }
    public boolean isSingleFsuOnly() { return singleFsuOnly; }
    public void setSingleFsuOnly(boolean v) { this.singleFsuOnly = v; }
    public boolean isSchedulerForbidden() { return schedulerForbidden; }
    public void setSchedulerForbidden(boolean v) { this.schedulerForbidden = v; }
    public boolean isDryRunDefault() { return dryRunDefault; }
    public void setDryRunDefault(boolean v) { this.dryRunDefault = v; }
    public Map<String, CommandSafetyConfig> getCommands() { return commands; }
    public void setCommands(Map<String, CommandSafetyConfig> v) { this.commands = v; }

    /** 获取某命令的配置（返回全局默认或命令特定覆盖）。 */
    public CommandSafetyConfig forCommand(String commandName) {
        CommandSafetyConfig c = commands.get(commandName);
        if (c != null) return c;
        // 无逐命令配置时默认禁用（安全优先）
        return new CommandSafetyConfig();
    }

    /**
     * 单条命令安全配置。
     */
    public static class CommandSafetyConfig {
        private boolean enabled = false;
        private boolean allowRealCall = false;
        private boolean requireConfirmation = true;
        private boolean auditRequired = true;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean v) { this.enabled = v; }
        public boolean isAllowRealCall() { return allowRealCall; }
        public void setAllowRealCall(boolean v) { this.allowRealCall = v; }
        public boolean isRequireConfirmation() { return requireConfirmation; }
        public void setRequireConfirmation(boolean v) { this.requireConfirmation = v; }
        public boolean isAuditRequired() { return auditRequired; }
        public void setAuditRequired(boolean v) { this.auditRequired = v; }
    }
}
