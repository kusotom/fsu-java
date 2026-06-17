package com.dcim.platform.common.security;

import com.dcim.platform.common.security.audit.AuditLogService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Arrays;
import java.util.List;

/**
 * BE-AUTH-P0-001: 权限检查 Interceptor.
 * 检查 @RequirePermission 注解，验证当前用户是否有足够权限.
 */
public class AuthInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(AuthInterceptor.class);

    private final AuditLogService auditLogService;

    public AuthInterceptor(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    /**
     * BE-AUTH-P0-FIX-001:
     * admin/super_admin/platform_admin (isAdminLike) 可绕过普通业务权限检查.
     * 但对敏感权限 (raw:* / runonce:* / set:*), 仅 super_admin 可绕过.
     */
    private boolean canBypassPermissionCheck(RequestContext ctx, String[] perms) {
        if (ctx.isSuperAdmin()) return true; // super_admin 可绕过全部
        if (RequestContext.containsSensitivePermission(perms)) return false; // 敏感权限不允许 admin/platform_admin 绕过
        return ctx.isAdminLike(); // 普通权限允许 admin-like 绕过
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod hm)) return true;

        // 方法级注解优先，否则看类级
        RequirePermission methodAnn = hm.getMethodAnnotation(RequirePermission.class);
        RequirePermission classAnn = hm.getBeanType().getAnnotation(RequirePermission.class);
        RequirePermission ann = methodAnn != null ? methodAnn : classAnn;

        if (ann == null) return true; // 无需权限，放行

        RequestContext ctx = RequestContext.getCurrent();
        if (ctx == null) {
            // 未认证
            auditLogService.logPermissionDenied(null, null, request.getRequestURI(),
                    String.join(",", ann.value()), "未认证");
            throw new com.dcim.platform.common.exception.UnauthorizedException("请先登录");
        }

        // 检查角色
        String[] roles = ann.roles();
        if (roles.length > 0) {
            boolean hasRole = Arrays.stream(roles).anyMatch(ctx::hasRole);
            if (!hasRole && !canBypassPermissionCheck(ctx, ann.value())) {
                auditLogService.logPermissionDenied(ctx.getUserId(), ctx.getUsername(),
                        request.getRequestURI(), String.join(",", ann.value()),
                        "角色不满足: 需要 " + Arrays.toString(roles));
                throw new com.dcim.platform.common.exception.ForbiddenException("无权限访问", String.join(",", ann.value()));
            }
        }

        // 检查权限点
        String[] perms = ann.value();
        if (perms.length > 0) {
            boolean hasPerm;
            if (ann.requireAll()) {
                hasPerm = Arrays.stream(perms).allMatch(ctx::hasPermission);
            } else {
                hasPerm = Arrays.stream(perms).anyMatch(ctx::hasPermission);
            }
            if (!hasPerm && !canBypassPermissionCheck(ctx, perms)) {
                auditLogService.logPermissionDenied(ctx.getUserId(), ctx.getUsername(),
                        request.getRequestURI(), String.join(",", perms),
                        ann.requireAll() ? "缺少全部所需权限" : "缺少所需权限之一");
                throw new com.dcim.platform.common.exception.ForbiddenException("无权限访问", String.join(",", perms));
            }
        }

        return true;
    }
}
