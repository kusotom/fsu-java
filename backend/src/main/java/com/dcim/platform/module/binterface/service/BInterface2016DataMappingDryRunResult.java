package com.dcim.platform.module.binterface.service;

import java.util.ArrayList;
import java.util.List;

/**
 * DATA-MAPPING-006: GET_DATA TSemaphore → monitoring_point dry-run 映射结果。
 * 只做匹配分析，不写库。
 */
public class BInterface2016DataMappingDryRunResult {

    private final String fsuCode;
    private final int totalPoints;
    private final List<MappingEntry> entries = new ArrayList<>();
    private int matchedCount;
    private int unmatchedCount;
    private int ambiguousCount;

    public BInterface2016DataMappingDryRunResult(String fsuCode, int totalPoints) {
        this.fsuCode = fsuCode;
        this.totalPoints = totalPoints;
    }

    public void addMatched(String deviceId, String signalId, String value, Long pointId, String matchRule) {
        entries.add(new MappingEntry(deviceId, signalId, value, "matched", pointId, "low", matchRule));
        matchedCount++;
    }

    public void addUnmatched(String deviceId, String signalId, String value, String reason) {
        entries.add(new MappingEntry(deviceId, signalId, value, "unmatched", null, "high", reason));
        unmatchedCount++;
    }

    public void addAmbiguous(String deviceId, String signalId, String value, List<Long> candidatePointIds, String reason) {
        entries.add(new MappingEntry(deviceId, signalId, value, "ambiguous",
                candidatePointIds != null && !candidatePointIds.isEmpty() ? candidatePointIds.get(0) : null,
                "high", reason));
        ambiguousCount++;
    }

    public boolean hasAnyMatched() { return matchedCount > 0; }
    public String getFsuCode() { return fsuCode; }
    public int getTotalPoints() { return totalPoints; }
    public int getMatchedCount() { return matchedCount; }
    public int getUnmatchedCount() { return unmatchedCount; }
    public int getAmbiguousCount() { return ambiguousCount; }
    public List<MappingEntry> getEntries() { return List.copyOf(entries); }

    public record MappingEntry(String deviceId, String signalId, String value,
                                String status, Long pointId, String risk, String note) {}
}
