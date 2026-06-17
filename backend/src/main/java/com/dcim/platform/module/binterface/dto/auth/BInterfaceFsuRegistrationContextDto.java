package com.dcim.platform.module.binterface.dto.auth;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * BIF2016-AUTH-002: FSU LOGIN 注册上下文只读模型。
 * 从 BInterfaceMessageLog (latest LOGIN raw message) + session/status 表合并构建。
 * 不写数据库。
 */
public class BInterfaceFsuRegistrationContextDto {

    // ── FSU 身份 ──
    private String fsuCode;
    private String fsuId;
    private String fsuIp;
    private String macId;
    private String version;

    // ── 认证 ──
    private String authUsername;
    private String authMode;       // strict-2016 | emerson-2016-compatible | unknown
    private String authStatus;     // AUTHENTICATED | COMPATIBLE | UNKNOWN

    // ── 会话 ──
    private String sessionId;
    private Integer expireSeconds;
    private String serverTime;

    // ── 状态 ──
    private String loginStatus;    // LOGIN | LOGOUT | REJECTED
    private String onlineStatus;   // ONLINE | OFFLINE
    private LocalDateTime lastLoginTime;

    // ── raw trace ──
    private Long rawLoginMessageId;
    private LocalDateTime rawLoginCreatedAt;

    // ── 设备能力 ──
    private List<DeviceCapabilityDto> deviceCapabilities = new ArrayList<>();

    // ── 完整性 ──
    private String contextSource;          // latest-login-message-log | session-status-merged | manual-probe
    private String contextCompleteness;    // COMPLETE | PARTIAL | MINIMAL | MISSING
    private List<String> missingFields = new ArrayList<>();

    // ── Getters / Setters ──
    public String getFsuCode() { return fsuCode; }
    public void setFsuCode(String v) { this.fsuCode = v; }
    public String getFsuId() { return fsuId; }
    public void setFsuId(String v) { this.fsuId = v; }
    public String getFsuIp() { return fsuIp; }
    public void setFsuIp(String v) { this.fsuIp = v; }
    public String getMacId() { return macId; }
    public void setMacId(String v) { this.macId = v; }
    public String getVersion() { return version; }
    public void setVersion(String v) { this.version = v; }
    public String getAuthUsername() { return authUsername; }
    public void setAuthUsername(String v) { this.authUsername = v; }
    public String getAuthMode() { return authMode; }
    public void setAuthMode(String v) { this.authMode = v; }
    public String getAuthStatus() { return authStatus; }
    public void setAuthStatus(String v) { this.authStatus = v; }
    public String getSessionId() { return sessionId; }
    public void setSessionId(String v) { this.sessionId = v; }
    public Integer getExpireSeconds() { return expireSeconds; }
    public void setExpireSeconds(Integer v) { this.expireSeconds = v; }
    public String getServerTime() { return serverTime; }
    public void setServerTime(String v) { this.serverTime = v; }
    public String getLoginStatus() { return loginStatus; }
    public void setLoginStatus(String v) { this.loginStatus = v; }
    public String getOnlineStatus() { return onlineStatus; }
    public void setOnlineStatus(String v) { this.onlineStatus = v; }
    public LocalDateTime getLastLoginTime() { return lastLoginTime; }
    public void setLastLoginTime(LocalDateTime v) { this.lastLoginTime = v; }
    public Long getRawLoginMessageId() { return rawLoginMessageId; }
    public void setRawLoginMessageId(Long v) { this.rawLoginMessageId = v; }
    public LocalDateTime getRawLoginCreatedAt() { return rawLoginCreatedAt; }
    public void setRawLoginCreatedAt(LocalDateTime v) { this.rawLoginCreatedAt = v; }
    public List<DeviceCapabilityDto> getDeviceCapabilities() { return deviceCapabilities; }
    public void setDeviceCapabilities(List<DeviceCapabilityDto> v) { this.deviceCapabilities = v; }
    public String getContextSource() { return contextSource; }
    public void setContextSource(String v) { this.contextSource = v; }
    public String getContextCompleteness() { return contextCompleteness; }
    public void setContextCompleteness(String v) { this.contextCompleteness = v; }
    public List<String> getMissingFields() { return missingFields; }
    public void setMissingFields(List<String> v) { this.missingFields = v; }

    // ── DeviceCapability ──
    public static class DeviceCapabilityDto {
        private String deviceId;
        private String deviceCode;
        private String deviceTypeCode;
        private String deviceTypeSource = "protocol-deviceid-inference";
        private String source = "LOGIN";
        private String confidence = "high";
        private boolean valid = true;
        private Map<String, String> rawAttributes = new LinkedHashMap<>();

        public String getDeviceId() { return deviceId; }
        public void setDeviceId(String v) { this.deviceId = v; }
        public String getDeviceCode() { return deviceCode; }
        public void setDeviceCode(String v) { this.deviceCode = v; }
        public String getDeviceTypeCode() { return deviceTypeCode; }
        public void setDeviceTypeCode(String v) { this.deviceTypeCode = v; }
        public String getDeviceTypeSource() { return deviceTypeSource; }
        public void setDeviceTypeSource(String v) { this.deviceTypeSource = v; }
        public String getSource() { return source; }
        public void setSource(String v) { this.source = v; }
        public String getConfidence() { return confidence; }
        public void setConfidence(String v) { this.confidence = v; }
        public boolean isValid() { return valid; }
        public void setValid(boolean v) { this.valid = v; }
        public Map<String, String> getRawAttributes() { return rawAttributes; }
        public void setRawAttributes(Map<String, String> v) { this.rawAttributes = v; }
    }
}
