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
@Table(name = "projects")
public class Project {
    @Id
    @Column(length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private AppUser owner;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(length = 4000)
    private String instructions;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected Project() {}

    public Project(AppUser owner, String name, String description, String instructions) {
        this.id = java.util.UUID.randomUUID().toString();
        this.owner = owner;
        this.name = name;
        this.description = description;
        this.instructions = instructions;
        this.createdAt = LocalDateTime.now(java.time.Clock.systemUTC());
        this.updatedAt = this.createdAt;
    }

    public String getId() { return id; }
    public AppUser getOwner() { return owner; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; touch(); }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; touch(); }
    public String getInstructions() { return instructions; }
    public void setInstructions(String instructions) { this.instructions = instructions; touch(); }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void touch() { this.updatedAt = LocalDateTime.now(java.time.Clock.systemUTC()); }
}
