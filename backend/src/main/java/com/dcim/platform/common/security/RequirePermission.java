package com.dcim.platform.common.security;

import java.lang.annotation.*;

/**
 * BE-AUTH-P0-001: 后端最小权限控制注解.
 * 标记在 Controller 类或方法上，由 AuthInterceptor 在 preHandle 中检查.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequirePermission {

    /** 所需权限点列表 (OR 语义, 满足一个即放行) */
    String[] value() default {};

    /** 所需角色列表 (OR 语义) */
    String[] roles() default {};

    /** true = 所有权限点必须全部满足 (AND 语义) */
    boolean requireAll() default false;
}
