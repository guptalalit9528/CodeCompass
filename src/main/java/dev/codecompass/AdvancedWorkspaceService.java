package dev.codecompass;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdvancedWorkspaceService {
    private final ConversationRepository conversationRepository;
    private final ConversationBranchRepository branchRepository;
    private final BranchCheckpointRepository checkpointRepository;
    private final ContextItemRepository contextRepository;
    private final SharedCollaboratorRepository collaboratorRepository;
    private final AppUserRepository userRepository;

    public AdvancedWorkspaceService(ConversationRepository conversationRepository,
                                   ConversationBranchRepository branchRepository,
                                   BranchCheckpointRepository checkpointRepository,
                                   ContextItemRepository contextRepository,
                                   SharedCollaboratorRepository collaboratorRepository,
                                   AppUserRepository userRepository) {
        this.conversationRepository = conversationRepository;
        this.branchRepository = branchRepository;
        this.checkpointRepository = checkpointRepository;
        this.contextRepository = contextRepository;
        this.collaboratorRepository = collaboratorRepository;
        this.userRepository = userRepository;
    }

    private Conversation requireConversation(String ownerId, String conversationId) {
        return conversationRepository.findByIdAndOwner_Id(conversationId, ownerId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Conversation not found."));
    }

    // --- 1. BRANCHING ---
    @Transactional(readOnly = true)
    public List<AdvancedDtos.BranchDto> listBranches(String ownerId, String conversationId) {
        requireConversation(ownerId, conversationId);
        return branchRepository.findAllByConversation_IdOrderByCreatedAtAsc(conversationId)
                .stream().map(AdvancedDtos.BranchDto::from).toList();
    }

    @Transactional
    public AdvancedDtos.BranchDto createBranch(String ownerId, String conversationId, AdvancedDtos.CreateBranchRequest request) {
        Conversation conv = requireConversation(ownerId, conversationId);
        ConversationBranch branch = new ConversationBranch(conv, request.name(), request.forkedFromMessageId());
        return AdvancedDtos.BranchDto.from(branchRepository.save(branch));
    }

    @Transactional
    public void deleteBranch(String ownerId, String branchId) {
        ConversationBranch branch = branchRepository.findByIdAndConversation_Owner_Id(branchId, ownerId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Branch not found."));
        branchRepository.delete(branch);
    }

    // --- 2. CHECKPOINTS ---
    @Transactional(readOnly = true)
    public List<AdvancedDtos.CheckpointDto> listCheckpoints(String ownerId, String conversationId) {
        requireConversation(ownerId, conversationId);
        return checkpointRepository.findAllByConversation_IdOrderByCreatedAtDesc(conversationId)
                .stream().map(AdvancedDtos.CheckpointDto::from).toList();
    }

    @Transactional
    public AdvancedDtos.CheckpointDto createCheckpoint(String ownerId, String conversationId, AdvancedDtos.CreateCheckpointRequest request) {
        Conversation conv = requireConversation(ownerId, conversationId);
        BranchCheckpoint checkpoint = new BranchCheckpoint(
                conv,
                request.branchId(),
                request.messageId(),
                request.title(),
                request.description()
        );
        return AdvancedDtos.CheckpointDto.from(checkpointRepository.save(checkpoint));
    }

    // --- 3. CONTEXT PINNING ---
    @Transactional(readOnly = true)
    public List<AdvancedDtos.ContextItemDto> listContext(String userId, String conversationId) {
        return contextRepository.findAllByUser_IdAndConversationIdOrderByCreatedAtDesc(userId, conversationId)
                .stream().map(AdvancedDtos.ContextItemDto::from).toList();
    }

    @Transactional
    public AdvancedDtos.ContextItemDto addContext(String userId, AdvancedDtos.CreateContextRequest request) {
        AppUser user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "User not found."));
        ContextItem item = new ContextItem(
                user,
                request.conversationId(),
                request.projectId(),
                request.name(),
                request.type(),
                request.content()
        );
        return AdvancedDtos.ContextItemDto.from(contextRepository.save(item));
    }

    @Transactional
    public AdvancedDtos.ContextItemDto toggleContextPin(String userId, String contextId) {
        ContextItem item = contextRepository.findByIdAndUser_Id(contextId, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Context item not found."));
        item.setPinned(!item.isPinned());
        return AdvancedDtos.ContextItemDto.from(contextRepository.save(item));
    }

    @Transactional
    public void deleteContext(String userId, String contextId) {
        ContextItem item = contextRepository.findByIdAndUser_Id(contextId, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Context item not found."));
        contextRepository.delete(item);
    }

    // --- 4. COLLABORATION ---
    @Transactional(readOnly = true)
    public List<AdvancedDtos.CollaboratorDto> listCollaborators(String ownerId, String conversationId) {
        requireConversation(ownerId, conversationId);
        return collaboratorRepository.findAllByConversation_IdOrderByCreatedAtAsc(conversationId)
                .stream().map(AdvancedDtos.CollaboratorDto::from).toList();
    }

    @Transactional
    public AdvancedDtos.CollaboratorDto inviteCollaborator(String ownerId, String conversationId, AdvancedDtos.InviteCollaboratorRequest request) {
        Conversation conv = requireConversation(ownerId, conversationId);
        String email = request.email().trim().toLowerCase();
        if (collaboratorRepository.existsByConversation_IdAndCollaboratorEmailIgnoreCase(conversationId, email)) {
            throw new ApiException(HttpStatus.CONFLICT, "This collaborator is already invited.");
        }
        AppUser owner = conv.getOwner();
        SharedCollaborator collaborator = new SharedCollaborator(conv, owner, email, request.role());
        return AdvancedDtos.CollaboratorDto.from(collaboratorRepository.save(collaborator));
    }

    @Transactional
    public void removeCollaborator(String ownerId, String collaboratorId) {
        SharedCollaborator sc = collaboratorRepository.findByIdAndConversation_Owner_Id(collaboratorId, ownerId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Collaborator record not found."));
        collaboratorRepository.delete(sc);
    }
}
