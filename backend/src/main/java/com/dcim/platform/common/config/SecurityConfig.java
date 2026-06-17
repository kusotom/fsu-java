package com.dcim.platform.common.security;

import com.dcim.platform.common.security.audit.AuditLogService;
import com.dcim.platform.module.auth.service.TokenStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * BE-AUTH-P0-001: 安全配置 — 注册 Filter 和 Interceptor.
 */
@Configuration
public class SecurityConfig implements WebMvcConfigurer {

    @Autowired
    private TokenStore tokenStore;

    @Autowired
    private AuditLogService auditLogService;

    @Bean
    public FilterRegistrationBean<SecurityContextFilter> securityContextFilter() {
        FilterRegistrationBean<SecurityContextFilter> bean = new FilterRegistrationBean<>();
        bean.setFilter(new SecurityContextFilter(tokenStore));
        bean.addUrlPatterns("/api/*");
        bean.setOrder(1);
        return bean;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new AuthInterceptor(auditLogService))
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/auth/**", "/api/health", "/api/b-interface/health",
                        "/api/b-interface/sc-service", "/services/**");
    }
}
