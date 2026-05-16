package com.dcim.platform.module.binterface.safety;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * SET 类命令安全判定结果 (BIF-P4-SAFE-001)。
 */
public class SetCommandSafetyDecision {

    private final boolean allowed;
    private final boolean dryRun;
    private final boolean realCallAllowed;
    private final boolean confirmationRequired;
    private final boolean confirmationProvided;
    private final boolean auditRequired;
    private final boolean schedulerForbidden;
    private final boolean highRisk;
    private final String commandName;
    private final String normalizedCommandName;
    private final String suid;
    private final String reasonCode;
    private final String reasonMessage;
    private final List<String> errors;
    private final List<String> warnings;

    private SetCommandSafetyDecision(Builder b) {
        this.allowed = b.allowed; this.dryRun = b.dryRun; this.realCallAllowed = b.realCallAllowed;
        this.confirmationRequired = b.confirmationRequired; this.confirmationProvided = b.confirmationProvided;
        this.auditRequired = b.auditRequired; this.schedulerForbidden = b.schedulerForbidden;
        this.highRisk = b.highRisk; this.commandName = b.commandName;
        this.normalizedCommandName = b.normalizedCommandName; this.suid = b.suid;
        this.reasonCode = b.reasonCode; this.reasonMessage = b.reasonMessage;
        this.errors = b.errors != null ? List.copyOf(b.errors) : List.of();
        this.warnings = b.warnings != null ? List.copyOf(b.warnings) : List.of();
    }

    public static Builder builder() { return new Builder(); }

    public boolean isAllowed() { return allowed; }
    public boolean isDryRun() { return dryRun; }
    public boolean isRealCallAllowed() { return realCallAllowed; }
    public boolean isConfirmationRequired() { return confirmationRequired; }
    public boolean isConfirmationProvided() { return confirmationProvided; }
    public boolean isAuditRequired() { return auditRequired; }
    public boolean isSchedulerForbidden() { return schedulerForbidden; }
    public boolean isHighRisk() { return highRisk; }
    public String getCommandName() { return commandName; }
    public String getNormalizedCommandName() { return normalizedCommandName; }
    public String getSuid() { return suid; }
    public String getReasonCode() { return reasonCode; }
    public String getReasonMessage() { return reasonMessage; }
    public List<String> getErrors() { return errors; }
    public List<String> getWarnings() { return warnings; }

    @Override
    public String toString() {
        return "SetCommandSafetyDecision{allowed=" + allowed + ", dryRun=" + dryRun
                + ", command=" + commandName + ", reason=" + reasonCode + "}";
    }

    public static class Builder {
        private boolean allowed, dryRun, realCallAllowed, confirmationRequired, confirmationProvided;
        private boolean auditRequired, schedulerForbidden, highRisk;
        private String commandName, normalizedCommandName, suid, reasonCode, reasonMessage;
        private List<String> errors = new ArrayList<>(), warnings = new ArrayList<>();

        public Builder allowed(boolean v) { this.allowed = v; return this; }
        public Builder dryRun(boolean v) { this.dryRun = v; return this; }
        public Builder realCallAllowed(boolean v) { this.realCallAllowed = v; return this; }
        public Builder confirmationRequired(boolean v) { this.confirmationRequired = v; return this; }
        public Builder confirmationProvided(boolean v) { this.confirmationProvided = v; return this; }
        public Builder auditRequired(boolean v) { this.auditRequired = v; return this; }
        public Builder schedulerForbidden(boolean v) { this.schedulerForbidden = v; return this; }
        public Builder highRisk(boolean v) { this.highRisk = v; return this; }
        public Builder commandName(String v) { this.commandName = v; return this; }
        public Builder normalizedCommandName(String v) { this.normalizedCommandName = v; return this; }
        public Builder suid(String v) { this.suid = v; return this; }
        public Builder reasonCode(String v) { this.reasonCode = v; return this; }
        public Builder reasonMessage(String v) { this.reasonMessage = v; return this; }
        public Builder addError(String e) { this.errors.add(e); return this; }
        public Builder addWarning(String w) { this.warnings.add(w); return this; }
        public SetCommandSafetyDecision build() { return new SetCommandSafetyDecision(this); }
    }
}
