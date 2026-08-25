package com.aaax.it;

import com.aaax.support.LoginSmokeAccounts;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.util.StreamUtils;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Login quality gate (infra slice): two fixed smoke accounts seedable via Testcontainers PG,
 * Redis container up for future full grant IT. Does not boot full UAA (Kafka/Liquibase) —
 * asserts identity store + BCrypt match that password-grant depends on.
 */
@Testcontainers(disabledWithoutDocker = true)
class LoginSmokeAccountsTestcontainersTest {

    @Container
    @SuppressWarnings("resource")
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(DockerImageName.parse("postgres:15-alpine"))
            .withDatabaseName("uaa_smoke")
            .withUsername("uaa")
            .withPassword("uaa");

    @Container
    @SuppressWarnings("resource")
    static final GenericContainer<?> REDIS = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder();

    @BeforeAll
    static void seedAccounts() throws Exception {
        String sql = StreamUtils.copyToString(
                new ClassPathResource("login-smoke/seed_login_smoke_accounts.sql").getInputStream(),
                StandardCharsets.UTF_8
        );
        try (Connection c = open()) {
            for (String stmt : sql.split(";")) {
                String s = stmt.trim();
                if (s.isEmpty() || s.startsWith("--")) {
                    continue;
                }
                // keep multi-line; strip line comments lightly
                try (var st = c.createStatement()) {
                    st.execute(s);
                }
            }
        }
    }

    private static Connection open() throws Exception {
        return DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
    }

    @Test
    @DisplayName("Testcontainers PG+Redis are running")
    void containers_running() {
        assertTrue(POSTGRES.isRunning());
        assertTrue(REDIS.isRunning());
        assertTrue(REDIS.getMappedPort(6379) > 0);
    }

    @Test
    @DisplayName("PRIMARY smoke account seeded and password matches BCrypt")
    void primary_seededAndPasswordMatches() throws Exception {
        assertAccount(LoginSmokeAccounts.PRIMARY);
    }

    @Test
    @DisplayName("SECONDARY smoke account seeded and password matches BCrypt")
    void secondary_seededAndPasswordMatches() throws Exception {
        assertAccount(LoginSmokeAccounts.SECONDARY);
    }

    @Test
    @DisplayName("PRIMARY mixed-case email still resolves same row (case-insensitive identifier)")
    void primary_mixedCaseLookup() throws Exception {
        try (Connection c = open();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT a.identifier, a.credentials FROM authentications a " +
                             "WHERE LOWER(a.identifier) = LOWER(?) AND a.login_type = 'EMAIL' AND a.is_active = true")) {
            ps.setString(1, LoginSmokeAccounts.PRIMARY_EMAIL_MIXED_CASE);
            try (ResultSet rs = ps.executeQuery()) {
                assertTrue(rs.next(), "mixed-case email must find PRIMARY auth row");
                assertEquals(LoginSmokeAccounts.PRIMARY.canonicalEmail(), rs.getString("identifier").toLowerCase());
                assertTrue(ENCODER.matches(LoginSmokeAccounts.PRIMARY.password(), rs.getString("credentials")));
                assertFalse(rs.next(), "exactly one auth row");
            }
        }
    }

    @Test
    @DisplayName("wrong password does not match PRIMARY hash")
    void primary_wrongPasswordRejected() throws Exception {
        try (Connection c = open();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT credentials FROM authentications WHERE identifier = ?")) {
            ps.setString(1, LoginSmokeAccounts.PRIMARY.canonicalEmail());
            try (ResultSet rs = ps.executeQuery()) {
                assertTrue(rs.next());
                assertFalse(ENCODER.matches("TotallyWrong!9", rs.getString("credentials")));
            }
        }
    }

    private static void assertAccount(LoginSmokeAccounts.Account account) throws Exception {
        try (Connection c = open();
             PreparedStatement ps = c.prepareStatement(
                     """
                             SELECT u.username, u.status, u.is_active AS user_active,
                                    a.identifier, a.login_type, a.credentials, a.is_active AS auth_active
                             FROM users u
                             JOIN authentications a ON a.user_id = u.id
                             WHERE LOWER(u.username) = LOWER(?)
                             """)) {
            ps.setString(1, account.email());
            try (ResultSet rs = ps.executeQuery()) {
                assertTrue(rs.next(), "missing seed for " + account.email());
                assertEquals(account.canonicalEmail(), rs.getString("username").toLowerCase());
                assertEquals("ACTIVE", rs.getString("status"));
                assertTrue(rs.getBoolean("user_active"));
                assertEquals(account.canonicalEmail(), rs.getString("identifier").toLowerCase());
                assertEquals("EMAIL", rs.getString("login_type"));
                assertTrue(rs.getBoolean("auth_active"));
                assertTrue(
                        ENCODER.matches(account.password(), rs.getString("credentials")),
                        "BCrypt must match password for " + account.email()
                );
            }
        }
    }
}
