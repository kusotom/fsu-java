package com.dcim.platform.module.binterface.service;

import java.util.List;

/**
 * GET_FTP FTP 配置查询结果。
 *
 * <p>由 GetFtpService 返回，包含从 FSU 查询到的 FTP 配置参数。</p>
 *
 * <p>敏感字段说明：Host/Port/Username 可能涉及设备网络信息，
 * 不应直接暴露到前端或日志。Username 在 toString 中不输出。</p>
 *
 * <p>与 SET_FTP 无关。GET_FTP 是只读查询，不修改 FSU 的 FTP 配置。</p>
 */
public class GetFtpResult {

    private final boolean success;
    private final String resultCode;
    private final String resultDesc;
    private final String fsuCode;
    private final String host;
    private final int port;
    private final String username;
    private final boolean passiveMode;
    private final String basePath;
    private final List<String> errors;

    private GetFtpResult(boolean success, String resultCode, String resultDesc,
                          String fsuCode, String host, int port, String username,
                          boolean passiveMode, String basePath, List<String> errors) {
        this.success = success;
        this.resultCode = resultCode;
        this.resultDesc = resultDesc;
        this.fsuCode = fsuCode;
        this.host = host;
        this.port = port;
        this.username = username;
        this.passiveMode = passiveMode;
        this.basePath = basePath;
        this.errors = errors;
    }

    // ==================== 工厂方法 ====================

    public static GetFtpResult success(String fsuCode, String host, int port, String username,
                                        boolean passiveMode, String basePath) {
        return new GetFtpResult(true, "0", "查询成功",
                fsuCode, host, port, username, passiveMode, basePath, List.of());
    }

    public static GetFtpResult fail(String resultCode, String desc) {
        return new GetFtpResult(false, resultCode, desc,
                null, null, 0, null, false, null, List.of(desc));
    }

    public static GetFtpResult fail(String resultCode, String desc, String fsuCode) {
        return new GetFtpResult(false, resultCode, desc,
                fsuCode, null, 0, null, false, null, List.of(desc));
    }

    // ==================== Getters ====================

    public boolean isSuccess() { return success; }
    public String getResultCode() { return resultCode; }
    public String getResultDesc() { return resultDesc; }
    public String getFsuCode() { return fsuCode; }
    public String getHost() { return host; }
    public int getPort() { return port; }
    public String getUsername() { return username; }
    public boolean isPassiveMode() { return passiveMode; }
    public String getBasePath() { return basePath; }
    public List<String> getErrors() { return errors; }
    public boolean hasErrors() { return !errors.isEmpty(); }

    /**
     * 获取脱敏后的用户名（仅用于日志输出）。
     */
    public String getMaskedUsername() {
        if (username == null) return null;
        if (username.length() <= 2) return "****";
        return username.substring(0, 1) + "****" + username.substring(username.length() - 1);
    }

    @Override
    public String toString() {
        return "GetFtpResult{success=" + success + ", resultCode=" + resultCode
                + ", fsuCode=" + fsuCode + ", host=" + host
                + ", port=" + port + ", username=" + getMaskedUsername() + "}";
    }
}
