package com.dcim.platform.module.binterface.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 离线检测结果模型。
 *
 * <p>记录一次扫描中已扫描 FSU 总数、被置为 OFFLINE 的 FSU 列表、
 * 仍为 ONLINE 的 FSU 列表以及处理过程中的错误信息。</p>
 */
public class OfflineDetectionResult {

    private final int scannedCount;
    private final List<String> offlineFsuCodes;
    private final List<String> onlineFsuCodes;
    private final List<String> errors;

    private OfflineDetectionResult(int scannedCount,
                                   List<String> offlineFsuCodes,
                                   List<String> onlineFsuCodes,
                                   List<String> errors) {
        this.scannedCount = scannedCount;
        this.offlineFsuCodes = Collections.unmodifiableList(new ArrayList<>(offlineFsuCodes));
        this.onlineFsuCodes = Collections.unmodifiableList(new ArrayList<>(onlineFsuCodes));
        this.errors = Collections.unmodifiableList(new ArrayList<>(errors));
    }

    public static OfflineDetectionResult completed(int scannedCount,
                                                    List<String> offlineFsuCodes,
                                                    List<String> onlineFsuCodes) {
        return new OfflineDetectionResult(scannedCount, offlineFsuCodes, onlineFsuCodes, List.of());
    }

    public static OfflineDetectionResult completedWithErrors(int scannedCount,
                                                              List<String> offlineFsuCodes,
                                                              List<String> onlineFsuCodes,
                                                              List<String> errors) {
        return new OfflineDetectionResult(scannedCount, offlineFsuCodes, onlineFsuCodes, errors);
    }

    public static OfflineDetectionResult empty() {
        return new OfflineDetectionResult(0, List.of(), List.of(), List.of());
    }

    // ==================== Getters ====================

    public int getScannedCount() {
        return scannedCount;
    }

    public int getOfflineCount() {
        return offlineFsuCodes.size();
    }

    public int getOnlineCount() {
        return onlineFsuCodes.size();
    }

    public int getErrorCount() {
        return errors.size();
    }

    public List<String> getOfflineFsuCodes() {
        return offlineFsuCodes;
    }

    public List<String> getOnlineFsuCodes() {
        return onlineFsuCodes;
    }

    public List<String> getErrors() {
        return errors;
    }

    public boolean hasError() {
        return !errors.isEmpty();
    }

    @Override
    public String toString() {
        return "OfflineDetectionResult{" +
                "scannedCount=" + scannedCount +
                ", offlineCount=" + getOfflineCount() +
                ", onlineCount=" + getOnlineCount() +
                ", errors=" + errors +
                '}';
    }
}
