package dev.codecompass;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:codecompass-settings-test;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.flyway.enabled=true",
        "spring.ai.model.chat=none",
        "MAIL_HOST=mail.test",
        "spring.mail.host=mail.test",
        "codecompass.auth.login.max-attempts=10"
})
@AutoConfigureMockMvc
class SettingsControllerTest {
    @Autowired MockMvc mvc;
    @Autowired AppUserRepository users;
    @Autowired ApiKeyRepository apiKeys;
    @Autowired UserPreferenceRepository preferences;
    @Autowired UsageEventRepository usageEvents;
    @Autowired ProjectRepository projects;
    @Autowired ConversationRepository conversations;
    @Autowired ChatMessageRepository messages;
    @Autowired AuthTokenRepository tokens;
    @Autowired PasswordEncoder passwordEncoder;

    @MockitoBean GeminiAssistantService gemini;
    @MockitoBean JavaMailSender mailSender;

    @BeforeEach
    void clean() {
        tokens.deleteAll();
        apiKeys.deleteAll();
        usageEvents.deleteAll();
        preferences.deleteAll();
        messages.deleteAll();
        conversations.deleteAll();
        projects.deleteAll();
        users.deleteAll();
        when(gemini.isConfigured()).thenReturn(true);
        when(gemini.answer(any(), any(), any())).thenAnswer(call -> "AI Reply: " + call.getArgument(1));
    }

    private AppUser createUser(String name, String email, String password) {
        AppUser user = new AppUser(name, email, passwordEncoder.encode(password));
        user.setEnabled(true);
        return users.saveAndFlush(user);
    }

    private MockHttpSession login(String email, String password) throws Exception {
        MvcResult result = mvc.perform(post("/api/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }

    @Test
    void unauthenticatedSettingsEndpointsReturnUnauthorized() throws Exception {
        mvc.perform(get("/api/settings/profile")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/settings/api-keys")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/settings/preferences")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/settings/usage")).andExpect(status().isUnauthorized());
    }

    @Test
    void userCanViewAndEditProfile() throws Exception {
        createUser("Alice Dev", "alice@example.test", "Pass@1234");
        MockHttpSession session = login("alice@example.test", "Pass@1234");

        mvc.perform(get("/api/settings/profile").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Alice Dev"))
                .andExpect(jsonPath("$.email").value("alice@example.test"))
                .andExpect(jsonPath("$.role").value("Developer"))
                .andExpect(jsonPath("$.plan").value("Free Developer Tier"));

        mvc.perform(patch("/api/settings/profile").session(session).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Alice Engineer\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Alice Engineer"));
    }

    @Test
    void userCanCreateListAndRevokeApiKeys() throws Exception {
        createUser("Bob Dev", "bob@example.test", "Pass@1234");
        MockHttpSession session = login("bob@example.test", "Pass@1234");

        MvcResult createRes = mvc.perform(post("/api/settings/api-keys").session(session).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"CLI Token\",\"scopes\":\"chat:write\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.key.name").value("CLI Token"))
                .andExpect(jsonPath("$.secretKey").isNotEmpty())
                .andReturn();

        String secretKey = JsonPath.read(createRes.getResponse().getContentAsString(), "$.secretKey");
        String keyId = JsonPath.read(createRes.getResponse().getContentAsString(), "$.key.id");
        org.junit.jupiter.api.Assertions.assertTrue(secretKey.startsWith("cc_live_"));

        // Listing keys should NEVER expose secretKey
        mvc.perform(get("/api/settings/api-keys").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("CLI Token"))
                .andExpect(jsonPath("$[0].secretKey").doesNotExist());

        // Revoking key
        mvc.perform(delete("/api/settings/api-keys/" + keyId).session(session).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.revoked").value(true));

        mvc.perform(get("/api/settings/api-keys").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].revoked").value(true));
    }

    @Test
    void apiKeyCanAuthenticateChatRequestsProgrammatically() throws Exception {
        createUser("Charlie Dev", "charlie@example.test", "Pass@1234");
        MockHttpSession session = login("charlie@example.test", "Pass@1234");

        MvcResult createRes = mvc.perform(post("/api/settings/api-keys").session(session).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Automation Key\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        String secretKey = JsonPath.read(createRes.getResponse().getContentAsString(), "$.secretKey");

        // Use API Key header without session and without CSRF token
        mvc.perform(post("/api/chat")
                        .header("X-API-Key", secretKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"Hello CodeCompass!\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reply.content").value("AI Reply: Hello CodeCompass!"));

        // Check usage metrics
        mvc.perform(get("/api/settings/usage").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRequests").value(1))
                .andExpect(jsonPath("$.apiKeyRequests").value(1))
                .andExpect(jsonPath("$.recentEvents[0].source").value("API_KEY"));
    }

    @Test
    void userPreferencesPersistAndReturnDefaults() throws Exception {
        createUser("Diana Dev", "diana@example.test", "Pass@1234");
        MockHttpSession session = login("diana@example.test", "Pass@1234");

        mvc.perform(get("/api/settings/preferences").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.theme").value("dark"))
                .andExpect(jsonPath("$.codeStyle").value("concise"))
                .andExpect(jsonPath("$.preferredLanguage").value("Java"));

        mvc.perform(patch("/api/settings/preferences").session(session).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"theme\":\"light\",\"codeStyle\":\"step-by-step\",\"preferredLanguage\":\"TypeScript\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.theme").value("light"))
                .andExpect(jsonPath("$.codeStyle").value("step-by-step"))
                .andExpect(jsonPath("$.preferredLanguage").value("TypeScript"));
    }

    @Test
    void accountDeletionRequiresCorrectPasswordAndCleansResources() throws Exception {
        AppUser user = createUser("Evan Dev", "evan@example.test", "Pass@1234");
        MockHttpSession session = login("evan@example.test", "Pass@1234");

        // Wrong password rejected
        mvc.perform(delete("/api/settings/account").session(session).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"confirmation\":\"DELETE\",\"currentPassword\":\"WrongPassword\"}"))
                .andExpect(status().isBadRequest());

        // Wrong confirmation rejected
        mvc.perform(delete("/api/settings/account").session(session).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"confirmation\":\"NOPE\",\"currentPassword\":\"Pass@1234\"}"))
                .andExpect(status().isBadRequest());

        // Correct password & confirmation
        mvc.perform(delete("/api/settings/account").session(session).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"confirmation\":\"DELETE\",\"currentPassword\":\"Pass@1234\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deleted").value(true));

        org.junit.jupiter.api.Assertions.assertFalse(users.existsByEmailIgnoreCase("evan@example.test"));
    }
}
