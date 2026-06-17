package com.dcim.platform.module.auth.service;

import com.dcim.platform.module.auth.dto.AuthDtos.*;
import com.dcim.platform.module.system.entity.RoleEntity;
import com.dcim.platform.module.system.entity.UserAccountEntity;
import com.dcim.platform.module.system.repository.RoleRepository;
import com.dcim.platform.module.system.repository.UserAccountRepository;
import com.dcim.platform.module.system.repository.UserRoleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class AuthService {
    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private final UserAccountRepository userRepo; private final RoleRepository roleRepo;
    private final UserRoleRepository userRoleRepo; private final SmsCodeStore smsCodeStore;
    private final TokenStore tokenStore; private final SecureRandom random = new SecureRandom();

    public AuthService(UserAccountRepository ur, RoleRepository rr, UserRoleRepository urr, SmsCodeStore sms, TokenStore ts) {
        this.userRepo = ur; this.roleRepo = rr; this.userRoleRepo = urr; this.smsCodeStore = sms; this.tokenStore = ts;
    }

    public boolean sendSmsCode(String phone) { if (!smsCodeStore.canSend(phone)) return false; smsCodeStore.storeCode(phone, String.format("%06d", random.nextInt(1000000))); log.info("SMS code generated for phone={}", phone); return true; }

    public LoginData smsLogin(String phone, String smsCode) {
        if (!smsCodeStore.validateAndConsume(phone, smsCode)) return null;
        var user = userRepo.findByPhone(phone).orElse(null);
        if (user == null || !"ACTIVE".equals(user.getStatus())) return null;
        user.setLastLoginTime(LocalDateTime.now()); userRepo.save(user);
        var roles = userRoleRepo.findByUserId(user.getId()).stream().map(ur -> roleRepo.findById(ur.getRoleId()).orElse(null)).filter(Objects::nonNull).toList();
        List<String> roleCodes = roles.stream().map(RoleEntity::getRoleCode).toList();
        List<String> perms = roles.stream().flatMap(r -> parsePerms(r.getPermissions()).stream()).distinct().toList();
        var pair = tokenStore.createTokens(user.getId(), user.getUsername(), phone, roleCodes, perms,
                Collections.emptySet(), Collections.emptySet()); // BE-AUTH-P0-FIX-001: scope 预留, admin 通过 isAdminLike() 绕过 DataScope
        LoginData d = new LoginData(); d.accessToken = pair.accessToken(); d.refreshToken = pair.refreshToken(); d.expiresIn = 7200L;
        d.user = toUserInfo(user); d.roles = roles.stream().map(this::toRoleInfo).toList(); d.permissions = perms; d.tokenType = "Bearer";
        return d;
    }

    public MeData getMe(String accessToken) {
        var e = tokenStore.validateAndGet(accessToken); if (e == null) return null;
        var u = userRepo.findById(e.userId()).orElse(null); if (u == null) return null;
        MeData m = new MeData(); m.user = toUserInfo(u); m.permissions = e.permissions();
        m.roles = e.roles().stream().map(c -> { RoleInfo r = new RoleInfo(); r.code = c; r.name = c; return r; }).toList(); return m;
    }

    public void logout(String accessToken) { tokenStore.invalidate(accessToken); }
    public String refreshAccess(String refreshToken) { return tokenStore.refreshAccess(refreshToken); }

    public boolean changePassword(Long userId, String oldPwd, String newPwd) {
        var u = userRepo.findById(userId).orElse(null); if (u == null || !hash(oldPwd).equals(u.getPasswordHash())) return false;
        u.setPasswordHash(hash(newPwd)); userRepo.save(u); return true;
    }

    public boolean resetPassword(String phone, String smsCode, String newPwd) {
        if (!smsCodeStore.validateAndConsume(phone, smsCode)) return false;
        var u = userRepo.findByPhone(phone).orElse(null); if (u == null) return false;
        u.setPasswordHash(hash(newPwd)); userRepo.save(u); return true;
    }

    public List<RoleInfo> getRoles() { return roleRepo.findAll().stream().map(this::toRoleInfo).toList(); }
    public List<PermissionInfo> getPermissions() { return permList().stream().map(p -> { PermissionInfo pi = new PermissionInfo(); pi.code = p[0]; pi.name = p[0]; pi.group = p[1]; pi.riskLevel = "low"; return pi; }).toList(); }

    public List<Map<String, Object>> getRolePermissions() {
        List<Map<String, Object>> r = new ArrayList<>();
        for (var role : roleRepo.findAll()) { Map<String, Object> m = new LinkedHashMap<>(); m.put("roleCode", role.getRoleCode()); m.put("permissions", parsePerms(role.getPermissions())); r.add(m); }
        return r;
    }

    private UserInfo toUserInfo(UserAccountEntity u) {
        UserInfo ui = new UserInfo(); ui.id = u.getId(); ui.username = u.getUsername(); ui.displayName = u.getDisplayName();
        ui.email = u.getEmail(); ui.phone = u.getPhone(); ui.enabled = "ACTIVE".equals(u.getStatus()); ui.locked = false;
        ui.lastLoginAt = u.getLastLoginTime() != null ? u.getLastLoginTime().toString() : null; ui.createdAt = u.getCreatedAt() != null ? u.getCreatedAt().toString() : null; return ui;
    }

    private RoleInfo toRoleInfo(RoleEntity r) { RoleInfo ri = new RoleInfo(); ri.id = r.getId(); ri.code = r.getRoleCode(); ri.name = r.getRoleName(); ri.description = r.getDescription(); ri.permissions = parsePerms(r.getPermissions()); ri.builtin = true; ri.enabled = true; return ri; }

    private List<String> parsePerms(String s) { if (s == null || s.isBlank()) return List.of(); return Arrays.stream(s.split(",")).map(String::trim).filter(p -> !p.isEmpty()).toList(); }

    private List<String[]> permList() { return List.of(new String[]{"binterface.read","B接口查看"}, new String[]{"binterface.fsu.read","B接口查看"}, new String[]{"binterface.realtime.read","B接口查看"}, new String[]{"binterface.alarm.read","B接口查看"}, new String[]{"binterface.threshold.read","B接口查看"}, new String[]{"binterface.ftp.read","B接口查看"}, new String[]{"binterface.ftp_image.read","B接口查看"}, new String[]{"binterface.scheduler.read","B接口查看"}, new String[]{"binterface.protocol_audit.read","B接口查看"}, new String[]{"binterface.dry_run","B接口只读操作"}, new String[]{"binterface.mock","B接口只读操作"}, new String[]{"binterface.get.run","B接口只读操作"}, new String[]{"scheduler.read","调度"}, new String[]{"scheduler.manage","调度"}, new String[]{"user.read","系统管理"}, new String[]{"role.read","系统管理"}, new String[]{"permission.read","系统管理"}, new String[]{"audit.read","审计"}); }

    static String hash(String input) { try { MessageDigest md = MessageDigest.getInstance("SHA-256"); byte[] h = md.digest(input.getBytes()); StringBuilder sb = new StringBuilder(); for (byte b : h) sb.append(String.format("%02x", b)); return sb.toString(); } catch (Exception e) { throw new RuntimeException(e); } }
}
