package dev.codecompass;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Clock;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_preferences")
public class UserPreference {
    @Id
    @Column(name = "user_id", length = 36)
    private String userId;

    @Column(nullable = false, length = 16)
    private String theme;

    @Column(name = "code_style", nullable = false, length = 32)
    private String codeStyle;

    @Column(name = "preferred_language", nullable = false, length = 32)
    private String preferredLanguage;

    @Column(name = "font_size", nullable = false, length = 16)
    private String fontSize;

    @Column(name = "line_numbers", nullable = false)
    private boolean lineNumbers;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected UserPreference() {}

    public UserPreference(String userId) {
        this.userId = userId;
        this.theme = "dark";
        this.codeStyle = "concise";
        this.preferredLanguage = "Java";
        this.fontSize = "medium";
        this.lineNumbers = true;
        this.updatedAt = LocalDateTime.now(Clock.systemUTC());
    }

    public String getUserId() { return userId; }
    public String getTheme() { return theme; }
    public void setTheme(String theme) { this.theme = theme; touch(); }
    public String getCodeStyle() { return codeStyle; }
    public void setCodeStyle(String codeStyle) { this.codeStyle = codeStyle; touch(); }
    public String getPreferredLanguage() { return preferredLanguage; }
    public void setPreferredLanguage(String preferredLanguage) { this.preferredLanguage = preferredLanguage; touch(); }
    public String getFontSize() { return fontSize; }
    public void setFontSize(String fontSize) { this.fontSize = fontSize; touch(); }
    public boolean isLineNumbers() { return lineNumbers; }
    public void setLineNumbers(boolean lineNumbers) { this.lineNumbers = lineNumbers; touch(); }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    private void touch() {
        this.updatedAt = LocalDateTime.now(Clock.systemUTC());
    }
}
