package dev.codecompass;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class ChatController {
    private static final int MAX_STORED_MESSAGES = 100;

    private final GeminiAssistantService geminiAssistant;
    private final SupportService supportService;
    private final int maxMessageCharacters;
    private final Map<String, List<Message>> conversations = new ConcurrentHashMap<String, List<Message>>();

    public ChatController(GeminiAssistantService geminiAssistant,
                          SupportService supportService,
                          @Value("${codecompass.chat.max-message-characters:12000}") int maxMessageCharacters) {
        this.geminiAssistant = geminiAssistant;
        this.supportService = supportService;
        this.maxMessageCharacters = maxMessageCharacters;
    }

    @PostMapping("/chat")
    public ChatResponse chat(@RequestBody(required = false) ChatRequest request) {
        if (request == null || request.message == null || request.message.trim().isEmpty())
            throw new ChatApiException(HttpStatus.BAD_REQUEST, "Please enter a question.");

        String question = request.message.trim();
        if (question.length() > maxMessageCharacters)
            throw new ChatApiException(HttpStatus.PAYLOAD_TOO_LARGE,
                    "Your message is too long. Please keep it under " + maxMessageCharacters + " characters.");

        String session = request.sessionId == null || request.sessionId.trim().isEmpty()
                ? UUID.randomUUID().toString() : request.sessionId.trim();
        if (session.length() > 128)
            throw new ChatApiException(HttpStatus.BAD_REQUEST, "The conversation ID is invalid.");

        List<Message> history = conversations.computeIfAbsent(session,
                key -> Collections.synchronizedList(new ArrayList<Message>()));
        synchronized (history) {
            List<Message> context = new ArrayList<Message>(history);
            String answer;
            if (geminiAssistant.isConfigured()) {
                answer = geminiAssistant.answer(context, question);
            } else {
                answer = "Gemini is not configured, so this is offline troubleshooting guidance:\n\n"
                        + supportService.answer(question);
            }
            Message user = new Message("user", question);
            Message assistant = new Message("assistant", answer);
            history.add(user);
            history.add(assistant);
            while (history.size() > MAX_STORED_MESSAGES) {
                history.remove(0);
            }
            return new ChatResponse(session, assistant, new ArrayList<Message>(history));
        }
    }

    @GetMapping("/chat/{sessionId}")
    public List<Message> history(@PathVariable String sessionId) {
        List<Message> messages = conversations.get(sessionId);
        if (messages == null) return Collections.emptyList();
        synchronized (messages) { return new ArrayList<Message>(messages); }
    }

    @DeleteMapping("/chat/{sessionId}")
    public Map<String, Boolean> clear(@PathVariable String sessionId) {
        conversations.remove(sessionId);
        return Collections.singletonMap("cleared", true);
    }

    public static class ChatRequest { public String sessionId; public String message; }
    public static class Message {
        public String role;
        public String content;
        public long timestamp;
        public Message(String role, String content) {
            this.role = role; this.content = content; this.timestamp = System.currentTimeMillis();
        }
    }
    public static class ChatResponse {
        public String sessionId;
        public Message reply;
        public List<Message> messages;
        public ChatResponse(String id, Message reply, List<Message> messages) {
            this.sessionId = id; this.reply = reply; this.messages = messages;
        }
    }
    public static class ChatApiException extends RuntimeException {
        private final HttpStatus status;
        public ChatApiException(HttpStatus status, String message) { super(message); this.status = status; }
        public HttpStatus getStatus() { return status; }
    }
}
