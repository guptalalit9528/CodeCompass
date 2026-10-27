package dev.codecompass;

import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class ConversationService {
    private final ConversationRepository conversations;
    private final ChatMessageRepository messages;
    private final ProjectService projects;
    private final AppUserRepository users;
    private final int maxContextMessages;

    public ConversationService(ConversationRepository conversations, ChatMessageRepository messages,
                               ProjectService projects, AppUserRepository users,
                               @Value("${codecompass.chat.max-context-messages:12}") int maxContextMessages) {
        this.conversations = conversations;
        this.messages = messages;
        this.projects = projects;
        this.users = users;
        this.maxContextMessages = Math.max(0, maxContextMessages);
    }

    @Transactional
    public PreparedTurn prepare(String ownerId, String conversationId, String projectId, String question) {
        Conversation conversation;
        if (StringUtils.hasText(conversationId)) {
            if (conversationId.length() > 36) throw new ApiException(HttpStatus.BAD_REQUEST, "The conversation ID is invalid.");
            conversation = projects.requireConversation(ownerId, conversationId);
            if (projectId != null) {
                conversation.setProject(StringUtils.hasText(projectId) ? projects.requireProject(ownerId, projectId) : null);
            }
        } else {
            AppUser owner = users.findById(ownerId)
                    .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Please sign in again."));
            Project project = StringUtils.hasText(projectId) ? projects.requireProject(ownerId, projectId) : null;
            conversation = new Conversation(owner, project, title(question));
            conversation = conversations.save(conversation);
        }

        List<ChatMessage> recent = maxContextMessages == 0 ? List.of()
                : messages.findAllByConversation_Id(conversation.getId(), PageRequest.of(0,
                        maxContextMessages * 2, Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by("id"))));
        Collections.reverse(recent);
        List<ChatController.Message> history = recent.stream()
                .map(m -> ChatController.Message.from(m.getRole(), m.getContent(), m.getCreatedAt()))
                .toList();

        if ("New conversation".equals(conversation.getTitle())) conversation.setTitle(title(question));
        conversation.touch();
        messages.save(new ChatMessage(conversation, "user", question));
        String instructions = conversation.getProject() == null ? null : conversation.getProject().getInstructions();
        return new PreparedTurn(conversation.getId(), history, instructions);
    }

    @Transactional
    public ChatController.ChatResponse saveAssistantReply(String ownerId, String conversationId, String answer) {
        Conversation conversation = projects.requireConversation(ownerId, conversationId);
        messages.save(new ChatMessage(conversation, "assistant", answer));
        conversation.touch();
        return new ChatController.ChatResponse(conversationId,
                ChatController.Message.from("assistant", answer, java.time.LocalDateTime.now(ZoneOffset.UTC)), historyInTransaction(conversationId));
    }

    @Transactional(readOnly = true)
    public List<ChatController.Message> history(String ownerId, String conversationId) {
        projects.requireConversation(ownerId, conversationId);
        return messages.findAllByConversation_Id(conversationId, Sort.by(Sort.Direction.ASC, "createdAt").and(Sort.by("id"))).stream()
                .map(m -> ChatController.Message.from(m.getRole(), m.getContent(), m.getCreatedAt())).toList();
    }

    @Transactional
    public void clearMessages(String ownerId, String conversationId) {
        projects.requireConversation(ownerId, conversationId);
        messages.deleteAllByConversation_Id(conversationId);
    }

    private List<ChatController.Message> historyInTransaction(String conversationId) {
        List<ChatController.Message> result = new ArrayList<>();
        messages.findAllByConversation_Id(conversationId, Sort.by(Sort.Direction.ASC, "createdAt").and(Sort.by("id")))
                .forEach(m -> result.add(ChatController.Message.from(m.getRole(), m.getContent(), m.getCreatedAt())));
        return result;
    }

    private static String title(String question) {
        String normalized = question.replaceAll("\\s+", " ").trim();
        return normalized.length() <= 200 ? normalized : normalized.substring(0, 197) + "...";
    }

    public record PreparedTurn(String conversationId, List<ChatController.Message> history, String projectInstructions) {}
}
