package com.dcim.platform.common.security;

import java.util.*;

/**
 * BE-AUTH-P0-001: 最小安全上下文 — ThreadLocal 持有当前请求用户信息.
 * 生产环境可替换为 Spring Security SecurityContextHolder.
 */
public final class RequestContext {

    private static final ThreadLocal<RequestContext> HOLDER = new ThreadLocal<>();

    private final Long userId;
    private final String username;
    private final String tenantId;
    private final List<String> roles;
    private final List<String> permissions;
    private final Set<Long> stationScope;
    private final Set<String> fsuScope;

    public RequestContext(Long userId, String username, String tenantId,
                          List<String> roles, List<String> permissions,
                          Set<Long> stationScope, Set<String> fsuScope) {
        this.userId = userId;
        this.username = username;
        this.tenantId = tenantId;
        this.roles = roles != null ? Collections.unmodifiableList(roles) : Collections.emptyList();
        this.permissions = permissions != null ? Collections.unmodifiableList(permissions) : Collections.emptyList();
        this.stationScope = stationScope != null ? Collections.unmodifiableSet(stationScope) : Collections.emptySet();
        this.fsuScope = fsuScope != null ? Collections.unmodifiableSet(fsuScope) : Collections.emptySet();
    }

    public static void setCurrent(RequestContext ctx) { HOLDER.set(ctx); }
    public static RequestContext getCurrent() { return HOLDER.get(); }
    public static void clear() { HOLDER.remove(); }

    public Long getUserId() { return userId; }
    public String getUsername() { return username; }
    public String getTenantId() { return tenantId; }
    public List<String> getRoles() { return roles; }
    public List<String> getPermissions() { return permissions; }
    public Set<Long> getStationScope() { return stationScope; }
    public Set<String> getFsuScope() { return fsuScope; }

    public boolean hasRole(String role) {
        return roles.stream().anyMatch(r -> r.equalsIgnoreCase(role));
    }

    public boolean hasPermission(String permission) {
        return permissions.contains(permission);
    }

    /** admin / super_admin / platform_admin 视为可查看全部数据 (DataScope) */
    public boolean isAdminLike() {
        return hasRole("admin") || hasRole("super_admin") || hasRole("platform_admin");
    }

    /** 仅 super_admin 可绕过敏感权限 (raw XML, run-once, SET) */
    public boolean isSuperAdmin() {
        return hasRole("super_admin");
    }

    /** BE-AUTH-P0-FIX-001: 敏感权限点列表 — admin/adminLike 不得绕过 */
    private static final Set<String> SENSITIVE_PERMISSIONS = Set.of(
        "protocol:raw:view",
        "protocol:raw:download",
        "protocol:runonce:readonly",
        "protocol:set:point",
        "protocol:set:threshold",
        "protocol:set:ftp",
        "protocol:set:logininfo",
        "protocol:set:fsureboot"
    );

    /** 检查是否所有请求的权限都是敏感权限 (普通admin不得绕过) */
    public static boolean isAllSensitivePermissions(String[] perms) {
        if (perms == null || perms.length == 0) return false;
        return java.util.Arrays.stream(perms).allMatch(SENSITIVE_PERMISSIONS::contains);
    }

    /** 检查任一请求的权限是否是敏感权限 */
    public static boolean containsSensitivePermission(String[] perms) {
        if (perms == null || perms.length == 0) return false;
        return java.util.Arrays.stream(perms).anyMatch(SENSITIVE_PERMISSIONS::contains);
    }
}
