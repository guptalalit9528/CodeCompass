package dev.codecompass;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "usage_events")
public class UsageEvent {
    @Id
    @Column(length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Column(nullable = false, length = 16)
    private String source;

    @Column(name = "model_name", nullable = false, length = 64)
    private String modelName;

    @Column(nullable = false, length = 16)
    private String status;

    @Column(name = "prompt_tokens", nullable = false)
    private int promptTokens;

    @Column(name = "completion_tokens", nullable = false)
    private int completionTokens;

    @Column(name = "total_tokens", nullable = false)
    private int totalTokens;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected UsageEvent() {}

    public UsageEvent(AppUser user, String source, String modelName, String status, int promptTokens, int completionTokens) {
        this.id = UUID.randomUUID().toString();
        this.user = user;
        this.source = source;
        this.modelName = modelName;
        this.status = status;
        this.promptTokens = promptTokens;
        this.completionTokens = completionTokens;
        this.totalTokens = promptTokens + completionTokens;
        this.createdAt = LocalDateTime.now(Clock.systemUTC());
    }

    public String getId() { return id; }
    public AppUser getUser() { return user; }
    public String getSource() { return source; }
    public String getModelName() { return modelName; }
    public String getStatus() { return status; }
    public int getPromptTokens() { return promptTokens; }
    public int getCompletionTokens() { return completionTokens; }
    public int getTotalTokens() { return totalTokens; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
