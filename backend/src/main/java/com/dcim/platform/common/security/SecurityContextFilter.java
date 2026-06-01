package com.dcim.platform.common.security;

import com.dcim.platform.module.auth.service.TokenStore;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

/**
 * BE-AUTH-P0-001: Token 提取 Filter.
 * 从 Authorization header 提取 Bearer token，查 TokenStore 获取用户信息，注入 RequestContext.
 * 无 token 时 RequestContext 为 null (anonymous).
 */
public class SecurityContextFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(SecurityContextFilter.class);

    private final TokenStore tokenStore;

    public SecurityContextFilter(TokenStore tokenStore) {
        this.tokenStore = tokenStore;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        try {
            if (request instanceof HttpServletRequest httpReq) {
                String auth = httpReq.getHeader("Authorization");
                String token = extractToken(auth);
                if (token != null) {
                    TokenStore.TokenEntry entry = tokenStore.validateAndGet(token);
                    if (entry != null) {
                        // BE-AUTH-P0-FIX-001: scope 从 TokenEntry 获取, admin-like 用户由 DataScopeService.isAdminLike() 绕过
                        RequestContext ctx = new RequestContext(
                                entry.userId(), entry.username(),
                                null, // tenantId: 预留
                                entry.roles(), entry.permissions(),
                                entry.stationIds() != null ? entry.stationIds() : Collections.emptySet(),
                                entry.fsuCodes() != null ? entry.fsuCodes() : Collections.emptySet()
                        );
                        RequestContext.setCurrent(ctx);
                    }
                }
            }
            chain.doFilter(request, response);
        } finally {
            RequestContext.clear();
        }
    }

    private String extractToken(String auth) {
        if (auth != null && auth.startsWith("Bearer ")) {
            return auth.substring(7);
        }
        return null;
    }
}
