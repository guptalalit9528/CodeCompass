package dev.codecompass;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class MultiModelService {
    private final GeminiAssistantService geminiAssistant;
    private final SupportService supportService;
    private final UsageTrackingService usageService;

    public MultiModelService(GeminiAssistantService geminiAssistant,
                             SupportService supportService,
                             UsageTrackingService usageService) {
        this.geminiAssistant = geminiAssistant;
        this.supportService = supportService;
        this.usageService = usageService;
    }

    public AdvancedDtos.MultiModelResponse compareModels(String userId, AdvancedDtos.MultiModelRequest request) {
        String prompt = request.prompt().trim();
        List<String> targetModels = request.models() != null && !request.models().isEmpty()
                ? request.models()
                : List.of("gemini-2.5-flash", "offline-reasoning");

        List<AdvancedDtos.ModelResult> results = new ArrayList<>();

        for (String model : targetModels) {
            long start = System.currentTimeMillis();
            String reply;
            String status = "SUCCESS";
            int promptTokens = Math.max(1, prompt.length() / 4);
            int completionTokens = 0;
            double cost = 0.0;

            try {
                if ("offline-reasoning".equalsIgnoreCase(model) || !geminiAssistant.isConfigured()) {
                    reply = "Offline Static Reasoning Engine:\n\n" + supportService.answer(prompt);
                } else {
                    reply = geminiAssistant.answer(List.of(), prompt);
                }
                completionTokens = Math.max(1, reply.length() / 4);
                // Gemini Flash pricing: $0.075 per 1M in, $0.30 per 1M out
                cost = (promptTokens * 0.000000075) + (completionTokens * 0.00000030);
            } catch (Exception ex) {
                reply = "Execution error for " + model + ": " + ex.getMessage();
                status = "FAILED";
            }

            long latency = System.currentTimeMillis() - start;
            results.add(new AdvancedDtos.ModelResult(model, reply, latency, promptTokens, completionTokens, cost, status));

            if (userId != null) {
                usageService.recordUsage(userId, "MULTI_MODEL", model, status, promptTokens, completionTokens);
            }
        }

        return new AdvancedDtos.MultiModelResponse(prompt, results);
    }
}
