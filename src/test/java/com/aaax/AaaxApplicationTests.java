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
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
                .andExpect(jsonPath("$.version").value("0.3.0"));
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

        mockMvc.perform(post("/v1/accounts/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("alice"));

        mockMvc.perform(formLogin().user("alice").password("password123"))
                .andExpect(authenticated().withUsername("alice"));
    }

    @Test
    void registerRejectsDuplicateUsername() throws Exception {
        mockMvc.perform(post("/v1/accounts/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"demo\",\"password\":\"password123\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void demoAccountCanLogin() throws Exception {
        mockMvc.perform(formLogin().user("demo").password("demo1234"))
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
    void otpRequestVerifyAndLoginSession() throws Exception {
        mockMvc.perform(post("/v1/otp/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"demo\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("demo"));

        String code = otpStore.get("demo").code();

        MvcResult login = mockMvc.perform(post("/v1/auth/otp/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"demo\",\"code\":\"" + code + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("demo"))
                .andReturn();

        MockHttpSession session = (MockHttpSession) login.getRequest().getSession();
        assertThat(session).isNotNull();

        mockMvc.perform(get("/v1/accounts/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("demo"));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN", "USER"})
    void adminCanManageClients() throws Exception {
        mockMvc.perform(get("/v1/admin/clients"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].clientId").value("aaax-demo"));

        String createBody = """
                {
                  "clientId": "aaax-app-1",
                  "clientName": "App One",
                  "redirectUris": ["http://127.0.0.1:4000/callback"],
                  "scopes": ["openid", "api.read"],
                  "grantTypes": ["authorization_code", "refresh_token", "client_credentials"]
                }
                """;

        MvcResult created = mockMvc.perform(post("/v1/admin/clients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.client.clientId").value("aaax-app-1"))
                .andExpect(jsonPath("$.clientSecret").isNotEmpty())
                .andReturn();

        JsonNode json = objectMapper.readTree(created.getResponse().getContentAsString());
        String secret = json.get("clientSecret").asText();
        assertThat(secret).isNotBlank();

        mockMvc.perform(get("/v1/admin/clients/aaax-app-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clientName").value("App One"));

        mockMvc.perform(delete("/v1/admin/clients/aaax-app-1"))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/v1/admin/clients/aaax-app-1"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "demo", roles = "USER")
    void nonAdminCannotListClients() throws Exception {
        mockMvc.perform(get("/v1/admin/clients"))
                .andExpect(status().isForbidden());
    }

    @Test
    void oidcDiscoveryIsPublic() throws Exception {
        mockMvc.perform(get("/.well-known/openid-configuration"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.issuer").exists());
    }

    @Test
    void passwordResetFlowWorks() throws Exception {
        mockMvc.perform(post("/v1/accounts/password/forgot")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"usernameOrEmail\":\"demo\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accepted").value(true));

        String code = otpStore.get("reset:demo").code();
        mockMvc.perform(post("/v1/accounts/password/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"demo\",\"code\":\"" + code + "\",\"newPassword\":\"newpass123\"}"))
                .andExpect(status().isNoContent());

        mockMvc.perform(formLogin().user("demo").password("newpass123"))
                .andExpect(authenticated().withUsername("demo"));

        // restore seed password
        mockMvc.perform(post("/v1/accounts/password/forgot")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"usernameOrEmail\":\"demo\"}"))
                .andExpect(status().isOk());
        String code2 = otpStore.get("reset:demo").code();
        mockMvc.perform(post("/v1/accounts/password/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"demo\",\"code\":\"" + code2 + "\",\"newPassword\":\"demo1234\"}"))
                .andExpect(status().isNoContent());
    }

    @Test
    void uaaCompatRegistrationPathWorks() throws Exception {
        mockMvc.perform(post("/users/registrations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"compat1\",\"email\":\"c1@example.com\",\"password\":\"password123\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("compat1"));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN", "USER"})
    void adminCanListUsers() throws Exception {
        mockMvc.perform(get("/v1/admin/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").exists());
    }
}
