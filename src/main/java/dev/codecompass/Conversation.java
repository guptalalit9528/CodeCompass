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
@Table(name = "conversations")
public class Conversation {
    @Id
    @Column(length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private AppUser owner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private Project project;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected Conversation() {}

    public Conversation(AppUser owner, Project project, String title) {
        this.id = java.util.UUID.randomUUID().toString();
        this.owner = owner;
        this.project = project;
        this.title = title;
        this.createdAt = LocalDateTime.now(java.time.Clock.systemUTC());
        this.updatedAt = this.createdAt;
    }

    public String getId() { return id; }
    public AppUser getOwner() { return owner; }
    public Project getProject() { return project; }
    public void setProject(Project project) { this.project = project; touch(); }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; touch(); }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void touch() { this.updatedAt = LocalDateTime.now(java.time.Clock.systemUTC()); }
}
