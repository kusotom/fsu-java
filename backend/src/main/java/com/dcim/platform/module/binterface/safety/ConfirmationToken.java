package com.dcim.platform.module.binterface.safety;

import java.time.Instant;
import java.util.Objects;

/**
 * SET 命令确认令牌 (BIF-P4-SAFE-003)。
 *
 * <p>不保存明文 token。对外只暴露 tokenHash。</p>
 */
public class ConfirmationToken {

    private final String tokenHash;
    private final String commandName;
    private final String suid;
    private final String requestedBy;
    private final Instant expiresAt;
    private final Instant createdAt;
    private boolean used;

    public ConfirmationToken(String tokenHash, String commandName, String suid,
                              String requestedBy, Instant expiresAt) {
        this.tokenHash = Objects.requireNonNull(tokenHash);
        this.commandName = Objects.requireNonNull(commandName);
        this.suid = Objects.requireNonNull(suid);
        this.requestedBy = requestedBy != null ? requestedBy : "UNKNOWN";
        this.expiresAt = expiresAt != null ? expiresAt : Instant.now().plusSeconds(3600);
        this.createdAt = Instant.now();
        this.used = false;
    }

    public String getTokenHash() { return tokenHash; }
    public String getCommandName() { return commandName; }
    public String getSuid() { return suid; }
    public String getRequestedBy() { return requestedBy; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getCreatedAt() { return createdAt; }
    public boolean isUsed() { return used; }
    public boolean isExpired() { return Instant.now().isAfter(expiresAt); }
    public void markUsed() { this.used = true; }

    @Override
    public String toString() {
        return "ConfirmationToken{hash=" + tokenHash.substring(0, Math.min(8, tokenHash.length()))
                + "..., cmd=" + commandName + ", suid=" + suid + "}";
    }
}
