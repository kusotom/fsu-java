package com.dcim.platform.module.binterface.service;

import java.util.Collections;
import java.util.List;

/**
 * GET_SPCONFIGOPTION 监控点配置模板查询结果 (2024 标准, Code=401)。
 *
 * <p>BIF-P4-015: 只读查询，不修改 FSU 配置。GET_THRESHOLD 为旧兼容路径。</p>
 */
public class GetSpConfigOptionResult {

    private final boolean success;
    private final String resultCode;
    private final String resultDesc;
    private final String suid;
    private final List<String> errors;

    private GetSpConfigOptionResult(boolean success, String resultCode, String resultDesc,
                                     String suid, List<String> errors) {
        this.success = success;
        this.resultCode = resultCode;
        this.resultDesc = resultDesc;
        this.suid = suid;
        this.errors = errors != null ? List.copyOf(errors) : List.of();
    }

    public static GetSpConfigOptionResult success(String suid) {
        return new GetSpConfigOptionResult(true, "0", "OK", suid, List.of());
    }

    public static GetSpConfigOptionResult fail(String resultCode, String desc) {
        return new GetSpConfigOptionResult(false, resultCode, desc, null, List.of(desc));
    }

    public static GetSpConfigOptionResult fail(String resultCode, String desc, String suid) {
        return new GetSpConfigOptionResult(false, resultCode, desc, suid, List.of(desc));
    }

    public boolean isSuccess() { return success; }
    public String getResultCode() { return resultCode; }
    public String getResultDesc() { return resultDesc; }
    public String getSuid() { return suid; }
    public List<String> getErrors() { return errors; }
    public boolean hasErrors() { return !errors.isEmpty(); }

    @Override
    public String toString() {
        return "GetSpConfigOptionResult{success=" + success + ", suid=" + suid + "}";
    }
}
