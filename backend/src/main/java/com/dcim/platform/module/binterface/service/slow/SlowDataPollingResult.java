package com.dcim.platform.module.binterface.service.slow;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SlowDataPollingResult {

    private final boolean success;
    private final int scannedFsuCount;
    private final int skippedOfflineCount;
    private final int skippedNotLoggedInCount;
    private final int skippedNoSignalCount;
    private final int getDataSuccessCount;
    private final int getDataFailureCount;
    private final int getThresholdSuccessCount;
    private final int getThresholdFailureCount;
    private final List<String> processedFsuCodes;
    private final List<String> errors;
    private final boolean realCallEnabled;

    private SlowDataPollingResult(boolean success, int scannedFsuCount,
                                  int skippedOfflineCount, int skippedNotLoggedInCount,
                                  int skippedNoSignalCount,
                                  int getDataSuccessCount, int getDataFailureCount,
                                  int getThresholdSuccessCount, int getThresholdFailureCount,
                                  List<String> processedFsuCodes, List<String> errors,
                                  boolean realCallEnabled) {
        this.success = success;
        this.scannedFsuCount = scannedFsuCount;
        this.skippedOfflineCount = skippedOfflineCount;
        this.skippedNotLoggedInCount = skippedNotLoggedInCount;
        this.skippedNoSignalCount = skippedNoSignalCount;
        this.getDataSuccessCount = getDataSuccessCount;
        this.getDataFailureCount = getDataFailureCount;
        this.getThresholdSuccessCount = getThresholdSuccessCount;
        this.getThresholdFailureCount = getThresholdFailureCount;
        this.processedFsuCodes = Collections.unmodifiableList(new ArrayList<>(processedFsuCodes));
        this.errors = Collections.unmodifiableList(new ArrayList<>(errors));
        this.realCallEnabled = realCallEnabled;
    }

    public static SlowDataPollingResult completed(int scannedFsuCount,
                                                   int skippedOfflineCount, int skippedNotLoggedInCount,
                                                   int skippedNoSignalCount,
                                                   int getDataSuccessCount, int getDataFailureCount,
                                                   int getThresholdSuccessCount, int getThresholdFailureCount,
                                                   List<String> processedFsuCodes, List<String> errors,
                                                   boolean realCallEnabled) {
        boolean overallSuccess = getDataFailureCount == 0 && getThresholdFailureCount == 0
                && errors.isEmpty();
        return new SlowDataPollingResult(overallSuccess, scannedFsuCount,
                skippedOfflineCount, skippedNotLoggedInCount, skippedNoSignalCount,
                getDataSuccessCount, getDataFailureCount,
                getThresholdSuccessCount, getThresholdFailureCount,
                processedFsuCodes, errors, realCallEnabled);
    }

    // ==================== Getters ====================

    public boolean isSuccess() { return success; }
    public int getScannedFsuCount() { return scannedFsuCount; }
    public int getSkippedOfflineCount() { return skippedOfflineCount; }
    public int getSkippedNotLoggedInCount() { return skippedNotLoggedInCount; }
    public int getSkippedNoSignalCount() { return skippedNoSignalCount; }
    public int getGetDataSuccessCount() { return getDataSuccessCount; }
    public int getGetDataFailureCount() { return getDataFailureCount; }
    public int getGetThresholdSuccessCount() { return getThresholdSuccessCount; }
    public int getGetThresholdFailureCount() { return getThresholdFailureCount; }
    public List<String> getProcessedFsuCodes() { return processedFsuCodes; }
    public List<String> getErrors() { return errors; }
    public boolean isRealCallEnabled() { return realCallEnabled; }

    @Override
    public String toString() {
        return "SlowDataPollingResult{success=" + success
                + ", scanned=" + scannedFsuCount
                + ", skippedOffline=" + skippedOfflineCount
                + ", skippedNotLoggedIn=" + skippedNotLoggedInCount
                + ", skippedNoSignal=" + skippedNoSignalCount
                + ", getDataOK=" + getDataSuccessCount
                + ", getDataFail=" + getDataFailureCount
                + ", getThresholdOK=" + getThresholdSuccessCount
                + ", getThresholdFail=" + getThresholdFailureCount
                + ", errors=" + errors.size() + "}";
    }
}
