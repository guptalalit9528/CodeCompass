package dev.codecompass;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/settings", "/api"})
public class SettingsController {
    private final AuthService authService;
    private final ApiKeyService apiKeyService;
    private final UserPreferenceService preferenceService;
    private final UsageTrackingService usageService;
    private final AccountManagementService accountService;

    public SettingsController(AuthService authService,
                              ApiKeyService apiKeyService,
                              UserPreferenceService preferenceService,
                              UsageTrackingService usageService,
                              AccountManagementService accountService) {
        this.authService = authService;
        this.apiKeyService = apiKeyService;
        this.preferenceService = preferenceService;
        this.usageService = usageService;
        this.accountService = accountService;
    }

    public record DetailedProfileResponse(
            String id,
            String name,
            String email,
            boolean enabled,
            LocalDateTime createdAt,
            String role,
            String plan
    ) {
        public static DetailedProfileResponse from(AppUser user) {
            return new DetailedProfileResponse(
                    user.getId(),
                    user.getName(),
                    user.getEmail(),
                    user.isEnabled(),
                    user.getCreatedAt(),
                    "Developer",
                    "Free Developer Tier"
            );
        }
    }

    @GetMapping({"/settings/profile", "/users/me"})
    public DetailedProfileResponse profile(@AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = authService.currentUser(principal.getId());
        return DetailedProfileResponse.from(user);
    }

    @PatchMapping({"/settings/profile", "/users/me"})
    public DetailedProfileResponse updateProfile(@AuthenticationPrincipal UserPrincipal principal,
                                                 @Valid @RequestBody AuthDtos.UpdateProfileRequest request) {
        AppUser updated = authService.updateName(principal.getId(), request.name());
        return DetailedProfileResponse.from(updated);
    }

    @PostMapping("/settings/change-email")
    public DetailedProfileResponse changeEmail(@AuthenticationPrincipal UserPrincipal principal,
                                               @Valid @RequestBody SettingsDtos.ChangeEmailRequest request) {
        AppUser updated = accountService.changeEmail(principal.getId(), request.newEmail(), request.currentPassword());
        return DetailedProfileResponse.from(updated);
    }

    @GetMapping({"/settings/api-keys", "/api-keys"})
    public List<SettingsDtos.ApiKeyDto> listApiKeys(@AuthenticationPrincipal UserPrincipal principal) {
        return apiKeyService.listKeys(principal.getId());
    }

    @PostMapping({"/settings/api-keys", "/api-keys"})
    @ResponseStatus(HttpStatus.CREATED)
    public SettingsDtos.CreateApiKeyResponse createApiKey(@AuthenticationPrincipal UserPrincipal principal,
                                                         @Valid @RequestBody SettingsDtos.CreateApiKeyRequest request) {
        return apiKeyService.createKey(principal.getId(), request);
    }

    @DeleteMapping({"/settings/api-keys/{id}", "/api-keys/{id}"})
    public Map<String, Boolean> revokeApiKey(@AuthenticationPrincipal UserPrincipal principal,
                                            @PathVariable String id) {
        apiKeyService.revokeKey(principal.getId(), id);
        return Map.of("revoked", true);
    }

    @GetMapping("/settings/preferences")
    public SettingsDtos.UserPreferencesDto getPreferences(@AuthenticationPrincipal UserPrincipal principal) {
        return preferenceService.getPreferences(principal.getId());
    }

    @PatchMapping("/settings/preferences")
    public SettingsDtos.UserPreferencesDto updatePreferences(@AuthenticationPrincipal UserPrincipal principal,
                                                            @Valid @RequestBody SettingsDtos.UpdatePreferencesRequest request) {
        return preferenceService.updatePreferences(principal.getId(), request);
    }

    @GetMapping({"/settings/usage", "/usage"})
    public SettingsDtos.UsageSummaryDto getUsage(@AuthenticationPrincipal UserPrincipal principal) {
        return usageService.getUsageSummary(principal.getId());
    }

    @DeleteMapping("/settings/account")
    public Map<String, Boolean> deleteAccount(@AuthenticationPrincipal UserPrincipal principal,
                                              @Valid @RequestBody SettingsDtos.AccountDeletionRequest request,
                                              HttpServletRequest servletRequest) {
        accountService.deleteAccount(principal.getId(), request.confirmation(), request.currentPassword());
        SecurityContextHolder.clearContext();
        var session = servletRequest.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return Map.of("deleted", true);
    }
}
