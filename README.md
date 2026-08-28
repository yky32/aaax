# AAAX

**Accounts · Authentication · Authorization · eXperiences**

Self-host OpenID Connect for Spring teams.  
One Maven project: packages `com.aaax.core` · `com.aaax.server`.

| | |
|--|--|
| **Site** | https://aaax-www.vercel.app/ |
| **Main** | `com.aaax.server.App` |
| **Stack** | Spring Boot **3.1.0** · Java **17+** (JDK **21** recommended) |
| **Needs** | Postgres · Redis |
| **License** | Apache-2.0 |

```text
src/main/java/com/aaax/
├── core/      ← foundation (BizException, R/Result, AuditEntity, …)
└── server/    ← authentication server (entity, endpoint, usecase, OIDC, …)
```

Boot **3.1 OSS support has ended**. Do not treat 3.1 as a current production baseline; upgrade is a later lane.

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

`.env.example` sets **`SPRING_PROFILES_ACTIVE=local`**: Hibernate `ddl-auto=update` for domain tables, Liquibase creates `oauth2_registered_client`, and **`AAAX_LOCAL_SEED=true`** inserts OAuth client `client`/`secret` plus user `smoke.primary@aaax.local` / `SmokePrimary!1`.

Do **not** use `local` seed in production (`AAAX_LOCAL_SEED=false`, no `local` profile).

### 3. Build & run

```bash
mvn -Dmaven.test.skip=true package
java -jar target/aaax-0.9.0-SNAPSHOT.jar
```

(or `java -jar target/aaax-0.9.0-SNAPSHOT.jar --spring.profiles.active=local` if you skipped `.env`)

App listens on **http://localhost:8081**  
Issuer default: **`http://localhost:8081`** (`AS_ISSUER`).

`-DskipTests` still **compiles** tests (needs testcontainers). Use **`-Dmaven.test.skip=true`**.

### 4. Smoke

```bash
chmod +x scripts/quickstart-smoke.sh
./scripts/quickstart-smoke.sh
```

Expect OIDC discovery / JWKS JSON when the AS is healthy.

### 5. Token

With profile `local` + seed, run:

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

Live body is the `R` envelope: `data.accessToken` (not RFC `access_token`).

More HTTP recipes (register / OTP / `/users/me`): [`examples/curl/`](examples/curl/).

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

- Product site: https://aaax-www.vercel.app/  
- Eng SoT: `docs/booklet.md` (code wins if it drifts)  
- Security: `SECURITY.md`

---

## Build note

Avoid JDK **26** with older Lombok; use **21**.
