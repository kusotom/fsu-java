package com.dcim.platform.module.binterface.safety;

import java.time.Instant;
import java.util.UUID;

/**
 * SET 命令审计记录 (BIF-P4-SAFE-003)。
 *
 * <p>不包含明文 token 和密码。</p>
 */
public class SetCommandAuditRecord {

    private final String auditId = UUID.randomUUID().toString().substring(0, 8);
    private final String commandName;
    private final String normalizedCommandName;
    private final int commandCode;
    private final String suid;
    private final String requestedBy;
    private final String source;
    private final String operationSummary;
    private final int targetCount;
    private final boolean dryRun;
    private final boolean realCallRequested;
    private final boolean safetyAllowed;
    private final String safetyReasonCode;
    private final String safetyReasonMessage;
    private final boolean confirmationRequired;
    private final boolean confirmationProvided;
    private final String confirmationTokenHash;
    private final boolean schedulerTriggered;
    private final String auditResult;
    private final Instant createdAt = Instant.now();

    private SetCommandAuditRecord(Builder b) {
        this.commandName = b.commandName; this.normalizedCommandName = b.normalizedCommandName;
        this.commandCode = b.commandCode; this.suid = b.suid; this.requestedBy = b.requestedBy;
        this.source = b.source; this.operationSummary = b.operationSummary;
        this.targetCount = b.targetCount; this.dryRun = b.dryRun;
        this.realCallRequested = b.realCallRequested; this.safetyAllowed = b.safetyAllowed;
        this.safetyReasonCode = b.safetyReasonCode; this.safetyReasonMessage = b.safetyReasonMessage;
        this.confirmationRequired = b.confirmationRequired;
        this.confirmationProvided = b.confirmationProvided;
        this.confirmationTokenHash = b.confirmationTokenHash;
        this.schedulerTriggered = b.schedulerTriggered; this.auditResult = b.auditResult;
    }

    public static Builder builder() { return new Builder(); }

    public String getAuditId() { return auditId; }
    public String getCommandName() { return commandName; }
    public String getSuid() { return suid; }
    public boolean isSafetyAllowed() { return safetyAllowed; }
    public String getSafetyReasonCode() { return safetyReasonCode; }
    public String getAuditResult() { return auditResult; }
    public Instant getCreatedAt() { return createdAt; }
    public String getConfirmationTokenHash() { return confirmationTokenHash; }
    public boolean isDryRun() { return dryRun; }
    public boolean isSchedulerTriggered() { return schedulerTriggered; }

    @Override
    public String toString() {
        return "Audit{" + auditId + " " + commandName + " suid=" + suid
                + " allowed=" + safetyAllowed + " result=" + auditResult + "}";
    }

    public static class Builder {
        private String commandName, normalizedCommandName, suid, requestedBy = "UNKNOWN", source = "UNKNOWN";
        private String operationSummary, safetyReasonCode, safetyReasonMessage, auditResult = "RECORDED";
        private String confirmationTokenHash;
        private int commandCode, targetCount = 1;
        private boolean dryRun, realCallRequested, safetyAllowed, confirmationRequired, confirmationProvided, schedulerTriggered;

        public Builder commandName(String v) { this.commandName = v; return this; }
        public Builder normalizedCommandName(String v) { this.normalizedCommandName = v; return this; }
        public Builder commandCode(int v) { this.commandCode = v; return this; }
        public Builder suid(String v) { this.suid = v; return this; }
        public Builder requestedBy(String v) { this.requestedBy = v; return this; }
        public Builder source(String v) { this.source = v; return this; }
        public Builder operationSummary(String v) { this.operationSummary = v; return this; }
        public Builder targetCount(int v) { this.targetCount = v; return this; }
        public Builder dryRun(boolean v) { this.dryRun = v; return this; }
        public Builder realCallRequested(boolean v) { this.realCallRequested = v; return this; }
        public Builder safetyAllowed(boolean v) { this.safetyAllowed = v; return this; }
        public Builder safetyReasonCode(String v) { this.safetyReasonCode = v; return this; }
        public Builder safetyReasonMessage(String v) { this.safetyReasonMessage = v; return this; }
        public Builder confirmationRequired(boolean v) { this.confirmationRequired = v; return this; }
        public Builder confirmationProvided(boolean v) { this.confirmationProvided = v; return this; }
        public Builder confirmationTokenHash(String v) { this.confirmationTokenHash = v; return this; }
        public Builder schedulerTriggered(boolean v) { this.schedulerTriggered = v; return this; }
        public Builder auditResult(String v) { this.auditResult = v; return this; }
        public SetCommandAuditRecord build() { return new SetCommandAuditRecord(this); }
    }
}
