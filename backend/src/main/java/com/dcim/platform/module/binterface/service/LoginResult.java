package com.dcim.platform.module.binterface.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * LOGIN 命令处理结果。
 *
 * 由 LoginService 返回，供 LoginCommandHandler 构造 CommandResult。
 */
public class LoginResult {

    private final boolean success;
    private final String resultCode;
    private final String resultDesc;
    private final String fsuCode;
    private final String sessionId;
    private final Instant loginTime;
    private final List<String> errors;

    // BIF2016-REGISTER-INTERVAL-001: 120s duplicate detection
    private final boolean duplicateWithin120s;
    private final long registerIntervalSeconds;
    private final String registerDecision; // ACCEPTED_NEW / ACCEPTED_DUPLICATE / ACCEPTED_REFRESH

    private LoginResult(boolean success, String resultCode, String resultDesc,
                        String fsuCode, String sessionId, Instant loginTime,
                        List<String> errors,
                        boolean duplicateWithin120s, long registerIntervalSeconds,
                        String registerDecision) {
        this.success = success;
        this.resultCode = resultCode;
        this.resultDesc = resultDesc;
        this.fsuCode = fsuCode;
        this.sessionId = sessionId;
        this.loginTime = loginTime;
        this.errors = errors;
        this.duplicateWithin120s = duplicateWithin120s;
        this.registerIntervalSeconds = registerIntervalSeconds;
        this.registerDecision = registerDecision;
    }

    // ==================== 工厂方法 ====================

    public static LoginResult success(String fsuCode, String sessionId, Instant loginTime) {
        return new LoginResult(true, "1", "登录成功", fsuCode, sessionId, loginTime, List.of(),
                false, 0, "ACCEPTED_NEW");
    }

    public static LoginResult successDuplicate(String fsuCode, String sessionId, Instant loginTime,
                                                long intervalSeconds) {
        return new LoginResult(true, "1", "登录成功(120s内重复)", fsuCode, sessionId, loginTime, List.of(),
                true, intervalSeconds, "ACCEPTED_DUPLICATE");
    }

    public static LoginResult successRefresh(String fsuCode, String sessionId, Instant loginTime,
                                              long intervalSeconds) {
        return new LoginResult(true, "1", "登录成功(≥120s刷新)", fsuCode, sessionId, loginTime, List.of(),
                false, intervalSeconds, "ACCEPTED_REFRESH");
    }

    public static LoginResult fail(String resultCode, String desc) {
        return new LoginResult(false, resultCode, desc, null, null, null,
                List.of(desc), false, 0, "REJECTED");
    }

    public static LoginResult fail(String resultCode, String desc, String fsuCode) {
        return new LoginResult(false, resultCode, desc, fsuCode, null, null,
                List.of(desc), false, 0, "REJECTED");
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

    public String getSessionId() {
        return sessionId;
    }

    public Instant getLoginTime() {
        return loginTime;
    }

    public List<String> getErrors() {
        return errors;
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    public boolean isDuplicateWithin120s() { return duplicateWithin120s; }
    public long getRegisterIntervalSeconds() { return registerIntervalSeconds; }
    public String getRegisterDecision() { return registerDecision; }

    @Override
    public String toString() {
        return "LoginResult{success=" + success + ", resultCode=" + resultCode
                + ", fsuCode=" + fsuCode + ", sessionId=" + sessionId + "}";
    }
}
