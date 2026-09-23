# AAAX

**Accounts · Authentication · Authorization · eXperiences**

Self-host OpenID Connect for Spring teams.  
One Maven project: packages `com.aaax.core` · `com.aaax.server`.

| | |
|--|--|
| **Site** | https://aaax-www.vercel.app/ |
| **Main** | `com.aaax.server.App` |
| **Stack** | Spring Boot **4.1.1** · Java **21** |
| **Needs** | Postgres · Redis |
| **License** | Apache-2.0 |
| **Maven** | `com.aaax:aaax` (Central publish via tag — see [CONTRIBUTING](./CONTRIBUTING.md#maven-central-maintainers)) |

```text
src/main/java/com/aaax/
├── core/      ← foundation (BizException, R/Result, AuditEntity, …)
└── server/    ← authentication server (entity, endpoint, usecase, OIDC, …)
```

Jackson **3** (`tools.jackson`) is the app JSON stack. `@JsonInclude` and friends stay on `com.fasterxml.jackson.annotation`.

---

## Five minutes (local)

### 0. Prerequisites

- Docker  
- JDK 21  
- Maven 3.9+

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home   # macOS Homebrew example
# or: export JAVA_HOME=$(/usr/libexec/java_home -v 21)
```

### 1. Infra

```bash
git clone https://github.com/yky32/aaax.git && cd aaax
docker compose up -d
# postgres :5432  user/pass/db = aaax/aaax/aaax
# redis    :6379
```

### 2. Config

```bash
cp .env.example .env
set -a && source .env && set +a
```

`.env.example` sets:

- **`JPA_DDL_AUTO=update`** — Hibernate creates domain tables on empty Postgres  
- **`LIQUIBASE_ENABLED=true`** — creates `oauth2_registered_client`  
- **`AAAX_LOCAL_SEED=true`** — inserts confidential `client`/`secret`, public PKCE clients `aaax-pkce` + `aaax-portal`, and user `smoke.primary@aaax.local` / `SmokePrimary!1`
- **`AAAX_KAFKA_ENABLED=false`** — Kafka is optional; first clone only needs Postgres + Redis

Turn seed off with **`AAAX_LOCAL_SEED=false`**. Do not use this seed in production.

### 3. Build & run

```bash
mvn -Dmaven.test.skip=true package
java -jar target/aaax-0.9.1.jar
```

App listens on **http://localhost:8081**  
Issuer default: **`http://localhost:8081`** (`AS_ISSUER`).

`-DskipTests` still **compiles** tests (needs testcontainers). Use **`-Dmaven.test.skip=true`**.

### 4. Smoke

```bash
chmod +x scripts/quickstart-smoke.sh
./scripts/quickstart-smoke.sh
```

Expect RFC 8414 (`/.well-known/oauth-authorization-server`), OIDC discovery, and JWKS when the AS is healthy. With seed on: `./scripts/pkce-smoke.sh` then `./scripts/hosted-authorize-smoke.sh` (hosted `/login` → loopback code → public-client token). Not claimed HTTPS.

### 5. Token

With `.env` loaded (`AAAX_LOCAL_SEED=true`), run:

```bash
chmod +x scripts/token-smoke.sh
./scripts/token-smoke.sh
```

Defaults: client `client`/`secret`, user `smoke.primary@aaax.local` / `SmokePrimary!1`. Override with `AAAX_CLIENT_*` / `AAAX_USERNAME` / `AAAX_CREDENTIALS`.

Same call by hand:

```bash
curl -sS -u client:secret \
  -X POST http://localhost:8081/oauth2/token \
  -H 'content-type: application/x-www-form-urlencoded' \
  -d 'grant_type=custom-password-grant' \
  -d 'username=smoke.primary@aaax.local' \
  -d 'credentials=SmokePrimary!1'
```

Live body is RFC 6749 JSON: `access_token` (not an AAAX `R` envelope). `/users/me` still uses the Result envelope.

More HTTP recipes (register / OTP / `/users/me`): [`examples/curl/`](examples/curl/).

Operator portal (separate repo, not in this jar): [yky32/aaax-portal](https://github.com/yky32/aaax-portal). Point it at `http://localhost:8081` with `AAAX_LOCAL_SEED=true`. Seed public client **`aaax-portal`** (PKCE, redirect `:5173/callback`). The jar has no `/admin` UI. Gate `/swagger-ui` in production.

---

## Use as an OAuth 2.0 authorization server

AAAX is a **Spring Authorization Server** plus qs/uaa user APIs. Point a resource server at this issuer. It is **not** Keycloak, Authentik, or Clerk.

| | |
|--|--|
| Issuer | `http://localhost:8081` (`AS_ISSUER`) |
| RFC 8414 | `GET /.well-known/oauth-authorization-server` |
| OIDC discovery | `GET /.well-known/openid-configuration` |
| JWKS | `GET /oauth2/jwks` |
| Token | `POST /oauth2/token` — RFC 6749 JSON (`access_token`) |
| Authorize | `GET /oauth2/authorize` → hosted `/login` |
| User API | `/users/me` still uses the `R` / `Result` envelope (not RFC token JSON) |

**Confidential (first-party / machine):** seed client `client` / `secret`. Password grant field is **`credentials`**, not `password`.

```bash
curl -sS -u client:secret -X POST http://localhost:8081/oauth2/token \
  -H 'content-type: application/x-www-form-urlencoded' \
  -d 'grant_type=custom-password-grant' \
  -d 'username=smoke.primary@aaax.local' \
  -d 'credentials=SmokePrimary!1'
# → {"access_token":"…","token_type":"Bearer",…}
```

**Public (SPA / native loopback):** seed client `aaax-pkce` (`none`, `requireProofKey=true`). SAS checks `code_challenge` **before** login. Missing PKCE → `invalid_request`, not `/login`. Token POST is public (no Basic, no CSRF cookie). Script: `./scripts/hosted-authorize-smoke.sh`.

**Resource server:** validate JWT against this JWKS. Do not treat `/users/**` as OIDC UserInfo.

**Local only:** unset `AAAX_JWK_KEYSTORE` → ephemeral RSA (tokens die on restart). Seed credentials are not for production. Loopback any-port on `127.0.0.1` / `[::1]` is RFC 8252 §7.3 — **not** claimed HTTPS.

### Production

- Set **`AAAX_JWK_KEYSTORE`** (+ password + alias) and **`AAAX_ENCRYPTION_KEYSTORE`**. Turn **`AAAX_LOCAL_SEED=false`**.
- **`/swagger-ui/**`**, **`/v3/api-docs/**`**, and **`/actuator/**`** are **unauthenticated** on the API chain (local-dev convenience). Gate them at your reverse proxy, disable springdoc, and restrict Actuator exposure — see **`docs/booklet.md` §8.1**. There is no built-in admin UI.

---

## Layout

| Package | Role |
|---------|------|
| `com.aaax.core` | Response envelope, `BizException`, audit base, shared utils |
| `com.aaax.server` | Authentication server: users, OTP, OIDC, devices, RBAC templates |

Optional leftover clients: **Util** (off unless `AAAX_UTIL_ENABLED=true`) and a loopback Retrofit client (placeholder URL). Discord webhooks no-op when blank.

Secrets: **env only** — see `.env.example`. Never commit real tokens.

Classpath demo JKS is **not** shipped. Unset `AAAX_JWK_KEYSTORE` → ephemeral RSA (**local clone only**; tokens invalid after restart). Production **must** set `AAAX_JWK_KEYSTORE` + password + alias. Same for `AAAX_ENCRYPTION_KEYSTORE`.

---

## Docs

- **[docs/README.md](docs/README.md)** — start here (human + agent index)  
- **[docs/booklet.md](docs/booklet.md)** — product + eng SoT (code wins if it drifts)  
- **[AGENTS.md](AGENTS.md)** — instructions for AI coding agents  
- Product site: https://aaax-www.vercel.app/  
- Security: `SECURITY.md`

---

## Build note

Avoid JDK **26** with older Lombok; use **21**.
