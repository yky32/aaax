package com.aaax;

import com.aaax.account.AccountRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AaaxApplicationTests {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    AccountRepository accountRepository;

    @Test
    void contextLoads() {
        assertThat(accountRepository.count()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void rootIsPublicProductMeta() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.product").value("AAAX"))
                .andExpect(jsonPath("$.endpoints.register").value("POST /v1/accounts/register"));
    }

    @Test
    void healthIsUp() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void registerCreatesAccountAndAllowsLogin() throws Exception {
        String body = """
                {
                  "username": "alice",
                  "email": "alice@example.com",
                  "password": "password123"
                }
                """;

        MvcResult result = mockMvc.perform(post("/v1/accounts/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("alice"))
                .andExpect(jsonPath("$.email").value("alice@example.com"))
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(json.has("password")).isFalse();
        assertThat(json.has("passwordHash")).isFalse();

        mockMvc.perform(formLogin().user("alice").password("password123"))
                .andExpect(authenticated().withUsername("alice"));
    }

    @Test
    void registerRejectsDuplicateUsername() throws Exception {
        String body = """
                {
                  "username": "demo",
                  "password": "password123"
                }
                """;

        mockMvc.perform(post("/v1/accounts/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("username already taken"));
    }

    @Test
    void registerValidatesPasswordLength() throws Exception {
        String body = """
                {
                  "username": "bob",
                  "password": "short"
                }
                """;

        mockMvc.perform(post("/v1/accounts/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("validation_failed"));
    }

    @Test
    void demoAccountCanLogin() throws Exception {
        mockMvc.perform(formLogin().user("demo").password("demo"))
                .andExpect(authenticated().withUsername("demo"));
    }

    @Test
    void badPasswordRejected() throws Exception {
        mockMvc.perform(formLogin().user("demo").password("wrong"))
                .andExpect(unauthenticated());
    }

    @Test
    @WithMockUser(username = "demo")
    void meReturnsCurrentAccount() throws Exception {
        mockMvc.perform(get("/v1/accounts/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("demo"))
                .andExpect(jsonPath("$.email").value("demo@aaax.local"));
    }

    @Test
    void meRequiresAuth() throws Exception {
        mockMvc.perform(get("/v1/accounts/me"))
                .andExpect(status().isUnauthorized());
    }
}
