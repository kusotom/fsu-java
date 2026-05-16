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

    private LoginResult(boolean success, String resultCode, String resultDesc,
                        String fsuCode, String sessionId, Instant loginTime,
                        List<String> errors) {
        this.success = success;
        this.resultCode = resultCode;
        this.resultDesc = resultDesc;
        this.fsuCode = fsuCode;
        this.sessionId = sessionId;
        this.loginTime = loginTime;
        this.errors = errors;
    }

    // ==================== 工厂方法 ====================

    public static LoginResult success(String fsuCode, String sessionId, Instant loginTime) {
        return new LoginResult(true, "0", "登录成功", fsuCode, sessionId, loginTime, List.of());
    }

    public static LoginResult fail(String resultCode, String desc) {
        return new LoginResult(false, resultCode, desc, null, null, null,
                List.of(desc));
    }

    public static LoginResult fail(String resultCode, String desc, String fsuCode) {
        return new LoginResult(false, resultCode, desc, fsuCode, null, null,
                List.of(desc));
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

    @Override
    public String toString() {
        return "LoginResult{success=" + success + ", resultCode=" + resultCode
                + ", fsuCode=" + fsuCode + ", sessionId=" + sessionId + "}";
    }
}
