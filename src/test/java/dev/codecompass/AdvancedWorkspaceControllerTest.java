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
        "spring.datasource.url=jdbc:h2:mem:codecompass-adv-test;DB_CLOSE_DELAY=-1",
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
class AdvancedWorkspaceControllerTest {
    @Autowired MockMvc mvc;
    @Autowired AppUserRepository users;
    @Autowired ConversationRepository conversations;
    @Autowired ConversationBranchRepository branches;
    @Autowired BranchCheckpointRepository checkpoints;
    @Autowired ContextItemRepository contextItems;
    @Autowired SharedCollaboratorRepository collaborators;
    @Autowired PasswordEncoder passwordEncoder;

    @MockitoBean GeminiAssistantService gemini;
    @MockitoBean JavaMailSender mailSender;

    @BeforeEach
    void clean() {
        collaborators.deleteAll();
        contextItems.deleteAll();
        checkpoints.deleteAll();
        branches.deleteAll();
        conversations.deleteAll();
        users.deleteAll();
        when(gemini.isConfigured()).thenReturn(true);
        when(gemini.answer(any(), any())).thenAnswer(call -> "AI Answer: " + call.getArgument(1));
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
    void testBranchingAndCheckpoints() throws Exception {
        AppUser user = createUser("Grace Dev", "grace@example.test", "Pass@1234");
        MockHttpSession session = login("grace@example.test", "Pass@1234");

        Conversation conv = conversations.save(new Conversation(user, null, "Debug Spring Security"));

        // Create branch
        MvcResult branchRes = mvc.perform(post("/api/conversations/" + conv.getId() + "/branches")
                        .session(session).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"jwt-migration\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("jwt-migration"))
                .andReturn();

        String branchId = JsonPath.read(branchRes.getResponse().getContentAsString(), "$.id");

        // List branches
        mvc.perform(get("/api/conversations/" + conv.getId() + "/branches").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("jwt-migration"));

        // Create checkpoint
        mvc.perform(post("/api/conversations/" + conv.getId() + "/checkpoints")
                        .session(session).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"branchId\":\"" + branchId + "\",\"title\":\"Filter Configured\",\"description\":\"CSRF disabled for tokens\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Filter Configured"));

        // List checkpoints
        mvc.perform(get("/api/conversations/" + conv.getId() + "/checkpoints").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Filter Configured"));
    }

    @Test
    void testContextPinningAndCollaborators() throws Exception {
        AppUser user = createUser("Heidi Dev", "heidi@example.test", "Pass@1234");
        MockHttpSession session = login("heidi@example.test", "Pass@1234");

        Conversation conv = conversations.save(new Conversation(user, null, "Architecture Discussion"));

        // Add Context Item
        MvcResult ctxRes = mvc.perform(post("/api/context")
                        .session(session).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"SecurityConfig.java\",\"type\":\"file\",\"content\":\"public class SecurityConfig {}\",\"conversationId\":\"" + conv.getId() + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("SecurityConfig.java"))
                .andExpect(jsonPath("$.pinned").value(true))
                .andReturn();

        String ctxId = JsonPath.read(ctxRes.getResponse().getContentAsString(), "$.id");

        // Toggle Pin
        mvc.perform(patch("/api/context/" + ctxId + "/pin").session(session).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pinned").value(false));

        // Invite Collaborator
        mvc.perform(post("/api/conversations/" + conv.getId() + "/collaborators")
                        .session(session).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"peer@example.test\",\"role\":\"editor\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.collaboratorEmail").value("peer@example.test"))
                .andExpect(jsonPath("$.role").value("editor"));
    }

    @Test
    void testAstAnalysisAndDiffComputation() throws Exception {
        AppUser user = createUser("Ivan Dev", "ivan@example.test", "Pass@1234");
        MockHttpSession session = login("ivan@example.test", "Pass@1234");

        String javaCode = "package com.example;\npublic class Calculator {\n  public int add(int a, int b) {\n    if (a < 0) return 0;\n    return a + b;\n  }\n}";

        // AST Analysis
        mvc.perform(post("/api/analysis/ast").session(session).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"" + javaCode.replace("\n", "\\n") + "\",\"language\":\"java\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.parsed").value(true))
                .andExpect(jsonPath("$.nodes[0].type").value("PACKAGE"));

        // Diff Analysis
        String orig = "int x = 1;\nint y = 2;";
        String mod = "int x = 1;\nint y = 5;\nint z = 10;";
        mvc.perform(post("/api/analysis/diff").session(session).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"originalCode\":\"" + orig.replace("\n", "\\n") + "\",\"modifiedCode\":\"" + mod.replace("\n", "\\n") + "\",\"filename\":\"App.java\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.filename").value("App.java"))
                .andExpect(jsonPath("$.additions").value(2));
    }

    @Test
    void testMultiModelComparison() throws Exception {
        AppUser user = createUser("Judy Dev", "judy@example.test", "Pass@1234");
        MockHttpSession session = login("judy@example.test", "Pass@1234");

        mvc.perform(post("/api/analysis/multi-model").session(session).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"prompt\":\"Explain quicksort\",\"models\":[\"gemini-2.5-flash\",\"offline-reasoning\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results").isArray())
                .andExpect(jsonPath("$.results[0].status").value("SUCCESS"))
                .andExpect(jsonPath("$.results[1].status").value("SUCCESS"));
    }
}
