package dev.codecompass;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class ChatControllerTest {
    private GeminiAssistantService gemini;
    private ChatController controller;

    @BeforeEach
    void setUp() {
        gemini = mock(GeminiAssistantService.class);
        controller = new ChatController(gemini, new SupportService(), 12000);
    }

    @Test
    void answersJavaSpringAndPreviouslyUnsupportedQuestionsWithTheAiAndKeepsApiShape() {
        when(gemini.isConfigured()).thenReturn(true);
        when(gemini.answer(anyList(), anyString())).thenAnswer(call -> "AI: " + call.getArgument(1));

        ChatController.ChatResponse java = controller.chat(request(null, "Why is my Java list null?"));
        ChatController.ChatResponse spring = controller.chat(request(java.sessionId, "Why does Spring return 404?"));
        ChatController.ChatResponse unsupported = controller.chat(request(java.sessionId, "Explain a Python async generator."));

        assertNotNull(java.sessionId);
        assertEquals("assistant", java.reply.role);
        assertEquals("AI: Why is my Java list null?", java.reply.content);
        assertEquals(6, unsupported.messages.size());
        assertEquals("AI: Explain a Python async generator.", unsupported.reply.content);
        verify(gemini).answer(argThat(List::isEmpty), eq("Why is my Java list null?"));
        verify(gemini).answer(argThat(history -> history.size() == 2), eq("Why does Spring return 404?"));
        verify(gemini).answer(argThat(history -> history.size() == 4), eq("Explain a Python async generator."));
        assertEquals("AI: Why does Spring return 404?", spring.reply.content);
    }

    @Test
    void rejectsEmptyAndOversizedMessagesBeforeCallingGemini() {
        ChatController.ChatRequest empty = request(null, "  ");
        ChatController.ChatApiException blank = assertThrows(ChatController.ChatApiException.class,
                () -> controller.chat(empty));
        assertEquals(HttpStatus.BAD_REQUEST, blank.getStatus());

        ChatController.ChatApiException longMessage = assertThrows(ChatController.ChatApiException.class,
                () -> controller.chat(request(null, repeat('x', 12001))));
        assertEquals(HttpStatus.PAYLOAD_TOO_LARGE, longMessage.getStatus());
        verifyNoInteractions(gemini);
    }

    @Test
    void savesRetrievesAndClearsConversationHistory() {
        when(gemini.isConfigured()).thenReturn(true);
        when(gemini.answer(anyList(), anyString())).thenReturn("An explanation.");
        ChatController.ChatResponse response = controller.chat(request(null, "How does SQL JOIN work?"));
        assertEquals(2, controller.history(response.sessionId).size());
        assertEquals(Boolean.TRUE, controller.clear(response.sessionId).get("cleared"));
        assertTrue(controller.history(response.sessionId).isEmpty());
    }

    @Test
    void labelsTheOfflineFallbackAndDoesNotSaveAProviderFailureAsAReply() {
        when(gemini.isConfigured()).thenReturn(false);
        ChatController.ChatResponse offline = controller.chat(request(null, "What is a NullPointerException?"));
        assertTrue(offline.reply.content.startsWith("Gemini is not configured"));

        when(gemini.isConfigured()).thenReturn(true);
        when(gemini.answer(anyList(), anyString())).thenThrow(new GeminiAssistantService.GeminiUnavailableException(
                HttpStatus.BAD_GATEWAY, "Gemini could not complete this request."));
        ChatController.ChatRequest next = request(offline.sessionId, "New request");
        assertThrows(GeminiAssistantService.GeminiUnavailableException.class, () -> controller.chat(next));
        assertEquals(2, controller.history(offline.sessionId).size());
    }

    @Test
    void errorResponseDoesNotExposeProviderDetailsOrSecrets() {
        String secret = "test-secret-must-not-be-returned";
        GeminiAssistantService.GeminiUnavailableException exception =
                new GeminiAssistantService.GeminiUnavailableException(HttpStatus.BAD_GATEWAY,
                        "Gemini could not complete this request. Check the API key and model configuration, then try again.");
        org.springframework.http.ResponseEntity<java.util.Map<String, String>> response =
                new ChatApiExceptionHandler().handleGeminiFailure(exception);
        assertEquals(502, response.getStatusCode().value());
        assertFalse(response.getBody().toString().contains(secret));
    }

    private ChatController.ChatRequest request(String sessionId, String message) {
        ChatController.ChatRequest request = new ChatController.ChatRequest();
        request.sessionId = sessionId;
        request.message = message;
        return request;
    }

    private String repeat(char value, int count) {
        StringBuilder result = new StringBuilder(count);
        for (int i = 0; i < count; i++) result.append(value);
        return result.toString();
    }
}
