package dev.codecompass;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {
    private final ProjectService projects;
    public ProjectController(ProjectService projects) { this.projects = projects; }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProjectDtos.ProjectResponse create(@AuthenticationPrincipal UserPrincipal user,
                                               @Valid @RequestBody ProjectDtos.CreateProjectRequest request) {
        return projects.create(user.getId(), request);
    }

    @GetMapping
    public List<ProjectDtos.ProjectResponse> list(@AuthenticationPrincipal UserPrincipal user) {
        return projects.list(user.getId());
    }

    @GetMapping("/{projectId}")
    public ProjectDtos.ProjectResponse get(@AuthenticationPrincipal UserPrincipal user, @PathVariable String projectId) {
        return projects.get(user.getId(), projectId);
    }

    @PatchMapping("/{projectId}")
    public ProjectDtos.ProjectResponse update(@AuthenticationPrincipal UserPrincipal user, @PathVariable String projectId,
                                              @Valid @RequestBody ProjectDtos.UpdateProjectRequest request) {
        return projects.update(user.getId(), projectId, request);
    }

    @DeleteMapping("/{projectId}")
    public Map<String, Boolean> delete(@AuthenticationPrincipal UserPrincipal user, @PathVariable String projectId) {
        projects.delete(user.getId(), projectId);
        return Map.of("deleted", true, "conversationsDetached", true);
    }

    @GetMapping("/{projectId}/conversations")
    public List<ProjectDtos.ConversationSummary> conversations(@AuthenticationPrincipal UserPrincipal user,
                                                                @PathVariable String projectId,
                                                                @RequestParam(required = false) String search) {
        return projects.conversations(user.getId(), projectId, search);
    }

    @PutMapping("/{projectId}/conversations/{conversationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void attach(@AuthenticationPrincipal UserPrincipal user, @PathVariable String projectId,
                       @PathVariable String conversationId) {
        projects.attach(user.getId(), projectId, conversationId);
    }

    @DeleteMapping("/{projectId}/conversations/{conversationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void detach(@AuthenticationPrincipal UserPrincipal user, @PathVariable String projectId,
                       @PathVariable String conversationId) {
        projects.detach(user.getId(), projectId, conversationId);
    }
}
