package com.dcim.platform.module.binterface.service;

/**
 * SET_TIME 执行请求 (BIF-P4-SAFE-004)。
 */
public class SetTimeExecutionRequest {

    private final String suid;
    private final String serviceUrl;
    private final String targetTime;
    private final String confirmationToken;
    private final String requestedBy;
    private final String source;
    private final boolean schedulerTriggered;
    private final boolean realCallRequested;
    private final int targetCount;
    private final String operationSummary;

    private SetTimeExecutionRequest(Builder b) {
        this.suid = b.suid;
        this.serviceUrl = b.serviceUrl;
        this.targetTime = b.targetTime;
        this.confirmationToken = b.confirmationToken;
        this.requestedBy = b.requestedBy != null ? b.requestedBy : "SYSTEM";
        this.source = b.source != null ? b.source : "UNKNOWN";
        this.schedulerTriggered = b.schedulerTriggered;
        this.realCallRequested = b.realCallRequested;
        this.targetCount = b.targetCount > 0 ? b.targetCount : 1;
        this.operationSummary = b.operationSummary;
    }

    public static Builder builder() { return new Builder(); }

    public String getSuid() { return suid; }
    public String getServiceUrl() { return serviceUrl; }
    public String getTargetTime() { return targetTime; }
    public String getConfirmationToken() { return confirmationToken; }
    public String getRequestedBy() { return requestedBy; }
    public String getSource() { return source; }
    public boolean isSchedulerTriggered() { return schedulerTriggered; }
    public boolean isRealCallRequested() { return realCallRequested; }
    public int getTargetCount() { return targetCount; }
    public String getOperationSummary() { return operationSummary; }

    public static class Builder {
        private String suid, serviceUrl, targetTime, confirmationToken, requestedBy, source, operationSummary;
        private boolean schedulerTriggered, realCallRequested;
        private int targetCount = 1;

        public Builder suid(String v) { this.suid = v; return this; }
        public Builder serviceUrl(String v) { this.serviceUrl = v; return this; }
        public Builder targetTime(String v) { this.targetTime = v; return this; }
        public Builder confirmationToken(String v) { this.confirmationToken = v; return this; }
        public Builder requestedBy(String v) { this.requestedBy = v; return this; }
        public Builder source(String v) { this.source = v; return this; }
        public Builder schedulerTriggered(boolean v) { this.schedulerTriggered = v; return this; }
        public Builder realCallRequested(boolean v) { this.realCallRequested = v; return this; }
        public Builder targetCount(int v) { this.targetCount = v; return this; }
        public Builder operationSummary(String v) { this.operationSummary = v; return this; }
        public SetTimeExecutionRequest build() { return new SetTimeExecutionRequest(this); }
    }
}
