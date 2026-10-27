package dev.codecompass;

import com.google.genai.Client;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.ai.google.genai.GoogleGenAiChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GeminiChatModelConfiguration {
    @Bean
    @Conditional(GeminiApiKeyCondition.class)
    public ChatModel codeCompassGeminiChatModel(
            @Value("${spring.ai.google.genai.api-key}") String apiKey,
            @Value("${spring.ai.google.genai.chat.model:gemini-2.5-flash}") String model,
            @Value("${spring.ai.google.genai.chat.temperature:0.2}") double temperature) {
        Client client = Client.builder().apiKey(apiKey).build();
        GoogleGenAiChatOptions options = GoogleGenAiChatOptions.builder()
                .model(model)
                .temperature(temperature)
                .build();
        return GoogleGenAiChatModel.builder()
                .genAiClient(client)
                .options(options)
                .build();
    }
}
