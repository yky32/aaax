package com.aaax.server.it;

import com.aaax.server.support.LoginSmokeAccounts;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.util.StreamUtils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Login smoke accounts on real Docker Postgres (+ Redis up).
 * Uses {@code docker} CLI (more reliable than Testcontainers on some Docker Desktop setups).
 */
@EnabledIf("dockerAvailable")
class LoginSmokeAccountsDockerTest {

    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder();
    private static final String PG_NAME = "uaa-login-smoke-pg";
    private static final String REDIS_NAME = "uaa-login-smoke-redis";
    private static String jdbcUrl;
    private static int redisPort;

    static boolean dockerAvailable() {
        try {
            Process p = new ProcessBuilder("docker", "info").redirectErrorStream(true).start();
            boolean finished = p.waitFor(20, TimeUnit.SECONDS);
            return finished && p.exitValue() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    @BeforeAll
    static void startDockerAndSeed() throws Exception {
        exec("docker", "rm", "-f", PG_NAME);
        exec("docker", "rm", "-f", REDIS_NAME);

        exec("docker", "run", "-d", "--rm",
                "--name", PG_NAME,
                "-e", "POSTGRES_DB=uaa_smoke",
                "-e", "POSTGRES_USER=uaa",
                "-e", "POSTGRES_PASSWORD=uaa",
                "-p", "55432:5432",
                "postgres:15-alpine");

        exec("docker", "run", "-d", "--rm",
                "--name", REDIS_NAME,
                "-p", "56379:6379",
                "redis:7-alpine");

        jdbcUrl = "jdbc:postgresql://127.0.0.1:55432/uaa_smoke";
        redisPort = 56379;

        waitForPostgres();
        seedSql();
    }

    @AfterAll
    static void stopDocker() {
        try {
            exec("docker", "rm", "-f", PG_NAME);
            exec("docker", "rm", "-f", REDIS_NAME);
        } catch (Exception ignored) {
            // best-effort cleanup
        }
    }

    private static void waitForPostgres() throws Exception {
        Exception last = null;
        for (int i = 0; i < 40; i++) {
            try (Connection c = open()) {
                try (var st = c.createStatement(); ResultSet rs = st.executeQuery("SELECT 1")) {
                    assertTrue(rs.next());
                    return;
                }
            } catch (Exception e) {
                last = e;
                Thread.sleep(500);
            }
        }
        throw new IllegalStateException("Postgres not ready", last);
    }

    private static void seedSql() throws Exception {
        String sql = StreamUtils.copyToString(
                new ClassPathResource("login-smoke/seed_login_smoke_accounts.sql").getInputStream(),
                StandardCharsets.UTF_8
        );
        // Write to temp and docker exec -i psql for reliable multi-statement
        Path tmp = Files.createTempFile("uaa-smoke-seed", ".sql");
        Files.writeString(tmp, sql);
        try {
            Process p = new ProcessBuilder(
                    "docker", "exec", "-i", PG_NAME,
                    "psql", "-U", "uaa", "-d", "uaa_smoke", "-v", "ON_ERROR_STOP=1"
            ).redirectErrorStream(true).start();
            try (var in = Files.newInputStream(tmp); var out = p.getOutputStream()) {
                in.transferTo(out);
            }
            String log = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))
                    .lines().collect(Collectors.joining("\n"));
            boolean ok = p.waitFor(60, TimeUnit.SECONDS);
            if (!ok || p.exitValue() != 0) {
                throw new IllegalStateException("seed failed exit=" + p.exitValue() + " log=" + log);
            }
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    private static Connection open() throws Exception {
        return DriverManager.getConnection(jdbcUrl, "uaa", "uaa");
    }

    private static void exec(String... cmd) throws Exception {
        Process p = new ProcessBuilder(cmd).redirectErrorStream(true).start();
        String log = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))
                .lines().collect(Collectors.joining("\n"));
        boolean finished = p.waitFor(120, TimeUnit.SECONDS);
        if (!finished) {
            p.destroyForcibly();
            throw new IllegalStateException("timeout: " + String.join(" ", cmd));
        }
        // rm -f may exit 1 if missing — ignore for rm
        if (p.exitValue() != 0 && !(cmd.length >= 2 && "rm".equals(cmd[1]))) {
            throw new IllegalStateException("cmd failed " + String.join(" ", cmd) + "\n" + log);
        }
    }

    @Test
    @DisplayName("Docker PG+Redis up for smoke accounts")
    void containers_up() throws Exception {
        assertNotNull(jdbcUrl);
        assertEquals(56379, redisPort);
        try (Connection c = open(); var st = c.createStatement(); ResultSet rs = st.executeQuery("SELECT 1")) {
            assertTrue(rs.next());
        }
        Process p = new ProcessBuilder("docker", "exec", REDIS_NAME, "redis-cli", "PING")
                .redirectErrorStream(true).start();
        String out = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))
                .lines().collect(Collectors.joining()).trim();
        assertTrue(p.waitFor(10, TimeUnit.SECONDS));
        assertEquals(0, p.exitValue());
        assertTrue(out.toUpperCase(Locale.ROOT).contains("PONG"));
    }

    @Test
    @DisplayName("PRIMARY smoke account seeded + BCrypt matches")
    void primary() throws Exception {
        assertAccount(LoginSmokeAccounts.PRIMARY);
    }

    @Test
    @DisplayName("SECONDARY smoke account seeded + BCrypt matches")
    void secondary() throws Exception {
        assertAccount(LoginSmokeAccounts.SECONDARY);
    }

    @Test
    @DisplayName("mixed-case PRIMARY email resolves same auth row")
    void primaryMixedCase() throws Exception {
        try (Connection c = open();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT identifier, credentials FROM authentications " +
                             "WHERE LOWER(identifier) = LOWER(?) AND login_type = 'EMAIL' AND is_active = true")) {
            ps.setString(1, LoginSmokeAccounts.PRIMARY_EMAIL_MIXED_CASE);
            try (ResultSet rs = ps.executeQuery()) {
                assertTrue(rs.next());
                assertEquals(LoginSmokeAccounts.PRIMARY.canonicalEmail(), rs.getString(1).toLowerCase(Locale.ROOT));
                assertTrue(ENCODER.matches(LoginSmokeAccounts.PRIMARY.password(), rs.getString(2)));
            }
        }
    }

    @Test
    @DisplayName("wrong password does not match PRIMARY")
    void wrongPassword() throws Exception {
        try (Connection c = open();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT credentials FROM authentications WHERE identifier = ?")) {
            ps.setString(1, LoginSmokeAccounts.PRIMARY.canonicalEmail());
            try (ResultSet rs = ps.executeQuery()) {
                assertTrue(rs.next());
                assertFalse(ENCODER.matches("WrongPass!0", rs.getString(1)));
            }
        }
    }

    private static void assertAccount(LoginSmokeAccounts.Account account) throws Exception {
        try (Connection c = open();
             PreparedStatement ps = c.prepareStatement(
                     """
                             SELECT u.username, u.status, u.is_active, a.identifier, a.login_type, a.credentials, a.is_active
                             FROM users u
                             JOIN authentications a ON a.user_id = u.id
                             WHERE LOWER(u.username) = LOWER(?)
                             """)) {
            ps.setString(1, account.email());
            try (ResultSet rs = ps.executeQuery()) {
                assertTrue(rs.next(), "seed missing " + account.email());
                assertEquals(account.canonicalEmail(), rs.getString(1).toLowerCase(Locale.ROOT));
                assertEquals("ACTIVE", rs.getString(2));
                assertTrue(rs.getBoolean(3));
                assertEquals(account.canonicalEmail(), rs.getString(4).toLowerCase(Locale.ROOT));
                assertEquals("EMAIL", rs.getString(5));
                assertTrue(ENCODER.matches(account.password(), rs.getString(6)));
                assertTrue(rs.getBoolean(7));
            }
        }
    }
}
