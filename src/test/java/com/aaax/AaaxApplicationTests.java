package com.aaax;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import com.aaax.otp.InMemoryOtpStore;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
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
                .andExpect(jsonPath("$.version").value("0.5.0-SNAPSHOT"))
                .andExpect(jsonPath("$.wedge").exists());
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
    void meReturnsCurrentAccount() throws Exception {
        mockMvc.perform(get("/v1/accounts/me").with(user("demo").roles("USER")))
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
                .asString();

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
                .andExpect(jsonPath("$.account.username").value("demo"))
                .andExpect(jsonPath("$.sessionId").exists())
                .andReturn();

        MockHttpSession session = (MockHttpSession) login.getRequest().getSession();
        assertThat(session).isNotNull();

        mockMvc.perform(get("/v1/accounts/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("demo"));
    }

    @Test
    void adminCanManageClients() throws Exception {
        mockMvc.perform(get("/v1/admin/clients").with(user("admin").roles("ADMIN", "USER")))
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
                        .with(user("admin").roles("ADMIN", "USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.client.clientId").value("aaax-app-1"))
                .andExpect(jsonPath("$.clientSecret").isNotEmpty())
                .andReturn();

        JsonNode json = objectMapper.readTree(created.getResponse().getContentAsString());
        String secret = json.get("clientSecret").asString();
        assertThat(secret).isNotBlank();

        mockMvc.perform(get("/v1/admin/clients/aaax-app-1").with(user("admin").roles("ADMIN", "USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clientName").value("App One"));

        mockMvc.perform(delete("/v1/admin/clients/aaax-app-1").with(user("admin").roles("ADMIN", "USER")))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/v1/admin/clients/aaax-app-1").with(user("admin").roles("ADMIN", "USER")))
                .andExpect(status().isNotFound());
    }

    @Test
    void nonAdminCannotListClients() throws Exception {
        mockMvc.perform(get("/v1/admin/clients").with(user("demo").roles("USER")))
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
    void adminCanListUsers() throws Exception {
        mockMvc.perform(get("/v1/admin/users").with(user("admin").roles("ADMIN", "USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").exists());
    }

    @Test
    void magicLinkRequestAndConsume() throws Exception {
        MvcResult req = mockMvc.perform(post("/v1/auth/magic/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identifier\":\"demo\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sent").value(true))
                .andExpect(jsonPath("$.devLink").exists())
                .andReturn();
        String body = req.getResponse().getContentAsString();
        String link = objectMapper.readTree(body).get("devLink").asText();
        String token = link.substring(link.indexOf("magic=") + 6);
        mockMvc.perform(post("/v1/auth/magic/consume")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + token + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.account.username").value("demo"))
                .andExpect(jsonPath("$.sessionId").exists());
    }

    @Test
    void socialProvidersPublicEndpoint() throws Exception {
        mockMvc.perform(get("/v1/auth/social/providers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false))
                .andExpect(jsonPath("$.providers").isArray());
    }

    @Test
    void passwordLoginApiEstablishesSession() throws Exception {
        MvcResult login = mockMvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"admin12345\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mfaRequired").value(false))
                .andExpect(jsonPath("$.account.username").value("admin"))
                .andReturn();
        MockHttpSession session = (MockHttpSession) login.getRequest().getSession();
        mockMvc.perform(get("/v1/admin/settings").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.features.adminPortal").value(true))
                .andExpect(jsonPath("$.features.identityEventBus").value(true))
                .andExpect(jsonPath("$.identityEventBus.enabled").value(true));
        mockMvc.perform(get("/v1/admin/events").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].type").value("com.aaax.auth.login"));
    }

    @Test
    void adminPortalIndexIsPublic() throws Exception {
        mockMvc.perform(get("/admin/index.html"))
                .andExpect(status().isOk());
    }

    @Test
    void bootstrapStatusPublic() throws Exception {
        mockMvc.perform(get("/v1/auth/bootstrap/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.needsBootstrap").exists());
    }
}
