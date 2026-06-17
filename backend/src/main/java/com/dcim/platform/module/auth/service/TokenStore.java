package com.dcim.platform.module.auth.service;

import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Token 存储 (BACKEND-FE-API-001 + BE-AUTH-P0-FIX-001, dev/单实例).
 * 生产环境建议替换为 Redis 或 JWT 自包含 Token。
 */
@Component
public class TokenStore {

    private static final long ACCESS_TTL_MS = 2 * 3600_000L;  // 2h
    private static final long REFRESH_TTL_MS = 7 * 86400_000L; // 7d

    private final Map<String, TokenEntry> tokens = new ConcurrentHashMap<>();

    /** BE-AUTH-P0-FIX-001: 增加 stationIds/fsuCodes 数据范围字段 */
    public record TokenEntry(Long userId, String username, String phone,
                             List<String> roles, List<String> permissions,
                             Set<Long> stationIds, Set<String> fsuCodes,
                             String type, long expiresAt) {}

    public record TokenPair(String accessToken, String refreshToken, TokenEntry entry) {}

    public TokenPair createTokens(Long userId, String username, String phone,
                                   List<String> roles, List<String> permissions,
                                   Set<Long> stationIds, Set<String> fsuCodes) {
        String accessToken = "at-" + UUID.randomUUID();
        String refreshToken = "rt-" + UUID.randomUUID();
        long now = System.currentTimeMillis();
        tokens.put(accessToken, new TokenEntry(userId, username, phone, roles, permissions,
                stationIds, fsuCodes, "ACCESS", now + ACCESS_TTL_MS));
        tokens.put(refreshToken, new TokenEntry(userId, username, phone, roles, permissions,
                stationIds, fsuCodes, "REFRESH", now + REFRESH_TTL_MS));
        return new TokenPair(accessToken, refreshToken, tokens.get(accessToken));
    }

    public TokenEntry validateAndGet(String token) {
        TokenEntry e = tokens.get(token);
        if (e == null || System.currentTimeMillis() > e.expiresAt) return null;
        return e;
    }

    public void invalidate(String token) { tokens.remove(token); }

    public String refreshAccess(String refreshToken) {
        TokenEntry e = validateAndGet(refreshToken);
        if (e == null || !"REFRESH".equals(e.type)) return null;
        String newAccess = "at-" + UUID.randomUUID();
        tokens.put(newAccess, new TokenEntry(e.userId, e.username, e.phone, e.roles, e.permissions,
                e.stationIds, e.fsuCodes, "ACCESS", System.currentTimeMillis() + ACCESS_TTL_MS));
        return newAccess;
    }
}
