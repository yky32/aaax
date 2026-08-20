# AAAX Booklet

**Accounts · Authentication · Authorization · eXperiences**

| | |
|--|--|
| **Status** | Source of truth for product + eng docs |
| **Repo** | https://github.com/yky32/aaax |
| **Version** | `0.3.0-SNAPSHOT` |
| **Local** | `~/Documents/git/personal/aaax` |
| **License** | Apache-2.0 |
| **Updated** | 2026-08-20 |

> **This file is the single booklet.**  
> Root `README.md` is the shop window.  
> `VISION.md` / `ROADMAP.md` / `SECURITY.md` / `docs/HAPPY_PATH.md` point here.

---

## Table of contents

1. [One-liner & bet](#1-one-liner--bet)
2. [The four letters](#2-the-four-letters)
3. [Competitive frame](#3-competitive-frame)
4. [Principles](#4-principles)
5. [Scope (v1)](#5-scope-v1)
6. [Current status (0.3)](#6-current-status-03)
7. [Architecture](#7-architecture)
8. [Repo map](#8-repo-map)
9. [Stack & run](#9-stack--run)
10. [Configuration](#10-configuration)
11. [Demo credentials](#11-demo-credentials)
12. [HTTP API](#12-http-api)
13. [OAuth2 / OIDC](#13-oauth2--oidc)
14. [Happy path (curl)](#14-happy-path-curl)
15. [Security posture](#15-security-posture)
16. [Deploy checklist](#16-deploy-checklist)
17. [Roadmap](#17-roadmap)
18. [Dev workflow](#18-dev-workflow)
19. [Glossary](#19-glossary)

---

## 1. One-liner & bet

> **AAAX** — open AAA with experiences: own your identity stack without giving up UX/DX.

**X is the product bet:** self-host *and* feel good to integrate.

---

## 2. The four letters

| | | |
|--|--|--|
| **A** | **Accounts** | People, orgs, profiles, identity records |
| **A** | **Authentication** | Password, OTP, OAuth, sessions, tokens |
| **A** | **Authorization** | Roles, permissions, clients, scopes |
| **X** | **eXperiences** | UX + DX |

---

## 3. Competitive frame

| | Strength | AAAX angle |
|--|----------|------------|
| **Clerk** | Hosted UX | Self-host, no seat tax |
| **better-auth** | TS/Next DX | JVM/Spring + OIDC server |
| **Logto** | OIDC self-host | Clearer DX + ops defaults |

---

## 4. Principles

1. Self-host first  
2. OIDC-grade core  
3. X is mandatory (docs/quickstarts)  
4. Greenfield honesty  
5. Secrets never in git  
6. Product GH org later  

---

## 5. Scope (v1)

**In:** Accounts, password+OTP, OAuth2/OIDC, RBAC baseline, Compose, curl docs.  
**Out:** Full admin dashboard, every social, passkeys day-one, Quinsic business APIs.

---

## 6. Current status (0.3)

| Area | State |
|------|--------|
| Accounts + register + DB login + roles | ✅ |
| JDBC OAuth clients + authorizations | ✅ |
| File-backed RSA JWK | ✅ |
| OTP request/verify | ✅ |
| OTP channel `console` \| `mail` (SMTP) | ✅ |
| Passwordless `POST /v1/auth/otp/login` | ✅ |
| Admin clients CRUD `/v1/admin/clients` | ✅ |
| Protected API `GET /v1/api/hello` | ✅ |
| `prod` profile (no demo seeds) | ✅ |
| Deploy checklist (this §16) | ✅ |
| Redis multi-node OTP | ⬜ |
| Passkeys / social | ⬜ |

---

## 7. Architecture

```text
 Browser / SPA ──► Form login + /oauth2/* (AS)
 API clients  ──► JWT /v1/api/**
 Admin (session ROLE_ADMIN) ──► /v1/admin/clients
 OTP ──► OtpSender (console | mail)
        │
 accounts (JPA) · oauth2_* (JDBC) · aaax-jwk.json (file)
        │
 H2 (dev) / PostgreSQL (Compose)
```

### Filter chains

| Order | Matcher | Role |
|------:|---------|------|
| 1 | AS | token, authorize, jwks, OIDC |
| 2 | `/v1/api/**` | JWT · `SCOPE_api.read` / admin JWT role |
| 3 | default | session · register/otp/auth public · `/v1/admin/**` needs `ROLE_ADMIN` |

### Packages

| Package | |
|---------|--|
| `account` | Entity, register, UserDetails, seeds |
| `client` | Demo client seed + admin client service |
| `config` | Security, JWK file |
| `otp` | Store, service, Logging/Mail senders |
| `web` | REST + errors |

---

## 8. Repo map

```text
aaax/
├── docs/AAAX_BOOKLET.md     ← source of truth
├── src/main/java/com/aaax/
├── src/main/resources/
│   ├── application.yml
│   ├── application-prod.yml
│   └── schema.sql
├── docker-compose.yml
├── .env.example
└── README.md
```

---

## 9. Stack & run

- Java 17 · Spring Boot 3.3 · Authorization Server · Mail  
- JPA Accounts · JDBC OAuth · File JWK · H2 / Postgres  

```bash
mvn test
mvn spring-boot:run
# prod-ish local:
# mvn spring-boot:run -Dspring-boot.run.profiles=prod
```

```bash
cp .env.example .env
mvn -DskipTests package
docker compose up --build
```

---

## 10. Configuration

| Env | Default | |
|-----|---------|--|
| `AAAX_ISSUER` | `http://localhost:8081` | Issuer URL |
| `AAAX_JWK_PATH` | `./data/aaax-jwk.json` | Signing key file |
| `AAAX_OTP_CHANNEL` | `console` | `console` or `mail` |
| `MAIL_HOST` / `PORT` / `USERNAME` / `PASSWORD` | empty | SMTP |
| `AAAX_OTP_MAIL_FROM` | `noreply@aaax.local` | From header |
| `AAAX_DEMO_SEED_*` | `true` | **false in prod** |
| `DB_*` | H2 mem | Postgres in Compose |

`application-prod.yml`: seeds off, `ddl-auto=validate`, sql init never.

---

## 11. Demo credentials (local seeds only)

| | |
|--|--|
| User | `demo` / `demo` |
| Admin | `admin` / `admin12345` (`ROLE_ADMIN`) |
| Client | `aaax-demo` / `aaax-demo-secret` |

---

## 12. HTTP API

| Method | Path | Auth |
|--------|------|------|
| GET | `/` | public |
| POST | `/v1/accounts/register` | public |
| GET | `/v1/accounts/me` | session |
| POST | `/v1/otp/request` | public |
| POST | `/v1/otp/verify` | public |
| POST | `/v1/auth/otp/login` | public → **sets session** |
| GET/POST/DELETE | `/v1/admin/clients` | session **ROLE_ADMIN** |
| GET | `/v1/api/hello` | Bearer `SCOPE_api.read` |

### Admin create client body

```json
{
  "clientId": "my-app",
  "clientName": "My App",
  "redirectUris": ["http://127.0.0.1:4000/callback"],
  "scopes": ["openid", "profile", "api.read"],
  "grantTypes": ["authorization_code", "refresh_token", "client_credentials"]
}
```

Response includes one-time `clientSecret` (save it).

---

## 13. OAuth2 / OIDC

| Endpoint | |
|----------|--|
| `/.well-known/openid-configuration` | Discovery |
| `/oauth2/jwks` | JWKS |
| `/oauth2/token` | code / refresh / client_credentials |
| `/oauth2/authorize` | Auth code |

---

## 14. Happy path (curl)

### Register

```bash
curl -sS -X POST http://localhost:8081/v1/accounts/register \
  -H 'content-type: application/json' \
  -d '{"username":"alice","email":"alice@example.com","password":"password123"}' | jq
```

### Client credentials → API

```bash
TOKEN=$(curl -sS -u 'aaax-demo:aaax-demo-secret' \
  -X POST http://localhost:8081/oauth2/token \
  -d 'grant_type=client_credentials&scope=api.read' | jq -r .access_token)
curl -sS http://localhost:8081/v1/api/hello -H "Authorization: Bearer $TOKEN" | jq
```

### OTP passwordless login (cookie jar)

```bash
curl -sS -c /tmp/aaax.cj -X POST http://localhost:8081/v1/otp/request \
  -H 'content-type: application/json' -d '{"username":"demo"}' | jq
# read code from logs (console) or email (mail)
curl -sS -c /tmp/aaax.cj -b /tmp/aaax.cj -X POST http://localhost:8081/v1/auth/otp/login \
  -H 'content-type: application/json' \
  -d '{"username":"demo","code":"PASTE"}' | jq
curl -sS -b /tmp/aaax.cj http://localhost:8081/v1/accounts/me | jq
```

### Admin clients (login as admin first)

```bash
# form login session, or OTP login as admin then:
curl -sS -b /tmp/aaax.cj http://localhost:8081/v1/admin/clients | jq
```

### Authorization code

Login http://localhost:8081/login then:

```text
/oauth2/authorize?response_type=code&client_id=aaax-demo&redirect_uri=http://127.0.0.1:3000/login/oauth2/code/aaax&scope=openid%20profile%20api.read&state=xyz
```

---

## 15. Security posture

- Report via GitHub Security Advisories  
- Demo seeds are toys — disable in prod  
- `console` OTP logs codes; use `mail` + real SMTP for shared envs  
- Protect `AAAX_JWK_PATH` file (private key)  
- OTP store is in-memory (single node)  

---

## 16. Deploy checklist

### Before first real deploy

- [ ] Fresh host / VM / K8s namespace  
- [ ] Postgres reachable; set `DB_URL` / user / password / `DB_DRIVER=org.postgresql.Driver`  
- [ ] Run schema: app starts with `sql.init` **or** apply `schema.sql` + JPA entities manually when using `prod` (`ddl-auto=validate` → migrate first)  
- [ ] Set `AAAX_ISSUER` to **public** URL (https)  
- [ ] Generate/store JWK path on **persistent volume**; mode `600`  
- [ ] `AAAX_DEMO_SEED_CLIENT=false` · `AAAX_DEMO_SEED_ACCOUNT=false`  
- [ ] Activate profile `prod` (`SPRING_PROFILES_ACTIVE=prod`)  
- [ ] `AAAX_OTP_CHANNEL=mail` + working SMTP (`MAIL_*`)  
- [ ] Create first admin account out-of-band (SQL or temporary seed once)  
- [ ] Create OAuth clients via `/v1/admin/clients` — store secrets in a vault  
- [ ] TLS terminator in front; cookies Secure/HttpOnly in real browser apps  
- [ ] Backup Postgres + JWK file  
- [ ] Smoke: health, discovery, client_credentials, register, otp login  

### Compose smoke (dev)

```bash
mvn -DskipTests package
docker compose up --build -d
curl -sf http://localhost:8081/actuator/health
curl -sf http://localhost:8081/.well-known/openid-configuration | head
docker compose down
```

---

## 17. Roadmap

### Done

- [x] 0.2 greenfield core  
- [x] 0.3 mail OTP channel + console fallback  
- [x] 0.3 passwordless OTP session login  
- [x] 0.3 admin clients API  
- [x] 0.3 prod profile + deploy checklist  

### Next

- [ ] SMS `OtpSender`  
- [ ] Redis OTP / multi-node  
- [ ] First admin bootstrap endpoint (one-time token)  
- [ ] Passkeys / social  
- [ ] Sample BFF app  
- [ ] v1.0 stranger cold path  

### Non-goals (v1)

- Clerk dashboard clone  
- Quinsic business APIs in-repo  

---

## 18. Dev workflow

| | |
|--|--|
| Solo | Push `main` directly |
| Docs | Edit **this booklet** first |
| Tests | `mvn test` before push |
| Lane | 🧪 CTO · aaax · not WIP primary |

---

## 19. Glossary

| Term | |
|------|--|
| AS | Authorization Server |
| JWK | Signing key material |
| OtpSender | Delivery SPI (`console` / `mail`) |
| Registered client | OAuth app row |
| `SCOPE_api.read` | JWT authority for `/v1/api/**` |

---

*AAAX booklet — fix drift here first.*
