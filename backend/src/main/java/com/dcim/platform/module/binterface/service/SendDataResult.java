package com.dcim.platform.module.binterface.service;

import java.util.ArrayList;
import java.util.List;

/**
 * SEND_DATA 命令处理结果。
 *
 * 由 SendDataService 返回，供 SendDataCommandHandler 构造 CommandResult。
 */
public class SendDataResult {

    private final boolean success;
    private final String resultCode;
    private final String resultDesc;
    private final String fsuCode;
    private final int acceptedCount;
    private final int rejectedCount;
    private final List<String> errors;

    private SendDataResult(boolean success, String resultCode, String resultDesc,
                           String fsuCode, int acceptedCount, int rejectedCount,
                           List<String> errors) {
        this.success = success;
        this.resultCode = resultCode;
        this.resultDesc = resultDesc;
        this.fsuCode = fsuCode;
        this.acceptedCount = acceptedCount;
        this.rejectedCount = rejectedCount;
        this.errors = errors;
    }

    // ==================== 工厂方法 ====================

    public static SendDataResult success(String fsuCode, int accepted, int rejected) {
        return new SendDataResult(true, "0", "数据上报成功",
                fsuCode, accepted, rejected, List.of());
    }

    public static SendDataResult fail(String resultCode, String desc) {
        return new SendDataResult(false, resultCode, desc,
                null, 0, 0, List.of(desc));
    }

    public static SendDataResult fail(String resultCode, String desc, String fsuCode) {
        return new SendDataResult(false, resultCode, desc,
                fsuCode, 0, 0, List.of(desc));
    }

    public static SendDataResult partial(String fsuCode, int accepted, int rejected, List<String> errors) {
        return new SendDataResult(true, "0", "数据上报完成（部分失败）",
                fsuCode, accepted, rejected, errors);
    }

    // ==================== Getter ====================

    public boolean isSuccess() {
        return success;
    }

    public String getResultCode() {
        return resultCode;
    }

    public String getResultDesc() {
        return resultDesc;
    }

    public String getFsuCode() {
        return fsuCode;
    }

    public int getAcceptedCount() {
        return acceptedCount;
    }

    public int getRejectedCount() {
        return rejectedCount;
    }

    public List<String> getErrors() {
        return errors;
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    @Override
    public String toString() {
        return "SendDataResult{success=" + success + ", resultCode=" + resultCode
                + ", fsuCode=" + fsuCode + ", accepted=" + acceptedCount
                + ", rejected=" + rejectedCount + "}";
    }
}
