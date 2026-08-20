# AAAX Booklet

**Accounts · Authentication · Authorization · eXperiences**

| | |
|--|--|
| **Status** | Source of truth for product + eng docs |
| **Repo** | https://github.com/yky32/aaax |
| **Version** | `0.2.0-SNAPSHOT` |
| **Local** | `~/Documents/git/personal/aaax` |
| **License** | Apache-2.0 |
| **Updated** | 2026-08-20 |

> **This file is the single booklet.**  
> Root `README.md` is the shop window.  
> `VISION.md` / `ROADMAP.md` / `SECURITY.md` / `docs/HAPPY_PATH.md` are short pointers here — edit **this** file first, then sync pointers if needed.

---

## Table of contents

1. [One-liner & bet](#1-one-liner--bet)
2. [The four letters](#2-the-four-letters)
3. [Competitive frame](#3-competitive-frame)
4. [Principles](#4-principles)
5. [Scope (v1)](#5-scope-v1)
6. [Current status (0.2)](#6-current-status-02)
7. [Architecture](#7-architecture)
8. [Repo map](#8-repo-map)
9. [Stack & run](#9-stack--run)
10. [Configuration](#10-configuration)
11. [Demo credentials](#11-demo-credentials)
12. [HTTP API](#12-http-api)
13. [OAuth2 / OIDC](#13-oauth2--oidc)
14. [Happy path (curl)](#14-happy-path-curl)
15. [Security posture](#15-security-posture)
16. [Roadmap](#16-roadmap)
17. [Dev workflow](#17-dev-workflow)
18. [Glossary](#18-glossary)

---

## 1. One-liner & bet

> **AAAX** — open AAA with experiences: own your identity stack without giving up UX/DX.

Most auth stacks stop at Accounts / Authentication / Authorization.  
**X is the product bet:** self-host *and* feel good to integrate — not a dump of enterprise XML.

---

## 2. The four letters

| | | |
|--|--|--|
| **A** | **Accounts** | People, orgs, profiles, identity records |
| **A** | **Authentication** | Prove who you are (password, OTP, OAuth, sessions, tokens) |
| **A** | **Authorization** | What you may do (roles, permissions, clients, scopes) |
| **X** | **eXperiences** | UX for humans + DX for builders |

---

## 3. Competitive frame

| | Strength | AAAX angle |
|--|----------|------------|
| **Clerk** | Hosted UX, polish | Self-host + control + no seat tax |
| **better-auth** | TS/Next DX | JVM/Spring-first + OIDC-grade server |
| **Logto** | OIDC + self-host | Clearer DX + ops defaults from production scars |

We do **not** win by cloning Clerk’s dashboard day one.  
We win by **trust + run-your-own + fewer footguns**.

---

## 4. Principles

1. **Self-host first** — your keys, your DB, your region  
2. **OIDC-grade core** — clients, tokens, JWKS, refresh  
3. **X is mandatory** — docs, quickstarts, sane defaults  
4. **Greenfield honesty** — public tree is not a private monorepo dump  
5. **Secrets never in git**  
6. Product GitHub org later when name + scope are stable  

---

## 5. Scope (v1)

### In

- Accounts (register / basic profile hooks)
- Authentication (password + OTP path, OAuth2/OIDC server)
- Authorization (RBAC baseline + protected API sample)
- DX: Compose, curl cookbook, English docs
- UX: intentional login/OTP (not a full design system)

### Out (later)

- Full hosted admin dashboard  
- Every social provider day one  
- Passkeys / enterprise SSO packs  
- Any Quinsic / tgt business APIs  

---

## 6. Current status (0.2)

| Area | State |
|------|--------|
| Public greenfield repo | ✅ |
| Spring Authorization Server | ✅ |
| Accounts + register + DB login + roles | ✅ |
| JDBC OAuth clients + authorization/consent | ✅ |
| File-backed RSA JWK (stable across restarts) | ✅ |
| OTP request/verify + pluggable `OtpSender` | ✅ (default = log) |
| Protected API sample | ✅ `GET /v1/api/hello` |
| CI + Docker Compose | ✅ |
| Email/SMS OTP, Redis multi-node, passkeys | ⬜ v0.3+ |

---

## 7. Architecture

```text
                    ┌─────────────────────────────┐
   Browser / SPA ──►│  Form login  /oauth2/*      │
                    │  Authorization Server       │
   API clients   ──►│  JWT resource (/v1/api/**)  │
                    └─────────────┬───────────────┘
                                  │
              ┌───────────────────┼───────────────────┐
              ▼                   ▼                   ▼
        accounts (JPA)   oauth2_* (JDBC)      aaax-jwk.json (file)
              │                   │
              ▼                   ▼
         H2 (dev) / PostgreSQL (Compose / prod path)
```

### Security filter chains (order)

| Order | Matcher | Role |
|------:|---------|------|
| 1 | AS defaults | `/oauth2/**`, OIDC, token, jwks |
| 2 | `/v1/api/**` | JWT resource server · `SCOPE_api.read` / `ROLE_ADMIN` |
| 3 | everything else | Form login · session · public register/otp/health |

### Packages (`com.aaax`)

| Package | Responsibility |
|---------|----------------|
| `account` | Entity, register, `UserDetailsService`, demo seed |
| `client` | Demo OAuth client seed (JDBC) |
| `config` | Security chains, JWK file loader |
| `otp` | OTP store/service + `OtpSender` SPI |
| `web` | REST controllers + exception handler |

---

## 8. Repo map

```text
aaax/
├── docs/
│   ├── AAAX_BOOKLET.md      ← YOU ARE HERE (source of truth)
│   └── HAPPY_PATH.md        ← pointer → §14
├── src/main/java/com/aaax/
├── src/main/resources/
│   ├── application.yml
│   └── schema.sql           ← OAuth2 JDBC tables
├── src/test/
├── docker-compose.yml       ← Postgres + Redis + app
├── Dockerfile
├── pom.xml
├── .env.example
├── README.md                ← short entry
├── VISION.md                ← pointer → §1–6
├── ROADMAP.md               ← pointer → §16
├── SECURITY.md              ← policy + pointer → §15
└── LICENSE                  ← Apache-2.0
```

---

## 9. Stack & run

### Stack

- Java 17 · Spring Boot **3.3.5** · Spring Authorization Server  
- Spring Security · JPA · Validation · Actuator  
- H2 (default local/tests) · PostgreSQL (Compose)  
- JDBC OAuth2 registered client + authorization services  
- File RSA JWK  

### Local (fastest)

```bash
cd ~/Documents/git/personal/aaax
mvn test
mvn spring-boot:run
```

- Meta: http://localhost:8081/  
- Health: http://localhost:8081/actuator/health  
- OIDC: http://localhost:8081/.well-known/openid-configuration  

### Docker

```bash
cp .env.example .env
mvn -DskipTests package
docker compose up --build
```

App publishes `8081`. JWK volume: `aaax_jwk` → `/data/aaax-jwk.json`.

---

## 10. Configuration

### Important keys (`application.yml` / env)

| Key / env | Default | Meaning |
|-----------|---------|---------|
| `SERVER_PORT` | `8081` | HTTP port |
| `AAAX_ISSUER` | `http://localhost:8081` | OIDC issuer URL |
| `AAAX_JWK_PATH` | `./data/aaax-jwk.json` | RSA JWK file (private+public) |
| `DB_URL` | H2 mem | JDBC URL |
| `DB_USERNAME` / `DB_PASSWORD` | `sa` / empty | DB auth |
| `DB_DRIVER` | `org.h2.Driver` | Use `org.postgresql.Driver` with Postgres |
| `AAAX_OTP_TTL_SECONDS` | `300` | OTP lifetime |
| `AAAX_OTP_LENGTH` | `6` | OTP digits |
| `AAAX_DEMO_SEED_CLIENT` | `true` | Seed `aaax-demo` |
| `AAAX_DEMO_SEED_ACCOUNT` | `true` | Seed `demo` + `admin` |

Full template: `.env.example`.

### Data that must not be committed

- `.env`  
- `./data/` (JWK)  
- Any real cloud credentials  

---

## 11. Demo credentials

| Kind | Value | Notes |
|------|--------|--------|
| User | `demo` / `demo` | `ROLE_USER` · seeded if missing |
| Admin | `admin` / `admin12345` | `ROLE_USER,ADMIN` |
| OAuth client | `aaax-demo` / `aaax-demo-secret` | code + refresh + client_credentials |
| Scopes | `openid` `profile` `api.read` | |
| Redirect | `http://127.0.0.1:3000/login/oauth2/code/aaax` | (+ localhost variant) |

**Local only.** Change before any shared environment.

---

## 12. HTTP API

Base: `http://localhost:8081`

| Method | Path | Auth | Purpose |
|--------|------|------|---------|
| `GET` | `/` | public | Product meta JSON |
| `GET` | `/actuator/health` | public | Liveness |
| `POST` | `/v1/accounts/register` | public | Create account |
| `GET` | `/v1/accounts/me` | session | Current account |
| `POST` | `/v1/otp/request` | public | Issue OTP (sender) |
| `POST` | `/v1/otp/verify` | public | Check OTP |
| `GET` | `/v1/api/hello` | Bearer JWT · `SCOPE_api.read` | Protected sample |
| `GET` | `/v1/api/admin/ping` | Bearer JWT · `ROLE_ADMIN` | Admin sample |

### Register body

```json
{
  "username": "alice",
  "email": "alice@example.com",
  "password": "password123"
}
```

Rules: username `3–64` `[a-zA-Z0-9._-]`; password `8–128`; email optional unique.

### Register response (201)

```json
{
  "id": "…",
  "username": "alice",
  "email": "alice@example.com",
  "roles": ["USER"],
  "enabled": true,
  "createdAt": "…"
}
```

Never returns password hashes.

### Errors

JSON from `ApiExceptionHandler`:

```json
{
  "timestamp": "…",
  "status": 409,
  "error": "Conflict",
  "message": "username already taken"
}
```

Validation failures: `400` + `fields` map.

---

## 13. OAuth2 / OIDC

| Endpoint | Notes |
|----------|--------|
| `/.well-known/openid-configuration` | Discovery |
| `/oauth2/jwks` | Public JWKS |
| `/oauth2/token` | code / refresh / client_credentials |
| `/oauth2/authorize` | Authorization code (browser + login) |
| `/login` | Form login (DB accounts) |

### Persistence

| Store | Impl |
|-------|------|
| Registered clients | `JdbcRegisteredClientRepository` |
| Authorizations | `JdbcOAuth2AuthorizationService` |
| Consents | `JdbcOAuth2AuthorizationConsentService` |
| Schema | `src/main/resources/schema.sql` |

### Signing keys

- Generated once into `aaax.jwk.path` if missing  
- Same file reused → tokens remain verifiable across restarts  
- Protect file permissions in real deploys; **new key per environment**

---

## 14. Happy path (curl)

### 0. Start

```bash
mvn spring-boot:run
# or
mvn -DskipTests package && docker compose up --build
```

### 1. Register

```bash
curl -sS -X POST http://localhost:8081/v1/accounts/register \
  -H 'content-type: application/json' \
  -d '{"username":"alice","email":"alice@example.com","password":"password123"}' | jq
```

### 2. Client credentials → protected API

```bash
TOKEN=$(curl -sS -u 'aaax-demo:aaax-demo-secret' \
  -X POST http://localhost:8081/oauth2/token \
  -d 'grant_type=client_credentials&scope=api.read' | jq -r .access_token)

curl -sS http://localhost:8081/v1/api/hello \
  -H "Authorization: Bearer $TOKEN" | jq
```

### 3. OTP (dev: code in server logs)

```bash
curl -sS -X POST http://localhost:8081/v1/otp/request \
  -H 'content-type: application/json' \
  -d '{"username":"demo"}' | jq

# log line: AAAX OTP for demo@aaax.local => ######
curl -sS -X POST http://localhost:8081/v1/otp/verify \
  -H 'content-type: application/json' \
  -d '{"username":"demo","code":"PASTE"}' | jq
```

### 4. Authorization code (browser)

1. Login: http://localhost:8081/login → `demo` / `demo`  
2. Open:

```text
http://localhost:8081/oauth2/authorize?response_type=code&client_id=aaax-demo&redirect_uri=http://127.0.0.1:3000/login/oauth2/code/aaax&scope=openid%20profile%20api.read&state=xyz
```

3. Exchange:

```bash
curl -sS -u 'aaax-demo:aaax-demo-secret' \
  -X POST http://localhost:8081/oauth2/token \
  -d 'grant_type=authorization_code' \
  -d 'code=PASTE_CODE' \
  -d 'redirect_uri=http://127.0.0.1:3000/login/oauth2/code/aaax' | jq
```

### 5. Discovery / JWKS

```bash
curl -sS http://localhost:8081/.well-known/openid-configuration | jq
curl -sS http://localhost:8081/oauth2/jwks | jq
```

---

## 15. Security posture

### Report

GitHub Security Advisories on this repo, or maintainer contact on GitHub profile.  
Do **not** open public issues for active exploits.

### Supported

| Version | Supported |
|---------|-----------|
| `main` (`0.2.x-SNAPSHOT`) | Yes — best effort |

### Hard truths (pre-1.0)

- Demo users/clients are **toys**  
- Default `OtpSender` **logs codes** — swap before shared envs  
- JWK file holds **private** key material  
- No multi-node OTP store yet (in-memory)  
- Secrets only via env — never commit `.env`  

### If leaked

Rotate immediately (DB, OAuth secrets, JWK, any provider keys).

---

## 16. Roadmap

### Done (0.2)

- [x] Public repo + Apache-2.0  
- [x] Greenfield `com.aaax`  
- [x] Authorization Server  
- [x] Accounts + roles + register + DB login  
- [x] JDBC clients / authorizations / consents  
- [x] Stable JWK file  
- [x] OTP SPI + log sender  
- [x] Protected API sample  
- [x] Curl happy path + this booklet  
- [x] CI + Compose  

### Next (0.3+)

- [ ] Email / SMS `OtpSender` implementations  
- [ ] Redis OTP / authorization option (multi-node)  
- [ ] Passwordless OTP login grant  
- [ ] Admin client management API  
- [ ] Passkeys / social packs  
- [ ] v1.0 stranger cold-path polish  

### Non-goals (v1)

- Clerk dashboard clone  
- Shipping Quinsic/tgt business APIs inside this repo  

---

## 17. Dev workflow

| Rule | |
|------|--|
| Solo / pre-users | **Push `main` directly** — no PR required |
| Later (users/collab) | Resume branch + PR |
| Commits | Conventional-ish: `feature(aaax): …` / `docs(aaax): …` |
| Tests | `mvn test` before push when touching Java |
| Docs | **Edit this booklet first** |

### Lane

WY Limited: **🧪 CTO · aaax** (Telegram topic).  
Not indie main-ship (primary remains elsewhere until CEO gate).

---

## 18. Glossary

| Term | Meaning |
|------|---------|
| **AAAX** | Product name (always four letters) |
| **AS** | OAuth2 Authorization Server |
| **JWK / JWKS** | JSON Web Key / Key Set (signing) |
| **OIDC** | OpenID Connect (identity layer on OAuth2) |
| **Registered client** | OAuth application (e.g. `aaax-demo`) |
| **Scope `api.read`** | Required authority on `/v1/api/**` as `SCOPE_api.read` |
| **OtpSender** | Pluggable interface for delivering OTP codes |

---

*AAAX booklet — one place for product, architecture, API, security, and ops.*  
*When docs drift, fix this file.*
