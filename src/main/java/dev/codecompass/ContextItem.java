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
@Table(name = "context_items")
public class ContextItem {
    @Id
    @Column(length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Column(name = "conversation_id", length = 36)
    private String conversationId;

    @Column(name = "project_id", length = 36)
    private String projectId;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, length = 32)
    private String type;

    @Column(nullable = false, length = 12000)
    private String content;

    @Column(nullable = false)
    private boolean pinned;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected ContextItem() {}

    public ContextItem(AppUser user, String conversationId, String projectId, String name, String type, String content) {
        this.id = UUID.randomUUID().toString();
        this.user = user;
        this.conversationId = conversationId;
        this.projectId = projectId;
        this.name = name != null && !name.isBlank() ? name.trim() : "Snippet";
        this.type = type != null && !type.isBlank() ? type.trim().toLowerCase() : "file";
        this.content = content != null ? content : "";
        this.pinned = true;
        this.createdAt = LocalDateTime.now(Clock.systemUTC());
    }

    public String getId() { return id; }
    public AppUser getUser() { return user; }
    public String getConversationId() { return conversationId; }
    public String getProjectId() { return projectId; }
    public String getName() { return name; }
    public String getType() { return type; }
    public String getContent() { return content; }
    public boolean isPinned() { return pinned; }
    public void setPinned(boolean pinned) { this.pinned = pinned; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
