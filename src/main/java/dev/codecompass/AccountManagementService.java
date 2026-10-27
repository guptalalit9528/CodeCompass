package dev.codecompass;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountManagementService {
    private final AppUserRepository userRepository;
    private final ApiKeyRepository apiKeyRepository;
    private final UserPreferenceRepository preferenceRepository;
    private final UsageEventRepository usageRepository;
    private final AuthTokenRepository tokenRepository;
    private final ConversationRepository conversationRepository;
    private final ChatMessageRepository messageRepository;
    private final ProjectRepository projectRepository;
    private final ContextItemRepository contextRepository;
    private final SharedCollaboratorRepository collaboratorRepository;
    private final PasswordEncoder passwordEncoder;

    public AccountManagementService(AppUserRepository userRepository,
                                  ApiKeyRepository apiKeyRepository,
                                  UserPreferenceRepository preferenceRepository,
                                  UsageEventRepository usageRepository,
                                  AuthTokenRepository tokenRepository,
                                  ConversationRepository conversationRepository,
                                  ChatMessageRepository messageRepository,
                                  ProjectRepository projectRepository,
                                  ContextItemRepository contextRepository,
                                  SharedCollaboratorRepository collaboratorRepository,
                                  PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.apiKeyRepository = apiKeyRepository;
        this.preferenceRepository = preferenceRepository;
        this.usageRepository = usageRepository;
        this.tokenRepository = tokenRepository;
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.projectRepository = projectRepository;
        this.contextRepository = contextRepository;
        this.collaboratorRepository = collaboratorRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public void deleteAccount(String userId, String confirmation, String password) {
        AppUser user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found."));

        if (password == null || password.getBytes(StandardCharsets.UTF_8).length > 72
                || !passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "The current password is incorrect.");
        }

        String trimmedConfirm = confirmation != null ? confirmation.trim() : "";
        if (!"DELETE".equalsIgnoreCase(trimmedConfirm) && !user.getEmail().equalsIgnoreCase(trimmedConfirm)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Please type DELETE or your email address to confirm.");
        }

        // Cleanup user resources
        contextRepository.deleteAllByUser_Id(userId);
        collaboratorRepository.deleteAllByUser_Id(userId);
        usageRepository.deleteAllByUser_Id(userId);
        apiKeyRepository.deleteAllByUser_Id(userId);
        preferenceRepository.deleteById(userId);
        tokenRepository.deleteAllByUser_Id(userId);

        List<Conversation> userConversations = conversationRepository.findAllByOwner_IdOrderByUpdatedAtDesc(userId);
        for (Conversation conv : userConversations) {
            messageRepository.deleteAllByConversation_Id(conv.getId());
        }
        conversationRepository.deleteAllByOwner_Id(userId);
        projectRepository.deleteAllByOwner_Id(userId);

        userRepository.delete(user);
    }

    @Transactional
    public AppUser changeEmail(String userId, String newEmail, String password) {
        AppUser user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found."));

        if (password == null || password.getBytes(StandardCharsets.UTF_8).length > 72
                || !passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "The current password is incorrect.");
        }

        String normalized = newEmail.trim().toLowerCase(Locale.ROOT);
        if (normalized.equals(user.getEmail())) {
            return user;
        }

        if (userRepository.existsByEmailIgnoreCase(normalized)) {
            throw new ApiException(HttpStatus.CONFLICT, "An account with this email already exists.");
        }

        user.setEmail(normalized);
        return userRepository.save(user);
    }
}
