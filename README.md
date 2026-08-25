# AAAX

**Accounts · Authentication · Authorization · eXperiences**

Self-host OpenID Connect for Spring teams.  
**qs/uaa + app-core** in one Maven project (packages `com.aaax.core` · `com.aaax.server`).

| | |
|--|--|
| **Site** | https://aaax-www.vercel.app/ |
| **Main** | `com.aaax.server.App` |
| **Stack** | Spring Boot **3.1** · Java **17+** (JDK **21** recommended) |
| **Needs** | Postgres · Redis |
| **License** | Apache-2.0 |

```text
src/main/java/com/aaax/
├── core/      ← foundation (BizException, R/Result, AuditEntity, …)
└── server/    ← IdP (entity, endpoint, usecase, OIDC, …)
```

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
mvn -DskipTests package
java -jar target/aaax-0.9.0-SNAPSHOT.jar
```

App listens on **http://localhost:8081**  
Issuer default: **`http://localhost:8081`** (`AS_ISSUER`).

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

Optional mesh HTTP clients (tenant / idv / util) default to a **disabled placeholder** URL.  
Quinsic-only clients (GrandPay, Onboarding, Profile) are **not** in this tree.

Secrets: **env only** — see `.env.example`. Never commit real tokens.

---

## Docs

- Product site: https://aaax-www.vercel.app/  
- Deeper notes: `docs/` (some historical — code wins)  
- Security: `SECURITY.md`

---

## Build note

Avoid JDK **26** with older Lombok; use **21**.

```bash
mvn -DskipTests package
```
