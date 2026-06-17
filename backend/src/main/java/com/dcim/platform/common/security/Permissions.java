package com.dcim.platform.common.security;

/**
 * BE-AUTH-P0-001: 后端权限点常量定义.
 * 与前端 permissions.ts 和 AuthService.permList() 对齐.
 */
public final class Permissions {

    private Permissions() {}

    // 基础查看
    public static final String DASHBOARD_VIEW = "dashboard:view";
    public static final String SITE_VIEW = "site:view";
    public static final String FSU_VIEW = "fsu:view";
    public static final String REALTIME_VIEW = "realtime:view";
    public static final String ALARM_VIEW = "alarm:view";

    // BE-AUTH-P0-FIX-002: 写操作权限 — view 不得覆盖 POST/PUT/DELETE
    public static final String SITE_CREATE = "site:create";
    public static final String SITE_UPDATE = "site:update";
    public static final String SITE_DELETE = "site:delete";
    public static final String FSU_CREATE = "fsu:create";
    public static final String FSU_UPDATE = "fsu:update";
    public static final String FSU_DELETE = "fsu:delete";
    public static final String USER_CREATE = "user:create";
    public static final String USER_UPDATE = "user:update";
    public static final String USER_DELETE = "user:delete";
    public static final String ROLE_CREATE = "role:create";
    public static final String ROLE_UPDATE = "role:update";
    public static final String ROLE_DELETE = "role:delete";
    public static final String PROTOCOL_RAW_CLEANUP = "protocol:raw:cleanup";

    // 协议敏感
    public static final String PROTOCOL_RAW_VIEW = "protocol:raw:view";
    public static final String PROTOCOL_RAW_DOWNLOAD = "protocol:raw:download";
    public static final String PROTOCOL_RUNONCE_READONLY = "protocol:runonce:readonly";

    // 系统管理
    public static final String USER_VIEW = "user:view";
    public static final String ROLE_VIEW = "role:view";
    public static final String PERMISSION_VIEW = "permission:view";
    public static final String TENANT_VIEW = "tenant:view";
    public static final String AUDIT_VIEW = "audit:view";

    // 高风险 SET 类 — 仅定义，不赋权给非管理员
    public static final String PROTOCOL_SET_POINT = "protocol:set:point";
    public static final String PROTOCOL_SET_THRESHOLD = "protocol:set:threshold";
    public static final String PROTOCOL_SET_FTP = "protocol:set:ftp";
    public static final String PROTOCOL_SET_LOGININFO = "protocol:set:logininfo";
    public static final String PROTOCOL_SET_FSUREBOOT = "protocol:set:fsureboot";
}
