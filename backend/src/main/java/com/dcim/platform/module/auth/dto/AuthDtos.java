package com.dcim.platform.module.auth.dto;

import java.util.List;

/** Auth API 请求/响应 DTO (BACKEND-FE-API-001). */
public class AuthDtos {

    public static class SmsCodeSendReq { public String phone; public String captchaToken; }
    public static class SmsLoginReq { public String phone; public String smsCode; public String deviceId; }
    public static class RefreshReq { public String refreshToken; }
    public static class ChangePwdReq { public String oldPassword; public String newPassword; public String confirmPassword; }
    public static class ResetPwdReq { public String phone; public String smsCode; public String newPassword; public String confirmPassword; }

    public static class LoginData {
        public String accessToken; public String refreshToken; public String tokenType = "Bearer";
        public long expiresIn; public UserInfo user; public List<RoleInfo> roles; public List<String> permissions;
    }
    public static class UserInfo {
        public Long id; public String username; public String displayName; public String email; public String phone;
        public boolean enabled; public boolean locked; public String lastLoginAt; public String createdAt;
    }
    public static class RoleInfo {
        public Long id; public String code; public String name; public String description;
        public List<String> permissions; public boolean builtin; public boolean enabled;
    }
    public static class PermissionInfo {
        public String code; public String name; public String group; public String description; public String riskLevel;
    }
    public static class MeData { public UserInfo user; public List<RoleInfo> roles; public List<String> permissions; }
}
