package com.dcim.platform.module.binterface.service;

import java.util.List;

/**
 * GET_LOGININFO 登录信息查询结果。
 *
 * <p>由 GetLoginInfoService 返回，包含从 FSU 查询到的登录/在线信息。</p>
 *
 * <p>敏感字段说明：本结果不包含密码等敏感凭据。
 * 如果未来协议扩展需增加敏感字段，必须进行脱敏处理。</p>
 */
public class GetLoginInfoResult {

    private final boolean success;
    private final String resultCode;
    private final String resultDesc;
    private final String fsuCode;
    private final String loginStatus;
    private final String onlineStatus;
    private final String sessionId;
    private final String loginTime;
    private final String lastHeartbeat;
    private final List<String> errors;

    private GetLoginInfoResult(boolean success, String resultCode, String resultDesc,
                               String fsuCode, String loginStatus, String onlineStatus,
                               String sessionId, String loginTime, String lastHeartbeat,
                               List<String> errors) {
        this.success = success;
        this.resultCode = resultCode;
        this.resultDesc = resultDesc;
        this.fsuCode = fsuCode;
        this.loginStatus = loginStatus;
        this.onlineStatus = onlineStatus;
        this.sessionId = sessionId;
        this.loginTime = loginTime;
        this.lastHeartbeat = lastHeartbeat;
        this.errors = errors;
    }

    // ==================== 工厂方法 ====================

    public static GetLoginInfoResult success(String fsuCode, String loginStatus, String onlineStatus,
                                              String sessionId, String loginTime, String lastHeartbeat) {
        return new GetLoginInfoResult(true, "0", "查询成功",
                fsuCode, loginStatus, onlineStatus, sessionId, loginTime, lastHeartbeat, List.of());
    }

    public static GetLoginInfoResult fail(String resultCode, String desc) {
        return new GetLoginInfoResult(false, resultCode, desc,
                null, null, null, null, null, null, List.of(desc));
    }

    public static GetLoginInfoResult fail(String resultCode, String desc, String fsuCode) {
        return new GetLoginInfoResult(false, resultCode, desc,
                fsuCode, null, null, null, null, null, List.of(desc));
    }

    // ==================== Getters ====================

    public boolean isSuccess() { return success; }
    public String getResultCode() { return resultCode; }
    public String getResultDesc() { return resultDesc; }
    public String getFsuCode() { return fsuCode; }
    public String getLoginStatus() { return loginStatus; }
    public String getOnlineStatus() { return onlineStatus; }
    public String getSessionId() { return sessionId; }
    public String getLoginTime() { return loginTime; }
    public String getLastHeartbeat() { return lastHeartbeat; }
    public List<String> getErrors() { return errors; }
    public boolean hasErrors() { return !errors.isEmpty(); }

    @Override
    public String toString() {
        return "GetLoginInfoResult{success=" + success + ", resultCode=" + resultCode
                + ", fsuCode=" + fsuCode + ", loginStatus=" + loginStatus
                + ", onlineStatus=" + onlineStatus + "}";
    }
}
