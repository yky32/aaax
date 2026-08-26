# AAAX

**Accounts · Authentication · Authorization · eXperiences**

Self-host OpenID Connect for Spring teams.  
**qs/uaa + app-core** in one Maven project (packages `com.aaax.core` · `com.aaax.server`).

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
└── server/    ← IdP (entity, endpoint, usecase, OIDC, …)
```

Boot **3.1 OSS support has ended**. This pin matches upstream qs/uaa. Do not treat 3.1 as a current production baseline; upgrade is a later lane.

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

First empty database: keep **`JPA_DDL_AUTO=update`** and **`LIQUIBASE_ENABLED=false`**  
(Liquibase files here are incremental; full schema comes from JPA on first boot.)

### 3. Build & run

```bash
mvn -Dmaven.test.skip=true package
java -jar target/aaax-0.9.0-SNAPSHOT.jar
```

App listens on **http://localhost:8081**  
Issuer default: **`http://localhost:8081`** (`AS_ISSUER`).

`-DskipTests` still **compiles** tests (needs testcontainers). Use **`-Dmaven.test.skip=true`**.

### 4. Smoke

```bash
chmod +x scripts/quickstart-smoke.sh
./scripts/quickstart-smoke.sh
```

Expect OIDC discovery / JWKS JSON when the AS is healthy.

---

## Layout

| Package | Role |
|---------|------|
| `com.aaax.core` | Response envelope, `BizException`, audit base, shared utils |
| `com.aaax.server` | UAA: users, authn, OTP, OIDC AS, devices, RBAC templates |

Quinsic-only mesh clients are **not** in this tree (GrandPay, Onboarding, Profile, Tenant, IDV).  
Optional leftover clients: **Util** (off unless `AAAX_UTIL_ENABLED=true`) and **Uaa** self-loopback (placeholder URL). Discord webhooks no-op when blank.

Secrets: **env only** — see `.env.example`. Never commit real tokens.

Classpath demo JKS is **not** shipped. Unset `AAAX_JWK_KEYSTORE` → ephemeral RSA (tokens invalid after restart). For anything else set `AAAX_JWK_KEYSTORE` + password + alias.

---

## Docs

- Product site: https://aaax-www.vercel.app/  
- Eng SoT: `docs/booklet.md` (code wins if it drifts)  
- Security: `SECURITY.md`

---

## Build note

Avoid JDK **26** with older Lombok; use **21**.
