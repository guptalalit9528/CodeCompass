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
@Table(name = "conversation_branches")
public class ConversationBranch {
    @Id
    @Column(length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "forked_from_message_id", length = 36)
    private String forkedFromMessageId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected ConversationBranch() {}

    public ConversationBranch(Conversation conversation, String name, String forkedFromMessageId) {
        this.id = UUID.randomUUID().toString();
        this.conversation = conversation;
        this.name = name != null && !name.isBlank() ? name.trim() : "branch-" + id.substring(0, 6);
        this.forkedFromMessageId = forkedFromMessageId;
        this.createdAt = LocalDateTime.now(Clock.systemUTC());
    }

    public String getId() { return id; }
    public Conversation getConversation() { return conversation; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getForkedFromMessageId() { return forkedFromMessageId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
