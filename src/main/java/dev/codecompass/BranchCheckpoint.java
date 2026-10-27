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
@Table(name = "branch_checkpoints")
public class BranchCheckpoint {
    @Id
    @Column(length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    @Column(name = "branch_id", length = 36)
    private String branchId;

    @Column(name = "message_id", length = 36)
    private String messageId;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(length = 500)
    private String description;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected BranchCheckpoint() {}

    public BranchCheckpoint(Conversation conversation, String branchId, String messageId, String title, String description) {
        this.id = UUID.randomUUID().toString();
        this.conversation = conversation;
        this.branchId = branchId;
        this.messageId = messageId;
        this.title = title != null && !title.isBlank() ? title.trim() : "Checkpoint";
        this.description = description;
        this.createdAt = LocalDateTime.now(Clock.systemUTC());
    }

    public String getId() { return id; }
    public Conversation getConversation() { return conversation; }
    public String getBranchId() { return branchId; }
    public String getMessageId() { return messageId; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
