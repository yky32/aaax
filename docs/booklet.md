# AAAX Booklet

**Accounts · Authentication · Authorization · eXperiences**

| | |
|--|--|
| **This file** | Product + eng source of truth — **code wins if this drifts** |
| **Repo** | https://github.com/yky32/aaax |
| **Version** | `0.9.0-SNAPSHOT` on `main` |
| **Stack** | JDK **21** · Spring Boot **4.1.1** · Apache-2.0 |
| **Local** | `~/Documents/git/personal/aaax` |
| **Updated** | 2026-08-28 |

> Root `README.md` = shop window (five-minute local).  
> Other files under `docs/` are stubs that point here.

---

## 1. What this is

**AAAX** is a self-host OpenID Connect authentication server in one public Maven jar.

- Packages: `com.aaax.core` (foundation) · `com.aaax.server` (authentication server)
- Main: `com.aaax.server.App`
- Identity: `User` 1:n `Authentication` (`loginType` + `identifier`)
- Errors: `BizException(Response)` → `BaseGlobalExceptionHandler` → `R` / `Result`

It is **not** a Clerk/Logto clone, **not** a Keycloak fork, **not** the official Spring Authorization Server sample.

**ICP:** Spring/JVM teams that want a native Java authentication server without private packages.

**Bet:** self-host OIDC-grade AAA with Endpoint → UseCase → Repository layering.

---

## 2. Honest status (0.9)

| | |
|--|--|
| Single jar, Central Maven, no private `app-core` | ✅ |
| Postgres + Redis local (compose) | ✅ |
| First clone: `.env` + `AAAX_LOCAL_SEED` client/user + `aaax-pkce` | ✅ |
| RFC 8414 + OIDC discovery / JWKS / `/oauth2/token` | ✅ 8414 + RFC `access_token` JSON |
| PKCE (`aaax-pkce` seed client) | ✅ required on authorize |
| Hosted `/login` for `/oauth2/authorize` | ✅ form login + loopback `/authorized`; not RFC 8252 |
| Custom grants wired (see §5) | ✅ |
| Google + Apple idToken (verify / link, not a token grant) | ✅ |
| Register / OTP / forgot-password | ✅ |
| OSS mesh strip: GrandPay / Onboarding / Profile / Tenant / IDV HTTP | ✅ |
| Discord blank = no-op · Util gated · Kafka **off** (`AAAX_KAFKA_ENABLED`) | ✅ |
| Demo JKS **not** in the jar | ✅ ephemeral RSA if env unset (**local only**); file via `AAAX_JWK_KEYSTORE` |
| Password policy (min 8 unless system config) + login lockout (5) | ✅ |
| Device binding on login | ❌ OFF — register path only |
| Hosted `/admin` · `/sign-in` product UI · Event Bus catalog · `/v1/accounts` | ❌ stale greenfield — **not in this tree** |
| Passkeys · SAML · orgs | ❌ |
| Boot **4.1.1** | ✅ parent BOM; Java **21** |
| Jackson **3** | ✅ `tools.jackson` (`JSONUtil`, Redis, Retrofit factory). Annotations stay `com.fasterxml.jackson.annotation` |
| `mvn test` | ✅ unit + Testcontainers IT (Docker CLI IT excluded from default surefire) |
| `java -jar` / CI runtime smoke | ✅ Security 7 filter-chain matchers; no JwtUDS↔PasswordEncoder cycle |

---

## 3. Layout

```text
src/main/java/com/aaax/
├── core/      ← foundation (BizException, R/Result, AuditEntity, …)
└── server/    ← authentication server (endpoint, usecase, entity/po/<domain>, OIDC)
    └── App.java
```

Layering: Endpoint → UseCase → Repository → Entity. Do not invent a parallel tree.

---

## 4. HTTP

Public (resource chain): register `/users/registrations` · `/users` · `/ext/users` · OTP `/authentications/one-time-passwords/**` · forgot `/users/credentials/**` · `/keys/public-keys` · `/ws/**` · actuator/swagger.

Auth’d JWT: `/users/me` · profiles · devices · preferences · metadata · permissions · RBAC templates · clients · system-configurations · mgt · verification **query**.

OAuth/OIDC: `/oauth2/*` · hosted `/login` (authorize) · loopback `/authorized` · `/.well-known/oauth-authorization-server` (RFC 8414) · `/.well-known/openid-configuration` · JWKS. Issuer default `http://localhost:8081`.

There is **no** `/v1/accounts` API on this tree. There is **no** `/keys/private-keys` or `/keys/decryption`.

Curl recipes (register / OTP / login / me): `examples/curl/`. **No** events catalog endpoint.

---

## 5. Grants

**Wired** on `/oauth2/token`:

| `grant_type` | Notes |
|--------------|--------|
| `custom-password-grant` | Primary password |
| `custom-password-grant:e` | Encrypted password |
| `refresh_token` | RFC refresh |
| authorization_code / client_credentials | SAS defaults |

**Not on `/oauth2/token`:** QR/SMS / `custom_code` classes still on disk; they **reject** (`unsupported_grant_type`) and must not mint tokens. Device QR still has `POST /devices/qr-code-login` + WS. Google/Apple idToken **verify/link** stays in `SocialAuthenticationUseCase`; it is not a token grant.

**LoginType enum:** `USERNAME · MOBILE · EMAIL · GOOGLE · FACEBOOK · APPLE · LINE · OTP` · `GRANDPAY` reserved. Social **verify** path = Google + Apple only. Social signup does **not** create a password login.

---

## 6. Run locally

See README **Five minutes**. First empty DB: copy `.env.example` (`JPA_DDL_AUTO=update` + `AAAX_LOCAL_SEED=true`).

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home
docker compose up -d
cp .env.example .env && set -a && source .env && set +a
mvn -Dmaven.test.skip=true package
java -jar target/aaax-0.9.0-SNAPSHOT.jar
./scripts/quickstart-smoke.sh
./scripts/token-smoke.sh
```

Local seed (not production): client `client`/`secret` · user `smoke.primary@aaax.local` / `SmokePrimary!1`. Token grant: `custom-password-grant` + form field `credentials` (not `password`). Token JSON: `access_token` (RFC 6749).

Liquibase creates `oauth2_registered_client`. Domain tables come from Hibernate when `JPA_DDL_AUTO=update`. Jar default is `ddl-auto=validate` (bring your own schema; `AAAX_LOCAL_SEED` defaults **false**).

---

## 7. Configuration

One `application.yml`. Three roots: `spring` (Boot native) · `aaax` (this app) · `ext` (outbound). Secrets **env only**.

| Knob | Default |
|------|---------|
| `SERVER_PORT` | `8081` |
| `AS_ISSUER` | `http://localhost:8081` |
| `AAAX_UTIL_ENABLED` | `false` |
| Kafka | **off** (`AAAX_KAFKA_ENABLED=false`) — not required for first clone |
| `AAAX_JWK_KEYSTORE` | empty → **ephemeral RSA** (local clone only; tokens die on restart) |
| `AAAX_ENCRYPTION_KEYSTORE` | empty → ephemeral RSA (local clone only) |
| `AAAX_LOCAL_SEED` | `false` (jar default) · `true` in `.env.example` |
| `AAAX_MAX_LOGIN_ATTEMPTS` | `5` |
| `CONFIG_DEVICE_BINDING_MODE` | `OFF` (login does not check devices) |

File keystores: set path **and** password **and** alias. Nothing ships in the jar. Production **must** set `AAAX_JWK_KEYSTORE`.

---

## 8. Security posture

- No demo JKS in the classpath. Unset env = ephemeral keys for **local clone only**.
- Production: `AAAX_JWK_KEYSTORE` (+ password/alias) pointing at a file you control.
- Discord / ELK webhooks no-op when id/token blank.
- CSRF is **disabled** on the API resource chain. Hosted `/login` uses CSRF. Loopback `/authorized` is local PKCE only.
- CORS: `AAAX_CORS_ORIGINS` (default `http://localhost:*` and `http://127.0.0.1:*`). Wildcard `*` turns credentials off.
- Passwords: default pattern `.{8,}` (`aaax.security.password-patterns`). Failed logins lock after `aaax.security.max-login-attempts` (5).
- Private encryption key is **not** exposed over HTTP.
- Report vulns via GitHub Security Advisories (`SECURITY.md`).

---

## 9. OSS strip

**Removed HTTP mesh (do not re-add without an explicit ask):** GrandPay · Onboarding · Profile · Tenant · IDV.

**Kept local:** `User` / `Authentication` · `UserRoute` (opaque `tenantRoleRouteId`, no remote tenant call) · `UserVerification` list/get/patch (external IDV start throws) · Util client gated · loopback Retrofit client (placeholder URL, unused on register).

---

## 10. Out of scope until asked

- RFC 8252 native-app BCP (system browser + claimed HTTPS/loopback product path)
- Wiring QR/SMS / custom_code grants into `tokenEndpoint`
- Product web (`aaax-www`) claims beyond this booklet
- Re-adding Tenant/IDV/GrandPay mesh
- Inventing a greenfield exception stack or `/v1` overlay
