package dev.codecompass;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class AdvancedWorkspaceController {
    private final AdvancedWorkspaceService workspaceService;
    private final CodeAnalysisService analysisService;
    private final MultiModelService multiModelService;

    public AdvancedWorkspaceController(AdvancedWorkspaceService workspaceService,
                                       CodeAnalysisService analysisService,
                                       MultiModelService multiModelService) {
        this.workspaceService = workspaceService;
        this.analysisService = analysisService;
        this.multiModelService = multiModelService;
    }

    // --- 1. BRANCHING ---
    @GetMapping("/conversations/{conversationId}/branches")
    public List<AdvancedDtos.BranchDto> listBranches(@AuthenticationPrincipal UserPrincipal user,
                                                    @PathVariable String conversationId) {
        return workspaceService.listBranches(user.getId(), conversationId);
    }

    @PostMapping("/conversations/{conversationId}/branches")
    @ResponseStatus(HttpStatus.CREATED)
    public AdvancedDtos.BranchDto createBranch(@AuthenticationPrincipal UserPrincipal user,
                                              @PathVariable String conversationId,
                                              @Valid @RequestBody AdvancedDtos.CreateBranchRequest request) {
        return workspaceService.createBranch(user.getId(), conversationId, request);
    }

    @DeleteMapping("/conversations/{conversationId}/branches/{branchId}")
    public Map<String, Boolean> deleteBranch(@AuthenticationPrincipal UserPrincipal user,
                                            @PathVariable String conversationId,
                                            @PathVariable String branchId) {
        workspaceService.deleteBranch(user.getId(), branchId);
        return Map.of("deleted", true);
    }

    // --- 2. CHECKPOINTS ---
    @GetMapping("/conversations/{conversationId}/checkpoints")
    public List<AdvancedDtos.CheckpointDto> listCheckpoints(@AuthenticationPrincipal UserPrincipal user,
                                                          @PathVariable String conversationId) {
        return workspaceService.listCheckpoints(user.getId(), conversationId);
    }

    @PostMapping("/conversations/{conversationId}/checkpoints")
    @ResponseStatus(HttpStatus.CREATED)
    public AdvancedDtos.CheckpointDto createCheckpoint(@AuthenticationPrincipal UserPrincipal user,
                                                      @PathVariable String conversationId,
                                                      @Valid @RequestBody AdvancedDtos.CreateCheckpointRequest request) {
        return workspaceService.createCheckpoint(user.getId(), conversationId, request);
    }

    // --- 3. CONTEXT PINNING ---
    @GetMapping("/context")
    public List<AdvancedDtos.ContextItemDto> listContext(@AuthenticationPrincipal UserPrincipal user,
                                                        @RequestParam(required = false) String conversationId) {
        return workspaceService.listContext(user.getId(), conversationId);
    }

    @PostMapping("/context")
    @ResponseStatus(HttpStatus.CREATED)
    public AdvancedDtos.ContextItemDto addContext(@AuthenticationPrincipal UserPrincipal user,
                                                 @Valid @RequestBody AdvancedDtos.CreateContextRequest request) {
        return workspaceService.addContext(user.getId(), request);
    }

    @PatchMapping("/context/{id}/pin")
    public AdvancedDtos.ContextItemDto toggleContextPin(@AuthenticationPrincipal UserPrincipal user,
                                                       @PathVariable String id) {
        return workspaceService.toggleContextPin(user.getId(), id);
    }

    @DeleteMapping("/context/{id}")
    public Map<String, Boolean> deleteContext(@AuthenticationPrincipal UserPrincipal user,
                                             @PathVariable String id) {
        workspaceService.deleteContext(user.getId(), id);
        return Map.of("deleted", true);
    }

    // --- 4. COLLABORATION ---
    @GetMapping("/conversations/{conversationId}/collaborators")
    public List<AdvancedDtos.CollaboratorDto> listCollaborators(@AuthenticationPrincipal UserPrincipal user,
                                                              @PathVariable String conversationId) {
        return workspaceService.listCollaborators(user.getId(), conversationId);
    }

    @PostMapping("/conversations/{conversationId}/collaborators")
    @ResponseStatus(HttpStatus.CREATED)
    public AdvancedDtos.CollaboratorDto inviteCollaborator(@AuthenticationPrincipal UserPrincipal user,
                                                          @PathVariable String conversationId,
                                                          @Valid @RequestBody AdvancedDtos.InviteCollaboratorRequest request) {
        return workspaceService.inviteCollaborator(user.getId(), conversationId, request);
    }

    @DeleteMapping("/conversations/{conversationId}/collaborators/{collaboratorId}")
    public Map<String, Boolean> removeCollaborator(@AuthenticationPrincipal UserPrincipal user,
                                                   @PathVariable String conversationId,
                                                   @PathVariable String collaboratorId) {
        workspaceService.removeCollaborator(user.getId(), collaboratorId);
        return Map.of("removed", true);
    }

    // --- 5. CODE ANALYSIS (AST & DIFF) ---
    @PostMapping("/analysis/ast")
    public AdvancedDtos.AstAnalysisResponse analyzeAst(@Valid @RequestBody AdvancedDtos.AstAnalysisRequest request) {
        return analysisService.parseAst(request.code(), request.language());
    }

    @PostMapping("/analysis/diff")
    public AdvancedDtos.DiffAnalysisResponse computeDiff(@Valid @RequestBody AdvancedDtos.DiffAnalysisRequest request) {
        return analysisService.computeDiff(request.originalCode(), request.modifiedCode(), request.filename());
    }

    // --- 6. MULTI-MODEL COMPARISON ---
    @PostMapping("/analysis/multi-model")
    public AdvancedDtos.MultiModelResponse compareModels(@AuthenticationPrincipal UserPrincipal user,
                                                        @Valid @RequestBody AdvancedDtos.MultiModelRequest request) {
        return multiModelService.compareModels(user.getId(), request);
    }
}
