package com.dcim.platform.module.binterface.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * GET_SUFTP / GET_SUFTP_ACK FTP 参数查询结果 (2024 标准)。
 *
 * <p>BIF-P4-013: SC 主动查询 FSU 的 FTP 配置参数 (Code=801)。</p>
 *
 * <p>敏感字段：Password 在 toString/log 中脱敏，不在文档中输出明文。</p>
 */
public class GetSuFtpResult {

    private final boolean success;
    private final String resultCode;
    private final String resultDesc;
    private final String suid;
    private final String userName;
    private final String password;
    private final Integer ftpPort;
    private final List<String> errors;

    private GetSuFtpResult(boolean success, String resultCode, String resultDesc,
                           String suid, String userName, String password, Integer ftpPort,
                           List<String> errors) {
        this.success = success;
        this.resultCode = resultCode;
        this.resultDesc = resultDesc;
        this.suid = suid;
        this.userName = userName;
        this.password = password;
        this.ftpPort = ftpPort;
        this.errors = errors != null ? Collections.unmodifiableList(new ArrayList<>(errors)) : List.of();
    }

    // ==================== 工厂方法 ====================

    public static GetSuFtpResult success(String suid, String userName, String password, Integer ftpPort) {
        return new GetSuFtpResult(true, "0", "OK", suid, userName, password, ftpPort, List.of());
    }

    public static GetSuFtpResult fail(String resultCode, String desc) {
        return new GetSuFtpResult(false, resultCode, desc, null, null, null, null, List.of(desc));
    }

    public static GetSuFtpResult fail(String resultCode, String desc, String suid) {
        return new GetSuFtpResult(false, resultCode, desc, suid, null, null, null, List.of(desc));
    }

    // ==================== Getters ====================

    public boolean isSuccess() { return success; }
    public String getResultCode() { return resultCode; }
    public String getResultDesc() { return resultDesc; }
    public String getSuid() { return suid; }
    public String getUserName() { return userName; }
    /** 原始密码（仅内部使用，禁止日志输出）。 */
    public String getPassword() { return password; }
    public Integer getFtpPort() { return ftpPort; }
    public List<String> getErrors() { return errors; }
    public boolean hasErrors() { return !errors.isEmpty(); }

    /** 脱敏后的密码。 */
    public String getMaskedPassword() {
        if (password == null) return null;
        if (password.length() <= 2) return "****";
        return password.substring(0, 1) + "****" + password.substring(password.length() - 1);
    }

    /** 脱敏后的用户名。 */
    public String getMaskedUserName() {
        if (userName == null) return null;
        if (userName.length() <= 2) return "****";
        return userName.substring(0, 1) + "****" + userName.substring(userName.length() - 1);
    }

    @Override
    public String toString() {
        return "GetSuFtpResult{success=" + success + ", suid=" + suid
                + ", userName=" + getMaskedUserName()
                + ", ftpPort=" + ftpPort + "}";
    }
}
