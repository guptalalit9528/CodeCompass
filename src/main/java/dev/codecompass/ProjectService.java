package dev.codecompass;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class ProjectService {
    private final ProjectRepository projects;
    private final ConversationRepository conversations;
    private final AppUserRepository users;

    public ProjectService(ProjectRepository projects, ConversationRepository conversations, AppUserRepository users) {
        this.projects = projects;
        this.conversations = conversations;
        this.users = users;
    }

    @Transactional
    public ProjectDtos.ProjectResponse create(String ownerId, ProjectDtos.CreateProjectRequest request) {
        AppUser owner = user(ownerId);
        Project project = projects.save(new Project(owner, request.name().trim(), clean(request.description()), clean(request.instructions())));
        return ProjectDtos.ProjectResponse.from(project);
    }

    @Transactional(readOnly = true)
    public List<ProjectDtos.ProjectResponse> list(String ownerId) {
        return projects.findAllByOwner_IdOrderByUpdatedAtDesc(ownerId).stream().map(ProjectDtos.ProjectResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public ProjectDtos.ProjectResponse get(String ownerId, String projectId) {
        return ProjectDtos.ProjectResponse.from(requireProject(ownerId, projectId));
    }

    @Transactional
    public ProjectDtos.ProjectResponse update(String ownerId, String projectId, ProjectDtos.UpdateProjectRequest request) {
        Project project = requireProject(ownerId, projectId);
        if (request.name() != null) {
            if (!StringUtils.hasText(request.name())) throw new ApiException(HttpStatus.BAD_REQUEST, "Project name cannot be blank.");
            project.setName(request.name().trim());
        }
        if (request.description() != null) project.setDescription(clean(request.description()));
        if (request.instructions() != null) project.setInstructions(clean(request.instructions()));
        project.touch();
        return ProjectDtos.ProjectResponse.from(project);
    }

    @Transactional
    public void delete(String ownerId, String projectId) {
        Project project = requireProject(ownerId, projectId);
        List<Conversation> attached = conversations.findAllByOwner_IdAndProject_IdOrderByUpdatedAtDesc(ownerId, projectId);
        attached.forEach(conversation -> conversation.setProject(null));
        conversations.saveAll(attached);
        projects.delete(project);
    }

    @Transactional(readOnly = true)
    public List<ProjectDtos.ConversationSummary> conversations(String ownerId, String projectId, String search) {
        requireProject(ownerId, projectId);
        List<Conversation> result = StringUtils.hasText(search)
                ? conversations.findAllByOwner_IdAndProject_IdAndTitleContainingIgnoreCaseOrderByUpdatedAtDesc(ownerId, projectId, search.trim())
                : conversations.findAllByOwner_IdAndProject_IdOrderByUpdatedAtDesc(ownerId, projectId);
        return result.stream().map(ProjectDtos.ConversationSummary::from).toList();
    }

    @Transactional
    public void attach(String ownerId, String projectId, String conversationId) {
        Project project = requireProject(ownerId, projectId);
        Conversation conversation = requireConversation(ownerId, conversationId);
        conversation.setProject(project);
    }

    @Transactional
    public void detach(String ownerId, String projectId, String conversationId) {
        requireProject(ownerId, projectId);
        Conversation conversation = requireConversation(ownerId, conversationId);
        if (conversation.getProject() == null || !projectId.equals(conversation.getProject().getId())) {
            throw notFound();
        }
        conversation.setProject(null);
    }

    @Transactional
    public ProjectDtos.ConversationSummary move(String ownerId, String conversationId, String projectId) {
        Conversation conversation = requireConversation(ownerId, conversationId);
        conversation.setProject(StringUtils.hasText(projectId) ? requireProject(ownerId, projectId) : null);
        return ProjectDtos.ConversationSummary.from(conversation);
    }

    @Transactional(readOnly = true)
    public List<ProjectDtos.ConversationSummary> listConversations(String ownerId, String projectId, String search) {
        List<Conversation> result;
        if (StringUtils.hasText(projectId)) {
            requireProject(ownerId, projectId);
            result = StringUtils.hasText(search)
                    ? conversations.findAllByOwner_IdAndProject_IdAndTitleContainingIgnoreCaseOrderByUpdatedAtDesc(ownerId, projectId, search.trim())
                    : conversations.findAllByOwner_IdAndProject_IdOrderByUpdatedAtDesc(ownerId, projectId);
        } else if (StringUtils.hasText(search)) {
            result = conversations.findAllByOwner_IdOrderByUpdatedAtDesc(ownerId).stream()
                    .filter(c -> c.getTitle().toLowerCase(java.util.Locale.ROOT).contains(search.trim().toLowerCase(java.util.Locale.ROOT)))
                    .toList();
        } else {
            result = conversations.findAllByOwner_IdOrderByUpdatedAtDesc(ownerId);
        }
        return result.stream().map(ProjectDtos.ConversationSummary::from).toList();
    }

    @Transactional
    public void deleteConversation(String ownerId, String conversationId) {
        conversations.delete(requireConversation(ownerId, conversationId));
    }

    Project requireProject(String ownerId, String projectId) {
        return projects.findByIdAndOwner_Id(projectId, ownerId).orElseThrow(this::notFound);
    }

    Conversation requireConversation(String ownerId, String conversationId) {
        return conversations.findByIdAndOwner_Id(conversationId, ownerId).orElseThrow(this::notFound);
    }

    private AppUser user(String ownerId) {
        return users.findById(ownerId).orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Please sign in again."));
    }

    private ApiException notFound() { return new ApiException(HttpStatus.NOT_FOUND, "The requested resource was not found."); }
    private String clean(String value) { return StringUtils.hasText(value) ? value.trim() : null; }
}
