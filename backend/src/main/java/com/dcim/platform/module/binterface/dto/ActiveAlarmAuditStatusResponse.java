package com.dcim.platform.module.binterface.dto;

/**
 * 活动告警审计状态响应 DTO (BIF-P4-022, BIF-P4-023)。
 *
 * <p>只读快照，不触发审计、不访问 FSU、不修改 alarm_record。</p>
 */
public class ActiveAlarmAuditStatusResponse {

    // 配置摘要
    private final boolean schedulerEnabled;
    private final String configuredFsuCode;
    private final long fixedDelayMs;
    private final long initialDelayMs;

    // 最近一次运行状态
    private final Long lastRunTime;
    private final boolean lastSuccess;
    private final String lastError;
    private final String dataSource;

    // 最近一次审计结果
    private final boolean realDeviceAccessed;
    private final int fsuAlarmCount;
    private final int platformAlarmCount;
    private final int matchedCount;
    private final int missingInPlatformCount;
    private final int extraInPlatformCount;
    private final int mismatchedCount;
    private final String lastResult;

    private ActiveAlarmAuditStatusResponse(boolean schedulerEnabled, String configuredFsuCode,
                                           long fixedDelayMs, long initialDelayMs,
                                           Long lastRunTime, boolean lastSuccess, String lastError,
                                           String dataSource,
                                           boolean realDeviceAccessed, int fsuAlarmCount,
                                           int platformAlarmCount, int matchedCount,
                                           int missingInPlatformCount, int extraInPlatformCount,
                                           int mismatchedCount, String lastResult) {
        this.schedulerEnabled = schedulerEnabled;
        this.configuredFsuCode = configuredFsuCode;
        this.fixedDelayMs = fixedDelayMs;
        this.initialDelayMs = initialDelayMs;
        this.lastRunTime = lastRunTime;
        this.lastSuccess = lastSuccess;
        this.lastError = lastError;
        this.dataSource = dataSource;
        this.realDeviceAccessed = realDeviceAccessed;
        this.fsuAlarmCount = fsuAlarmCount;
        this.platformAlarmCount = platformAlarmCount;
        this.matchedCount = matchedCount;
        this.missingInPlatformCount = missingInPlatformCount;
        this.extraInPlatformCount = extraInPlatformCount;
        this.mismatchedCount = mismatchedCount;
        this.lastResult = lastResult;
    }

    public static Builder builder() { return new Builder(); }

    public boolean isSchedulerEnabled() { return schedulerEnabled; }
    public String getConfiguredFsuCode() { return configuredFsuCode; }
    public long getFixedDelayMs() { return fixedDelayMs; }
    public long getInitialDelayMs() { return initialDelayMs; }
    public Long getLastRunTime() { return lastRunTime; }
    public boolean isLastSuccess() { return lastSuccess; }
    public String getLastError() { return lastError; }
    public String getDataSource() { return dataSource; }
    public boolean isRealDeviceAccessed() { return realDeviceAccessed; }
    public int getFsuAlarmCount() { return fsuAlarmCount; }
    public int getPlatformAlarmCount() { return platformAlarmCount; }
    public int getMatchedCount() { return matchedCount; }
    public int getMissingInPlatformCount() { return missingInPlatformCount; }
    public int getExtraInPlatformCount() { return extraInPlatformCount; }
    public int getMismatchedCount() { return mismatchedCount; }
    public String getLastResult() { return lastResult; }

    public static class Builder {
        private boolean schedulerEnabled;
        private String configuredFsuCode;
        private long fixedDelayMs;
        private long initialDelayMs;
        private Long lastRunTime;
        private boolean lastSuccess;
        private String lastError;
        private String dataSource;
        private boolean realDeviceAccessed;
        private int fsuAlarmCount;
        private int platformAlarmCount;
        private int matchedCount;
        private int missingInPlatformCount;
        private int extraInPlatformCount;
        private int mismatchedCount;
        private String lastResult;

        public Builder schedulerEnabled(boolean v) { schedulerEnabled = v; return this; }
        public Builder configuredFsuCode(String v) { configuredFsuCode = v; return this; }
        public Builder fixedDelayMs(long v) { fixedDelayMs = v; return this; }
        public Builder initialDelayMs(long v) { initialDelayMs = v; return this; }
        public Builder lastRunTime(Long v) { lastRunTime = v; return this; }
        public Builder lastSuccess(boolean v) { lastSuccess = v; return this; }
        public Builder lastError(String v) { lastError = v; return this; }
        public Builder dataSource(String v) { dataSource = v; return this; }
        public Builder realDeviceAccessed(boolean v) { realDeviceAccessed = v; return this; }
        public Builder fsuAlarmCount(int v) { fsuAlarmCount = v; return this; }
        public Builder platformAlarmCount(int v) { platformAlarmCount = v; return this; }
        public Builder matchedCount(int v) { matchedCount = v; return this; }
        public Builder missingInPlatformCount(int v) { missingInPlatformCount = v; return this; }
        public Builder extraInPlatformCount(int v) { extraInPlatformCount = v; return this; }
        public Builder mismatchedCount(int v) { mismatchedCount = v; return this; }
        public Builder lastResult(String v) { lastResult = v; return this; }

        public ActiveAlarmAuditStatusResponse build() {
            return new ActiveAlarmAuditStatusResponse(
                    schedulerEnabled, configuredFsuCode, fixedDelayMs, initialDelayMs,
                    lastRunTime, lastSuccess, lastError, dataSource, realDeviceAccessed,
                    fsuAlarmCount, platformAlarmCount, matchedCount,
                    missingInPlatformCount, extraInPlatformCount, mismatchedCount, lastResult);
        }
    }
}
