package com.dcim.platform.module.auth.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 短信验证码内存存储 (BACKEND-FE-API-001, dev/mock).
 * 生产环境应替换为 Redis 或 SMS 服务商。
 */
@Component
public class SmsCodeStore {

    private static class Entry { String codeHash; long expiresAt; boolean used; int sendCount; long lastSendAt; }

    private final Map<String, Entry> store = new ConcurrentHashMap<>();
    private static final long CODE_TTL_MS = 300_000;  // 5 min
    private static final long RATE_LIMIT_MS = 60_000;  // 60s
    private static final int MAX_SENDS_PER_DAY = 10;

    @Value("${app.auth.sms.dev-code-enabled:false}")
    private boolean devCodeEnabled;

    @Value("${app.auth.sms.dev-code:123456}")
    private String devCode;

    public boolean canSend(String phone) {
        Entry e = store.get(phone);
        if (e == null) return true;
        if (e.sendCount >= MAX_SENDS_PER_DAY) return false;
        return System.currentTimeMillis() - e.lastSendAt > RATE_LIMIT_MS;
    }

    public void storeCode(String phone, String code) {
        Entry e = store.computeIfAbsent(phone, k -> new Entry());
        e.codeHash = Integer.toHexString(code.hashCode());
        e.expiresAt = System.currentTimeMillis() + CODE_TTL_MS;
        e.used = false;
        e.sendCount++;
        e.lastSendAt = System.currentTimeMillis();
    }

    public boolean validateAndConsume(String phone, String code) {
        // dev/test 固定验证码（仅 dev-code-enabled=true 时）
        if (devCodeEnabled && devCode.equals(code)) return true;
        Entry e = store.get(phone);
        if (e == null || e.used) return false;
        if (System.currentTimeMillis() > e.expiresAt) return false;
        if (!e.codeHash.equals(Integer.toHexString(code.hashCode()))) return false;
        e.used = true;
        return true;
    }
}
