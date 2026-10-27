package dev.codecompass;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "auth_tokens")
public class AuthToken {
    public enum Purpose { VERIFY_EMAIL, RESET_PASSWORD }

    @Id
    @Column(length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Column(nullable = false, length = 24)
    private String purpose;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "consumed_at")
    private LocalDateTime consumedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected AuthToken() {}

    public AuthToken(AppUser user, Purpose purpose, String tokenHash, LocalDateTime expiresAt) {
        this.id = java.util.UUID.randomUUID().toString();
        this.user = user;
        this.purpose = purpose.name();
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
        this.createdAt = LocalDateTime.now(java.time.Clock.systemUTC());
    }

    public AppUser getUser() { return user; }
    public Purpose getPurpose() { return Purpose.valueOf(purpose); }
    public String getTokenHash() { return tokenHash; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public LocalDateTime getConsumedAt() { return consumedAt; }
    public void consume() { this.consumedAt = LocalDateTime.now(java.time.Clock.systemUTC()); }
}
