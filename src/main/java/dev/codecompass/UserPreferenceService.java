package dev.codecompass;

import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserPreferenceService {
    private static final Set<String> ALLOWED_THEMES = Set.of("dark", "light", "system");
    private static final Set<String> ALLOWED_STYLES = Set.of("concise", "detailed", "step-by-step");
    private static final Set<String> ALLOWED_FONT_SIZES = Set.of("small", "medium", "large");

    private final UserPreferenceRepository repository;

    public UserPreferenceService(UserPreferenceRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public SettingsDtos.UserPreferencesDto getPreferences(String userId) {
        return repository.findById(userId)
                .map(SettingsDtos.UserPreferencesDto::from)
                .orElseGet(SettingsDtos.UserPreferencesDto::defaults);
    }

    @Transactional
    public SettingsDtos.UserPreferencesDto updatePreferences(String userId, SettingsDtos.UpdatePreferencesRequest request) {
        UserPreference pref = repository.findById(userId)
                .orElseGet(() -> new UserPreference(userId));

        if (request.theme() != null && ALLOWED_THEMES.contains(request.theme().toLowerCase())) {
            pref.setTheme(request.theme().toLowerCase());
        }
        if (request.codeStyle() != null && ALLOWED_STYLES.contains(request.codeStyle().toLowerCase())) {
            pref.setCodeStyle(request.codeStyle().toLowerCase());
        }
        if (request.preferredLanguage() != null && !request.preferredLanguage().isBlank()) {
            pref.setPreferredLanguage(request.preferredLanguage().trim());
        }
        if (request.fontSize() != null && ALLOWED_FONT_SIZES.contains(request.fontSize().toLowerCase())) {
            pref.setFontSize(request.fontSize().toLowerCase());
        }
        if (request.lineNumbers() != null) {
            pref.setLineNumbers(request.lineNumbers());
        }

        pref = repository.save(pref);
        return SettingsDtos.UserPreferencesDto.from(pref);
    }
}
