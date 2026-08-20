package com.aaax;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import com.aaax.otp.InMemoryOtpStore;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
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
    InMemoryOtpStore otpStore;

    @Test
    void contextLoads() {
    }

    @Test
    void rootIsPublicProductMeta() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.product").value("AAAX"))
                .andExpect(jsonPath("$.endpoints.apiHello").exists());
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
                .andExpect(jsonPath("$.roles[0]").value("USER"))
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
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
                .andExpect(status().isConflict());
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
                .andExpect(jsonPath("$.username").value("demo"));
    }

    @Test
    void meRequiresAuth() throws Exception {
        mockMvc.perform(get("/v1/accounts/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void clientCredentialsTokenCanCallProtectedApi() throws Exception {
        String basic = Base64.getEncoder()
                .encodeToString("aaax-demo:aaax-demo-secret".getBytes(StandardCharsets.UTF_8));

        MvcResult tokenResult = mockMvc.perform(post("/oauth2/token")
                        .header(HttpHeaders.AUTHORIZATION, "Basic " + basic)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .content("grant_type=client_credentials&scope=api.read"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token").isNotEmpty())
                .andReturn();

        String accessToken = objectMapper
                .readTree(tokenResult.getResponse().getContentAsString())
                .get("access_token")
                .asText();

        mockMvc.perform(get("/v1/api/hello")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("hello from AAAX protected API"));
    }

    @Test
    void protectedApiWithoutTokenIsUnauthorized() throws Exception {
        mockMvc.perform(get("/v1/api/hello"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void otpRequestAndVerify() throws Exception {
        mockMvc.perform(post("/v1/otp/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"demo\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("demo"));

        String code = otpStore.get("demo").code();
        assertThat(code).hasSize(6);

        mockMvc.perform(post("/v1/otp/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"demo\",\"code\":\"" + code + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true));
    }

    @Test
    void oidcDiscoveryIsPublic() throws Exception {
        mockMvc.perform(get("/.well-known/openid-configuration"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.issuer").exists())
                .andExpect(jsonPath("$.token_endpoint").exists());
    }
}
