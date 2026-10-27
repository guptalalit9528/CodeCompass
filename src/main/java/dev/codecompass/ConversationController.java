package dev.codecompass;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/conversations")
public class ConversationController {
    private final ProjectService service;
    public ConversationController(ProjectService service) { this.service = service; }

    @GetMapping
    public List<ProjectDtos.ConversationSummary> list(@AuthenticationPrincipal UserPrincipal user,
                                                       @RequestParam(required = false) String projectId,
                                                       @RequestParam(required = false) String search) {
        return service.listConversations(user.getId(), projectId, search);
    }

    @PatchMapping("/{conversationId}/project")
    public ProjectDtos.ConversationSummary move(@AuthenticationPrincipal UserPrincipal user,
                                                 @PathVariable String conversationId,
                                                 @Valid @RequestBody ProjectDtos.MoveConversationRequest request) {
        return service.move(user.getId(), conversationId, request.projectId());
    }

    @DeleteMapping("/{conversationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal UserPrincipal user, @PathVariable String conversationId) {
        service.deleteConversation(user.getId(), conversationId);
    }
}
