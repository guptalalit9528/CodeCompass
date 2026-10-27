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
@Table(name = "shared_collaborators")
public class SharedCollaborator {
    @Id
    @Column(length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Column(name = "collaborator_email", nullable = false, length = 254)
    private String collaboratorEmail;

    @Column(nullable = false, length = 32)
    private String role;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected SharedCollaborator() {}

    public SharedCollaborator(Conversation conversation, AppUser user, String collaboratorEmail, String role) {
        this.id = UUID.randomUUID().toString();
        this.conversation = conversation;
        this.user = user;
        this.collaboratorEmail = collaboratorEmail.trim().toLowerCase();
        this.role = role != null && !role.isBlank() ? role.trim().toLowerCase() : "viewer";
        this.createdAt = LocalDateTime.now(Clock.systemUTC());
    }

    public String getId() { return id; }
    public Conversation getConversation() { return conversation; }
    public AppUser getUser() { return user; }
    public String getCollaboratorEmail() { return collaboratorEmail; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
