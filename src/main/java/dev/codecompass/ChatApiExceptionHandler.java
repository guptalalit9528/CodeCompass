package dev.codecompass;

import java.util.Collections;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ChatApiExceptionHandler {
    @ExceptionHandler(ChatController.ChatApiException.class)
    public ResponseEntity<Map<String, String>> handleChatApiException(ChatController.ChatApiException exception) {
        return ResponseEntity.status(exception.getStatus())
                .body(Collections.singletonMap("error", exception.getMessage()));
    }

    @ExceptionHandler(GeminiAssistantService.GeminiUnavailableException.class)
    public ResponseEntity<Map<String, String>> handleGeminiFailure(GeminiAssistantService.GeminiUnavailableException exception) {
        return ResponseEntity.status(exception.getStatus())
                .body(Collections.singletonMap("error", exception.getMessage()));
    }
}
