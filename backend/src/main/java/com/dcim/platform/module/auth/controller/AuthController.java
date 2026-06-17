package com.dcim.platform.module.auth.controller;

import com.dcim.platform.common.response.ApiResponse;
import com.dcim.platform.module.auth.dto.AuthDtos.*;
import com.dcim.platform.module.auth.service.AuthService;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) { this.authService = authService; }

    @PostMapping("/sms/send-code")
    public ApiResponse<Map<String,Object>> sendSmsCode(@RequestBody SmsCodeSendReq req) {
        if (req.phone == null || req.phone.isBlank()) return ApiResponse.fail("手机号必填");
        boolean sent = authService.sendSmsCode(req.phone.trim());
        return sent ? ApiResponse.success(Map.of("sent", true, "expiresIn", 300)) : ApiResponse.fail("发送失败或频率限制");
    }

    @PostMapping("/sms/login")
    public ApiResponse<LoginData> smsLogin(@RequestBody SmsLoginReq req) {
        if (req.phone == null || req.smsCode == null) return ApiResponse.fail("手机号和验证码必填");
        LoginData data = authService.smsLogin(req.phone.trim(), req.smsCode.trim());
        return data != null ? ApiResponse.success(data) : ApiResponse.fail("验证码错误或已过期");
    }

    @GetMapping("/me")
    public ApiResponse<MeData> me(@RequestHeader(value = "Authorization", required = false) String auth) {
        String token = extractToken(auth);
        if (token == null) return ApiResponse.fail(401, "未登录");
        MeData data = authService.getMe(token);
        return data != null ? ApiResponse.success(data) : ApiResponse.fail(401, "Token 无效或已过期");
    }

    @PostMapping("/logout")
    public ApiResponse<Boolean> logout(@RequestHeader(value = "Authorization", required = false) String auth) {
        String token = extractToken(auth);
        if (token != null) authService.logout(token);
        return ApiResponse.success(true);
    }

    @PostMapping("/refresh")
    public ApiResponse<Map<String,String>> refresh(@RequestBody RefreshReq req) {
        if (req.refreshToken == null) return ApiResponse.fail("refreshToken 必填");
        String newAccess = authService.refreshAccess(req.refreshToken);
        return newAccess != null ? ApiResponse.success(Map.of("accessToken", newAccess, "tokenType", "Bearer", "expiresIn", "7200"))
                : ApiResponse.fail(401, "refreshToken 无效或已过期");
    }

    @PostMapping("/change-password")
    public ApiResponse<Boolean> changePassword(@RequestHeader(value = "Authorization", required = false) String auth, @RequestBody ChangePwdReq req) {
        String token = extractToken(auth);
        if (token == null) return ApiResponse.fail(401, "未登录");
        if (req.newPassword == null || req.newPassword.length() < 8) return ApiResponse.fail("密码至少8位");
        if (!req.newPassword.equals(req.confirmPassword)) return ApiResponse.fail("两次密码不一致");
        var me = authService.getMe(token); if (me == null || me.user == null) return ApiResponse.fail(401, "");
        boolean ok = authService.changePassword(me.user.id, req.oldPassword, req.newPassword);
        return ok ? ApiResponse.success(true) : ApiResponse.fail("旧密码错误");
    }

    @PostMapping("/password-reset/send-code")
    public ApiResponse<Map<String,Object>> sendResetCode(@RequestBody SmsCodeSendReq req) {
        if (req.phone == null || req.phone.isBlank()) return ApiResponse.fail("手机号必填");
        boolean sent = authService.sendSmsCode(req.phone.trim());
        return sent ? ApiResponse.success(Map.of("sent", true)) : ApiResponse.fail("发送失败");
    }

    @PostMapping("/password-reset/confirm")
    public ApiResponse<Boolean> resetPassword(@RequestBody ResetPwdReq req) {
        if (req.newPassword == null || req.newPassword.length() < 8) return ApiResponse.fail("密码至少8位");
        if (!req.newPassword.equals(req.confirmPassword)) return ApiResponse.fail("两次密码不一致");
        boolean ok = authService.resetPassword(req.phone, req.smsCode, req.newPassword);
        return ok ? ApiResponse.success(true) : ApiResponse.fail("验证码错误或已过期");
    }

    @GetMapping("/roles")
    public ApiResponse<List<RoleInfo>> getRoles() { return ApiResponse.success(authService.getRoles()); }

    @GetMapping("/permissions")
    public ApiResponse<List<PermissionInfo>> getPermissions() { return ApiResponse.success(authService.getPermissions()); }

    @GetMapping("/role-permissions")
    public ApiResponse<List<Map<String,Object>>> getRolePermissions() { return ApiResponse.success(authService.getRolePermissions()); }

    private String extractToken(String auth) { if (auth != null && auth.startsWith("Bearer ")) return auth.substring(7); return null; }
}
