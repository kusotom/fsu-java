package com.dcim.platform.module.binterface.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * SET_TIME / SET_TIME_ACK 时间同步结果 (2024 标准, Code=901)。
 *
 * <p>BIF-P4-014/SAFE-002: 扩展安全门禁字段。</p>
 */
public class SetTimeResult {

    private final boolean success;
    private final String resultCode;
    private final String resultDesc;
    private final String suid;
    private final LocalDateTime sentTime;
    private final boolean dryRun;
    private final String safetyReasonCode;
    private final String safetyReasonMessage;
    private final List<String> errors;

    private SetTimeResult(boolean success, String resultCode, String resultDesc,
                          String suid, LocalDateTime sentTime, boolean dryRun,
                          String safetyReasonCode, String safetyReasonMessage,
                          List<String> errors) {
        this.success = success;
        this.resultCode = resultCode;
        this.resultDesc = resultDesc;
        this.suid = suid;
        this.sentTime = sentTime;
        this.dryRun = dryRun;
        this.safetyReasonCode = safetyReasonCode;
        this.safetyReasonMessage = safetyReasonMessage;
        this.errors = errors != null ? List.copyOf(errors) : List.of();
    }

    public static SetTimeResult success(String suid, LocalDateTime sentTime) {
        return new SetTimeResult(true, "0", "OK", suid, sentTime, false, null, null, List.of());
    }

    public static SetTimeResult dryRunSuccess(String suid, LocalDateTime sentTime) {
        return new SetTimeResult(true, "0", "OK (dry-run)", suid, sentTime, true, "DRY_RUN", "dry-run 模式，未真实下发", List.of());
    }

    public static SetTimeResult rejected(String reasonCode, String reasonMessage, String suid) {
        List<String> errs = new ArrayList<>();
        errs.add(reasonMessage);
        return new SetTimeResult(false, "5001", "安全门禁拒绝: " + reasonCode, suid, null, false, reasonCode, reasonMessage, errs);
    }

    public static SetTimeResult fail(String resultCode, String desc) {
        return new SetTimeResult(false, resultCode, desc, null, null, false, null, null, List.of(desc));
    }

    public static SetTimeResult fail(String resultCode, String desc, String suid) {
        return new SetTimeResult(false, resultCode, desc, suid, null, false, null, null, List.of(desc));
    }

    public boolean isSuccess() { return success; }
    public String getResultCode() { return resultCode; }
    public String getResultDesc() { return resultDesc; }
    public String getSuid() { return suid; }
    public LocalDateTime getSentTime() { return sentTime; }
    public boolean isDryRun() { return dryRun; }
    public String getSafetyReasonCode() { return safetyReasonCode; }
    public String getSafetyReasonMessage() { return safetyReasonMessage; }
    public List<String> getErrors() { return errors; }
    public boolean hasErrors() { return !errors.isEmpty(); }

    @Override
    public String toString() {
        return "SetTimeResult{success=" + success + ", suid=" + suid
                + ", sentTime=" + sentTime + ", dryRun=" + dryRun + "}";
    }
}
