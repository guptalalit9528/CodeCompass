package dev.codecompass;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ChatController {
    private final GeminiAssistantService geminiAssistant;
    private final SupportService supportService;
    private final ConversationService conversations;
    private final UsageTrackingService usageService;
    private final int maxMessageCharacters;

    public ChatController(GeminiAssistantService geminiAssistant, SupportService supportService,
                          ConversationService conversations,
                          UsageTrackingService usageService,
                          @Value("${codecompass.chat.max-message-characters:12000}") int maxMessageCharacters) {
        this.geminiAssistant = geminiAssistant;
        this.supportService = supportService;
        this.conversations = conversations;
        this.usageService = usageService;
        this.maxMessageCharacters = maxMessageCharacters;
    }

    @PostMapping("/chat")
    public ChatResponse chat(@AuthenticationPrincipal UserPrincipal user,
                             @Valid @RequestBody ChatRequest request,
                             HttpServletRequest servletRequest) {
        long startTime = System.currentTimeMillis();
        String question = request.message().trim();
        if (question.length() > maxMessageCharacters) {
            throw new ApiException(HttpStatus.PAYLOAD_TOO_LARGE,
                    "Your message is too long. Please keep it under " + maxMessageCharacters + " characters.");
        }
        if (request.projectId() != null && request.projectId().length() > 36) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "The project ID is invalid.");
        }

        ConversationService.PreparedTurn turn = conversations.prepare(user.getId(), request.sessionId(), request.projectId(), question);
        String answer = geminiAssistant.isConfigured()
                ? geminiAssistant.answer(turn.history(), question, turn.projectInstructions())
                : "Gemini is not configured, so this is offline troubleshooting guidance:\n\n" + supportService.answer(question);
        ChatResponse response = conversations.saveAssistantReply(user.getId(), turn.conversationId(), answer);

        long latencyMs = System.currentTimeMillis() - startTime;
        int promptTokens = Math.max(1, question.length() / 4);
        int completionTokens = Math.max(1, answer.length() / 4);
        double estimatedCost = (promptTokens * 0.000000075) + (completionTokens * 0.00000030);

        response.latencyMs = latencyMs;
        response.promptTokens = promptTokens;
        response.completionTokens = completionTokens;
        response.totalTokens = promptTokens + completionTokens;
        response.estimatedCost = estimatedCost;

        String source = servletRequest != null && Boolean.TRUE.equals(servletRequest.getAttribute(ApiKeyAuthenticationFilter.API_KEY_AUTH_ATTR))
                ? "API_KEY" : "WEB";
        usageService.recordUsage(user.getId(), source, "gemini-2.5-flash", "SUCCESS", promptTokens, completionTokens);

        return response;
    }

    @GetMapping("/chat/{sessionId}")
    public List<Message> history(@AuthenticationPrincipal UserPrincipal user, @PathVariable String sessionId) {
        return conversations.history(user.getId(), sessionId);
    }

    /** Preserves the old API meaning: clear messages but keep the conversation itself. */
    @DeleteMapping("/chat/{sessionId}")
    public java.util.Map<String, Boolean> clear(@AuthenticationPrincipal UserPrincipal user, @PathVariable String sessionId) {
        conversations.clearMessages(user.getId(), sessionId);
        return java.util.Map.of("cleared", true);
    }

    public record ChatRequest(String sessionId,
                              @NotBlank @Size(max = 12000) String message,
                              @Size(max = 36) String projectId) {}

    public static class Message {
        public String role;
        public String content;
        public long timestamp;

        public Message() {}
        public Message(String role, String content) { this(role, content, LocalDateTime.now(ZoneOffset.UTC)); }
        private Message(String role, String content, LocalDateTime createdAt) {
            this.role = role;
            this.content = content;
            this.timestamp = createdAt.toInstant(ZoneOffset.UTC).toEpochMilli();
        }
        static Message from(String role, String content, LocalDateTime createdAt) { return new Message(role, content, createdAt); }
    }

    public static class ChatResponse {
        public String sessionId;
        public Message reply;
        public List<Message> messages;
        public long latencyMs;
        public int promptTokens;
        public int completionTokens;
        public int totalTokens;
        public double estimatedCost;

        public ChatResponse(String id, Message reply, List<Message> messages) {
            this.sessionId = id;
            this.reply = reply;
            this.messages = messages;
        }
    }
}
