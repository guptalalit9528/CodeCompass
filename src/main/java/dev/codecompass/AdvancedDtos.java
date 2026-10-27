package dev.codecompass;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public final class AdvancedDtos {
    private AdvancedDtos() {}

    // 1. Branching
    public record BranchDto(String id, String name, String forkedFromMessageId, LocalDateTime createdAt) {
        public static BranchDto from(ConversationBranch branch) {
            return new BranchDto(branch.getId(), branch.getName(), branch.getForkedFromMessageId(), branch.getCreatedAt());
        }
    }

    public record CreateBranchRequest(
            @NotBlank @Size(max = 100) String name,
            String forkedFromMessageId
    ) {}

    // 2. Checkpoints
    public record CheckpointDto(String id, String branchId, String messageId, String title, String description, LocalDateTime createdAt) {
        public static CheckpointDto from(BranchCheckpoint cp) {
            return new CheckpointDto(cp.getId(), cp.getBranchId(), cp.getMessageId(), cp.getTitle(), cp.getDescription(), cp.getCreatedAt());
        }
    }

    public record CreateCheckpointRequest(
            String branchId,
            String messageId,
            @NotBlank @Size(max = 150) String title,
            @Size(max = 500) String description
    ) {}

    // 3. Pinned Context
    public record ContextItemDto(String id, String conversationId, String projectId, String name, String type, String content, boolean pinned, LocalDateTime createdAt) {
        public static ContextItemDto from(ContextItem item) {
            return new ContextItemDto(item.getId(), item.getConversationId(), item.getProjectId(), item.getName(), item.getType(), item.getContent(), item.isPinned(), item.getCreatedAt());
        }
    }

    public record CreateContextRequest(
            String conversationId,
            String projectId,
            @NotBlank @Size(max = 150) String name,
            String type,
            @NotBlank @Size(max = 12000) String content
    ) {}

    // 4. Collaboration
    public record CollaboratorDto(String id, String conversationId, String collaboratorEmail, String role, LocalDateTime createdAt) {
        public static CollaboratorDto from(SharedCollaborator sc) {
            return new CollaboratorDto(sc.getId(), sc.getConversation().getId(), sc.getCollaboratorEmail(), sc.getRole(), sc.getCreatedAt());
        }
    }

    public record InviteCollaboratorRequest(
            @NotBlank @Email @Size(max = 254) String email,
            String role
    ) {}

    // 5. Multi-Model Comparison
    public record MultiModelRequest(
            @NotBlank @Size(max = 12000) String prompt,
            List<String> models,
            String projectId
    ) {}

    public record ModelResult(
            String model,
            String response,
            long latencyMs,
            int promptTokens,
            int completionTokens,
            double estimatedCost,
            String status
    ) {}

    public record MultiModelResponse(
            String prompt,
            List<ModelResult> results
    ) {}

    // 6. AST & Static Analysis
    public record AstNodeDto(
            String type,
            String name,
            int lineStart,
            int lineEnd,
            List<AstNodeDto> children,
            Map<String, String> properties
    ) {}

    public record AstAnalysisRequest(
            @NotBlank @Size(max = 24000) String code,
            String language
    ) {}

    public record AstAnalysisResponse(
            String language,
            boolean parsed,
            List<AstNodeDto> nodes,
            String summary
    ) {}

    // 7. Inline Diff & Patch Viewer
    public record DiffAnalysisRequest(
            @NotBlank @Size(max = 24000) String originalCode,
            @NotBlank @Size(max = 24000) String modifiedCode,
            String filename
    ) {}

    public record DiffLineDto(String type, int oldLine, int newLine, String content) {}

    public record DiffAnalysisResponse(
            String filename,
            String patchText,
            int additions,
            int deletions,
            List<DiffLineDto> lines
    ) {}
}
