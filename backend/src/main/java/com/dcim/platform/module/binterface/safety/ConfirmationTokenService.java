package com.dcim.platform.module.binterface.safety;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * confirmationToken 生成与验证服务 (BIF-P4-SAFE-003)。
 *
 * <p>内存实现，不保存明文 token。生产环境需替换为持久化实现。</p>
 */
@Service
public class ConfirmationTokenService {

    private static final Logger log = LoggerFactory.getLogger(ConfirmationTokenService.class);
    private final Map<String, ConfirmationToken> store = new ConcurrentHashMap<>();

    /**
     * 生成一次性确认令牌。
     *
     * @return 明文 token（仅返回一次，调用方需妥善传递）
     */
    public String generateToken(String commandName, String suid, String requestedBy,
                                 String operationSummary, long ttlSeconds) {
        String raw = UUID.randomUUID().toString() + "-" + commandName + "-" + suid;
        String hash = sha256(raw);
        ConfirmationToken ct = new ConfirmationToken(hash, commandName, suid, requestedBy,
                Instant.now().plusSeconds(ttlSeconds > 0 ? ttlSeconds : 3600));
        store.put(hash, ct);
        log.info("Token 生成: hash={}, cmd={}, suid={}, ttl={}s",
                hash.substring(0, 8), commandName, suid, ttlSeconds);
        return raw;
    }

    /** 默认 TTL=3600s。 */
    public String generateToken(String commandName, String suid, String requestedBy, String summary) {
        return generateToken(commandName, suid, requestedBy, summary, 3600);
    }

    /**
     * 验证令牌。
     */
    public TokenValidationResult validate(String rawToken, String commandName, String suid) {
        if (rawToken == null || rawToken.isEmpty())
            return TokenValidationResult.fail("CONFIRMATION_TOKEN_MISSING", "缺少确认令牌");
        String hash = sha256(rawToken);
        ConfirmationToken ct = store.get(hash);
        if (ct == null)
            return TokenValidationResult.fail("CONFIRMATION_TOKEN_INVALID", "令牌无效");
        if (ct.isExpired()) {
            store.remove(hash);
            return TokenValidationResult.fail("CONFIRMATION_TOKEN_EXPIRED", "令牌已过期");
        }
        if (ct.isUsed())
            return TokenValidationResult.fail("CONFIRMATION_TOKEN_USED", "令牌已使用");
        if (!ct.getCommandName().equals(commandName))
            return TokenValidationResult.fail("CONFIRMATION_TOKEN_COMMAND_MISMATCH",
                    "令牌命令不匹配: expected=" + commandName + ", actual=" + ct.getCommandName());
        if (!ct.getSuid().equals(suid))
            return TokenValidationResult.fail("CONFIRMATION_TOKEN_SUID_MISMATCH",
                    "令牌 SUID 不匹配: expected=" + suid + ", actual=" + ct.getSuid());
        return TokenValidationResult.success(hash);
    }

    /** 标记令牌已使用。 */
    public void markUsed(String tokenHash) {
        ConfirmationToken ct = store.get(tokenHash);
        if (ct != null) ct.markUsed();
    }

    private String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    /**
     * Token 验证结果。
     */
    public static class TokenValidationResult {
        private final boolean valid;
        private final String reasonCode;
        private final String reasonMessage;
        private final String tokenHash;

        private TokenValidationResult(boolean v, String code, String msg, String hash) {
            this.valid = v; this.reasonCode = code; this.reasonMessage = msg; this.tokenHash = hash;
        }
        static TokenValidationResult success(String hash) {
            return new TokenValidationResult(true, null, null, hash);
        }
        static TokenValidationResult fail(String code, String msg) {
            return new TokenValidationResult(false, code, msg, null);
        }
        public boolean isValid() { return valid; }
        public String getReasonCode() { return reasonCode; }
        public String getReasonMessage() { return reasonMessage; }
        public String getTokenHash() { return tokenHash; }

        @Override
        public String toString() {
            return "TokenValidationResult{valid=" + valid
                    + (reasonCode != null ? ", reason=" + reasonCode : "") + "}";
        }
    }
}
