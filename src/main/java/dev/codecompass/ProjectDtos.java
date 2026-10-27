package dev.codecompass;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

public final class ProjectDtos {
    private ProjectDtos() {}

    public record CreateProjectRequest(@NotBlank @Size(max = 120) String name,
                                       @Size(max = 500) String description,
                                       @Size(max = 4000) String instructions) {}
    public record UpdateProjectRequest(@Size(max = 120) String name,
                                       @Size(max = 500) String description,
                                       @Size(max = 4000) String instructions) {}
    public record ProjectResponse(String id, String name, String description, String instructions,
                                  LocalDateTime createdAt, LocalDateTime updatedAt) {
        static ProjectResponse from(Project p) {
            return new ProjectResponse(p.getId(), p.getName(), p.getDescription(), p.getInstructions(),
                    p.getCreatedAt(), p.getUpdatedAt());
        }
    }
    public record MoveConversationRequest(@jakarta.validation.constraints.Size(max = 36) String projectId) {}
    public record ConversationSummary(String id, String title, String projectId,
                                      LocalDateTime createdAt, LocalDateTime updatedAt) {
        static ConversationSummary from(Conversation c) {
            return new ConversationSummary(c.getId(), c.getTitle(),
                    c.getProject() == null ? null : c.getProject().getId(), c.getCreatedAt(), c.getUpdatedAt());
        }
    }
}
