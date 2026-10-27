package dev.codecompass;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.mock.web.MockHttpSession;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:codecompass-test;DB_CLOSE_DELAY=-1",
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
class ChatControllerTest {
    @Autowired MockMvc mvc;
    @Autowired AppUserRepository users;
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
        messages.deleteAll();
        conversations.deleteAll();
        projects.deleteAll();
        users.deleteAll();
        when(gemini.isConfigured()).thenReturn(true);
        when(gemini.answer(any(), any(), any())).thenAnswer(call -> "AI: " + call.getArgument(1));
    }

    @Test
    void protectedEndpointsRequireSessionAndMutationsRequireCsrf() throws Exception {
        mvc.perform(get("/api/projects")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"one@example.test\",\"password\":\"long-enough-password\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void registerRejectDuplicateVerifyLoginAndLogoutInvalidatesSession() throws Exception {
        String body = "{\"name\":\"Ada Lovelace\",\"email\":\"ADA@example.test\",\"password\":\"sixteen-char-pass\"}";
        mvc.perform(post("/api/auth/register").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isAccepted());
        AppUser user = users.findByEmailIgnoreCase("ada@example.test").orElseThrow();
        org.junit.jupiter.api.Assertions.assertFalse(user.isEnabled());
        org.junit.jupiter.api.Assertions.assertNotEquals("sixteen-char-pass", user.getPasswordHash());
        mvc.perform(post("/api/auth/register").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());

        org.mockito.ArgumentCaptor<SimpleMailMessage> email = org.mockito.ArgumentCaptor.forClass(SimpleMailMessage.class);
        org.mockito.Mockito.verify(mailSender).send(email.capture());
        Matcher match = Pattern.compile("verifyToken=([^\\s]+)").matcher(email.getValue().getText());
        org.junit.jupiter.api.Assertions.assertTrue(match.find());
        mvc.perform(post("/api/auth/verify-email").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + match.group(1) + "\"}"))
                .andExpect(status().isOk());

        MockHttpSession session = login("ada@example.test", "sixteen-char-pass");
        mvc.perform(get("/api/auth/me").session(session)).andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("ada@example.test"));
        mvc.perform(post("/api/auth/logout").session(session).with(csrf())).andExpect(status().isNoContent());
        mvc.perform(get("/api/auth/me").session(session)).andExpect(status().isUnauthorized());
    }

    @Test
    void newPasswordsRequireSixToEighteenCharactersAndLegacyBcryptLoginStillWorks() throws Exception {
        mvc.perform(registerJson("short@example.test", "12345"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("Password must be between 6 and 18 characters."));
        mvc.perform(registerJson("empty@example.test", ""))
                .andExpect(status().isBadRequest());
        mvc.perform(registerJson("long@example.test", "1234567890123456789"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("Password must be between 6 and 18 characters."));

        String[] accepted = {"123456", "passWORD", "123456789012345678", "éééééééééééééééééé"};
        for (int i = 0; i < accepted.length; i++) {
            mvc.perform(registerJson("valid" + i + "@example.test", accepted[i]))
                    .andExpect(status().isAccepted());
        }

        // Historical users have direct BCrypt hashes; continue to accept those unchanged.
        String legacyPassword = "old-password-that-was-already-registered";
        users.saveAndFlush(verified("legacy@example.test", legacyPassword));
        MockHttpSession session = login("legacy@example.test", legacyPassword);
        mvc.perform(get("/api/auth/me").session(session)).andExpect(status().isOk());
        mvc.perform(post("/api/auth/change-password").session(session).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"" + legacyPassword + "\",\"newPassword\":\"12345\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/auth/change-password").session(session).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"" + legacyPassword + "\",\"newPassword\":\"changed8\"}"))
                .andExpect(status().isOk());
        login("legacy@example.test", "changed8");
        mvc.perform(post("/api/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"legacy@example.test\",\"password\":\"wrong-password\"}"))
                .andExpect(status().isUnauthorized());

        String legacy72Bytes = "a".repeat(66) + "ééé";
        users.saveAndFlush(verified("byte-limit@example.test", legacy72Bytes));
        mvc.perform(post("/api/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"byte-limit@example.test\",\"password\":\"" + legacy72Bytes + "x\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void resetPasswordUsesTheSameSixToEighteenCharacterLimit() throws Exception {
        mvc.perform(post("/api/auth/reset-password").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"unused\",\"newPassword\":\"12345\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Password must be between 6 and 18 characters."));
        mvc.perform(post("/api/auth/reset-password").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"unused\",\"newPassword\":\"1234567890123456789\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Password must be between 6 and 18 characters."));
    }

    @Test
    void usersCannotReadOrDeleteAnotherUsersProjectsOrConversations() throws Exception {
        users.saveAndFlush(verified("a@example.test", "password-for-a"));
        users.saveAndFlush(verified("b@example.test", "password-for-b"));
        MockHttpSession a = login("a@example.test", "password-for-a");
        MockHttpSession b = login("b@example.test", "password-for-b");

        MvcResult createdProject = mvc.perform(post("/api/projects").session(a).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Java Learning\",\"instructions\":\"Explain simply\"}"))
                .andExpect(status().isCreated()).andReturn();
        String projectId = com.jayway.jsonpath.JsonPath.read(createdProject.getResponse().getContentAsString(), "$.id");
        mvc.perform(get("/api/projects/" + projectId).session(b)).andExpect(status().isNotFound());

        MvcResult createdChat = mvc.perform(post("/api/chat").session(a).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"Explain Java records\",\"projectId\":\"" + projectId + "\"}"))
                .andExpect(status().isOk()).andReturn();
        String conversationId = com.jayway.jsonpath.JsonPath.read(createdChat.getResponse().getContentAsString(), "$.sessionId");
        mvc.perform(get("/api/chat/" + conversationId).session(b)).andExpect(status().isNotFound());
        mvc.perform(delete("/api/chat/" + conversationId).session(b).with(csrf())).andExpect(status().isNotFound());

        mvc.perform(patch("/api/projects/" + projectId).session(a).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Java Basics\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Java Basics"));
        mvc.perform(delete("/api/projects/" + projectId).session(a).with(csrf())).andExpect(status().isOk());
        org.junit.jupiter.api.Assertions.assertNull(conversations.findById(conversationId).orElseThrow().getProject());
        org.junit.jupiter.api.Assertions.assertEquals(2, messages.findAll().size());
    }

    @Test
    void passwordResetChangesHashAndConsumesTheOneTimeLink() throws Exception {
        AppUser user = users.saveAndFlush(verified("reset@example.test", "old-password-value"));
        mvc.perform(post("/api/auth/forgot-password").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"reset@example.test\"}"))
                .andExpect(status().isOk());
        org.mockito.ArgumentCaptor<SimpleMailMessage> email = org.mockito.ArgumentCaptor.forClass(SimpleMailMessage.class);
        org.mockito.Mockito.verify(mailSender).send(email.capture());
        Matcher match = Pattern.compile("resetToken=([^\\s]+)").matcher(email.getValue().getText());
        org.junit.jupiter.api.Assertions.assertTrue(match.find());
        String resetRequest = "{\"token\":\"" + match.group(1) + "\",\"newPassword\":\"new-password-value\"}";
        mvc.perform(post("/api/auth/reset-password").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(resetRequest))
                .andExpect(status().isOk());
        org.junit.jupiter.api.Assertions.assertTrue(passwordEncoder.matches("new-password-value",
                users.findById(user.getId()).orElseThrow().getPasswordHash()));
        mvc.perform(post("/api/auth/reset-password").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(resetRequest))
                .andExpect(status().isBadRequest());
    }

    @Test
    void chatHistoryPersistsAndMessagesAreClearedWithoutDeletingConversation() throws Exception {
        users.saveAndFlush(verified("chat@example.test", "chat-password-value"));
        MockHttpSession session = login("chat@example.test", "chat-password-value");
        MvcResult result = mvc.perform(post("/api/chat").session(session).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"How do I use a Java map?\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.reply.content").value("AI: How do I use a Java map?"))
                .andReturn();
        String conversationId = com.jayway.jsonpath.JsonPath.read(result.getResponse().getContentAsString(), "$.sessionId");
        mvc.perform(get("/api/chat/" + conversationId).session(session)).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].role").value("user")).andExpect(jsonPath("$[1].role").value("assistant"));
        mvc.perform(delete("/api/chat/" + conversationId).session(session).with(csrf())).andExpect(status().isOk());
        org.junit.jupiter.api.Assertions.assertTrue(conversations.existsById(conversationId));
        org.junit.jupiter.api.Assertions.assertTrue(messages.findAllByConversation_Id(conversationId, org.springframework.data.domain.Sort.unsorted()).isEmpty());
    }

    @Test
    void registerWithDevFallbackWhenSmtpFails() throws Exception {
        org.mockito.Mockito.doThrow(new org.springframework.mail.MailSendException("535 5.7.8 Authentication failed"))
                .when(mailSender).send(any(SimpleMailMessage.class));

        String body = "{\"name\":\"Grace Hopper\",\"email\":\"grace@example.test\",\"password\":\"hopper-password\"}";
        mvc.perform(post("/api/auth/register").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isAccepted());

        AppUser user = users.findByEmailIgnoreCase("grace@example.test").orElseThrow();
        org.junit.jupiter.api.Assertions.assertFalse(user.isEnabled());
    }

    private MockHttpSession login(String email, String password) throws Exception {
        MvcResult result = mvc.perform(post("/api/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk()).andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder registerJson(String email, String password) {
        return post("/api/auth/register").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Test User\",\"email\":\"" + email + "\",\"password\":\"" + password + "\"}");
    }

    private AppUser verified(String email, String password) {
        AppUser user = new AppUser("Test User", email, passwordEncoder.encode(password));
        user.setEnabled(true);
        return user;
    }
}
