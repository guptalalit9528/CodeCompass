package dev.codecompass;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;

public final class SettingsDtos {
    private SettingsDtos() {}

    public record ApiKeyDto(
            String id,
            String name,
            String keyPrefix,
            String scopes,
            LocalDateTime createdAt,
            LocalDateTime lastUsedAt,
            boolean revoked
    ) {
        public static ApiKeyDto from(ApiKey apiKey) {
            return new ApiKeyDto(
                    apiKey.getId(),
                    apiKey.getName(),
                    apiKey.getKeyPrefix(),
                    apiKey.getScopes(),
                    apiKey.getCreatedAt(),
                    apiKey.getLastUsedAt(),
                    apiKey.isRevoked()
            );
        }
    }

    public record CreateApiKeyRequest(
            @NotBlank @Size(max = 100) String name,
            @Size(max = 255) String scopes
    ) {}

    public record CreateApiKeyResponse(
            ApiKeyDto key,
            String secretKey
    ) {
        @Override public String toString() {
            return "CreateApiKeyResponse[key=" + key + ", secretKey=[REDACTED]]";
        }
    }

    public record UserPreferencesDto(
            String theme,
            String codeStyle,
            String preferredLanguage,
            String fontSize,
            boolean lineNumbers
    ) {
        public static UserPreferencesDto from(UserPreference pref) {
            return new UserPreferencesDto(
                    pref.getTheme(),
                    pref.getCodeStyle(),
                    pref.getPreferredLanguage(),
                    pref.getFontSize(),
                    pref.isLineNumbers()
            );
        }

        public static UserPreferencesDto defaults() {
            return new UserPreferencesDto("dark", "concise", "Java", "medium", true);
        }
    }

    public record UpdatePreferencesRequest(
            @Size(max = 16) String theme,
            @Size(max = 32) String codeStyle,
            @Size(max = 32) String preferredLanguage,
            @Size(max = 16) String fontSize,
            Boolean lineNumbers
    ) {}

    public record DailyUsageDto(String date, long requestCount, long totalTokens) {}

    public record RecentUsageDto(
            String id,
            String source,
            String modelName,
            String status,
            int promptTokens,
            int completionTokens,
            int totalTokens,
            LocalDateTime createdAt
    ) {
        public static RecentUsageDto from(UsageEvent event) {
            return new RecentUsageDto(
                    event.getId(),
                    event.getSource(),
                    event.getModelName(),
                    event.getStatus(),
                    event.getPromptTokens(),
                    event.getCompletionTokens(),
                    event.getTotalTokens(),
                    event.getCreatedAt()
            );
        }
    }

    public record UsageSummaryDto(
            long totalRequests,
            long successfulRequests,
            long failedRequests,
            long totalTokens,
            long webRequests,
            long apiKeyRequests,
            List<DailyUsageDto> dailyUsage,
            List<RecentUsageDto> recentEvents
    ) {}

    public record AccountDeletionRequest(
            @NotBlank String confirmation,
            @NotBlank @Size(max = 72) String currentPassword
    ) {
        @Override public String toString() {
            return "AccountDeletionRequest[confirmation=" + confirmation + ", currentPassword=[REDACTED]]";
        }
    }

    public record ChangeEmailRequest(
            @NotBlank @Email @Size(max = 254) String newEmail,
            @NotBlank @Size(max = 72) String currentPassword
    ) {
        @Override public String toString() {
            return "ChangeEmailRequest[newEmail=" + newEmail + ", currentPassword=[REDACTED]]";
        }
    }
}
