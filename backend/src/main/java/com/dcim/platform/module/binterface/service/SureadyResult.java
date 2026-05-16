package com.dcim.platform.module.binterface.service;

import java.util.List;

/**
 * SUREADY / SUREADY_ACK 注册状态验证结果 (2024 标准, Code=103)。
 */
public class SureadyResult {

    private final boolean success;
    private final String resultCode;
    private final String resultDesc;
    private final String suid;
    private final boolean registered;
    private final List<String> errors;

    private SureadyResult(boolean success, String resultCode, String resultDesc,
                           String suid, boolean registered, List<String> errors) {
        this.success = success;
        this.resultCode = resultCode;
        this.resultDesc = resultDesc;
        this.suid = suid;
        this.registered = registered;
        this.errors = errors != null ? List.copyOf(errors) : List.of();
    }

    public static SureadyResult ready(String suid) {
        return new SureadyResult(true, "0", "OK", suid, true, List.of());
    }

    public static SureadyResult fail(String resultCode, String desc) {
        return new SureadyResult(false, resultCode, desc, null, false, List.of(desc));
    }

    public static SureadyResult fail(String resultCode, String desc, String suid) {
        return new SureadyResult(false, resultCode, desc, suid, false, List.of(desc));
    }

    public boolean isSuccess() { return success; }
    public String getResultCode() { return resultCode; }
    public String getResultDesc() { return resultDesc; }
    public String getSuid() { return suid; }
    public boolean isRegistered() { return registered; }
    public List<String> getErrors() { return errors; }
    public boolean hasErrors() { return !errors.isEmpty(); }

    @Override
    public String toString() {
        return "SureadyResult{success=" + success + ", suid=" + suid + ", registered=" + registered + "}";
    }
}
