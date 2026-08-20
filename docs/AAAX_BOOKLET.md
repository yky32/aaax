# AAAX Booklet

**Accounts · Authentication · Authorization · eXperiences**

| | |
|--|--|
| **Status** | Source of truth for product + eng docs |
| **qs/uaa parity** | [PARITY_QS_UAA.md](./PARITY_QS_UAA.md) |
| **Repo** | https://github.com/yky32/aaax |
| **Version** | **`0.5.0`** (Boot **4.1** / JDK **21**) · tag `v0.5.0` |
| **Local** | `~/Documents/git/personal/aaax` |
| **License** | Apache-2.0 |
| **Updated** | 2026-08-20 |
| **Maven** | Central + Shibboleth OpenSAML (public; no private packages) |
| **Admin** | http://localhost:8081/admin/ |
| **Hosted** | `/sign-in/` · `/sign-up/` · `/user/` |
| **Events** | [IDENTITY_EVENTS.md](./IDENTITY_EVENTS.md) |
| **SMS / SAML** | [SMS_SAML.md](./SMS_SAML.md) |
| **Clerk map** | [CLERK_PARITY.md](./CLERK_PARITY.md) |

> **This file is the single booklet.**  
> Root `README.md` is the shop window.  
> `VISION.md` / `ROADMAP.md` / `SECURITY.md` / `docs/HAPPY_PATH.md` point here.

---

## Table of contents

1. [One-liner & bet](#1-one-liner--bet)
2. [Developer product](#2-developer-product)
3. [The four letters](#3-the-four-letters)
4. [Competitive frame](#4-competitive-frame)
5. [Principles](#5-principles)
6. [Scope (v1)](#6-scope-v1)
7. [Current status (0.4)](#7-current-status-04)
8. [Architecture](#8-architecture)
9. [Repo map](#9-repo-map)
10. [Stack & run](#10-stack--run)
11. [Configuration](#11-configuration)
12. [Demo credentials](#12-demo-credentials)
13. [HTTP API](#13-http-api)
14. [OAuth2 / OIDC](#14-oauth2--oidc)
15. [Happy path (curl)](#15-happy-path-curl)
16. [Security posture](#16-security-posture)
17. [Deploy checklist](#17-deploy-checklist)
18. [Roadmap](#18-roadmap)
19. [Dev workflow](#19-dev-workflow)
20. [Glossary](#20-glossary)

---

## 1. One-liner & bet

> **AAAX** — open AAA with experiences: own your identity stack without giving up UX/DX.

**X is the product bet:** self-host *and* feel good to integrate.

---

## 2. Developer product

AAAX is an **IT / developer product**, not an internal monorepo extract.

### Promise

| # | Promise | Status |
|---|---------|--------|
| 1 | `git clone && mvn test` with **Maven Central only** | ✅ |
| 2 | Compose or `spring-boot:run` → health + token + API | ✅ |
| 3 | Register → OTP login → `/me` | ✅ |
| 4 | Admin OAuth clients + sample protected API | ✅ |
| 5 | Curl examples pack | ✅ `examples/` |
| 6 | Resource-server integration sketch | ✅ `examples/resource-call.md` |

### ICP

- Spring/Java teams needing OIDC without Keycloak weight  
- Indie / small B2B products that must own identity data  
- Platform eng: a few SPAs + APIs, one issuer  

### Category line

> Open-source OIDC server for builders who outgrew JWT-in-a-weekend and refuse Keycloak weight.

### Product surfaces

| Surface | Role |
|---------|------|
| `aaax-server` | The runnable product |
| README | Shop window / 5-minute path |
| Booklet | Full truth |
| `examples/` | Integrate recipes |
| GitHub release tags | Trust signal (`v0.3.0`) |

### Explicit non-positioning

- Not Auth0 enterprise RFP bait day one  
- Not a 1:1 qs/uaa binary clone (see [PARITY_QS_UAA.md](./PARITY_QS_UAA.md))  
- Not a hosted multi-tenant SaaS (self-host first)

---

## 3. The four letters

| | | |
|--|--|--|
| **A** | **Accounts** | People, orgs, profiles, identity records |
| **A** | **Authentication** | Password, OTP, OAuth, sessions, tokens |
| **A** | **Authorization** | Roles, permissions, clients, scopes |
| **X** | **eXperiences** | UX + DX |

---

## 4. Competitive frame

**Positioning:** Spring-native lean self-host OIDC — not Keycloak kitchen-sink, not Clerk hosted UX.

> AAAX = self-host OIDC for **Spring/JVM teams** who want clients, MFA, admin UI, and **caller-owned SMS** (Kafka/webhook) — without Keycloak weight or Clerk seats.

Legend: ✅ has · 🟡 partial · ❌ no · ★ strong in that lane

### 4.1 Capability matrix

| Capability | **AAAX** | **Logto** | **Keycloak** | **Clerk** | **better-auth** | **Auth0 / Cognito** |
|------------|:--------:|:---------:|:------------:|:---------:|:---------------:|:-------------------:|
| Self-host / own data | ✅★ | ✅ | ✅ | ❌ SaaS | ✅ (lib) | 🟡 |
| OIDC AS (discovery/JWKS/code/refresh/cc) | ✅ | ✅★ | ✅★ | ✅ | 🟡 | ✅★ |
| Clone → run (no private Maven) | ✅★ | ✅ | 🟡 heavy | n/a | ✅★ | n/a |
| Spring / JVM native | ✅★ | ❌ | 🟡 | ❌ | ❌ | ❌ |
| Register / me / password reset | ✅ | ✅ | ✅ | ✅★ | ✅★ | ✅ |
| Passwordless OTP | ✅ | ✅ | ✅ | ✅★ | ✅ | ✅ |
| Email OTP | ✅ | ✅★ | ✅ | ✅★ | ✅ | ✅ |
| SMS OTP | 🟡★ webhook/Kafka (no Twilio lock-in) | ✅★ | ✅ | ✅★ | 🟡 | ✅★ |
| TOTP MFA | ✅ | ✅ | ✅★ | ✅★ | 🟡 | ✅★ |
| Passkeys | ❌ later | ✅ | ✅ | ✅★ | ✅ | ✅ |
| Social (Google…)| 🟡 **Google + GitHub** optional | ✅★ | ✅★ | ✅★ | ✅★ | ✅★ |
| SAML SP (login via IdP) | ✅ | ✅ | ✅★ | ✅ | ❌ | ✅★ |
| SAML IdP (serve SAML apps) | ❌ | 🟡/✅ | ✅★ | ✅ | ❌ | ✅★ |
| Admin console UI | ✅ `/admin` | ✅★ | ✅★ | ✅★ | 🟡 | ✅★ |
| Users + OAuth clients admin | ✅ | ✅ | ✅ | ✅ | 🟡 | ✅ |
| Orgs / multi-tenant | ❌ **single** | ✅★ | ✅★ | ✅★ | 🟡 | ✅★ |
| Fine RBAC | 🟡 roles + scopes | ✅ | ✅★ | ✅ | 🟡 | ✅★ |
| Audit log | ✅ basic | 🟡/✅ | ✅★ | ✅ | ❌ | ✅★ |
| SDKs / drop-in components | ❌ | ✅★ | 🟡 | ✅★ | ✅★ | ✅★ |
| Seat-tax free | ✅★ | ✅ OSS | ✅ | ❌ | ✅ | ❌ |

### 4.2 Scorecard (100 = credible pick in that lane)

| Lane | AAAX | Logto | Keycloak | Clerk | better-auth |
|------|-----:|------:|---------:|------:|------------:|
| Self-host OIDC core | **78** | 88 | 92 | — | 55 |
| Admin / ops UX | **55** | 85 | 80 | 95 | 40 |
| Auth methods breadth | **58** | 85 | 95 | 95 | 75 |
| Enterprise federation | **40** (SP) | 70 | **95** | 80 | 15 |
| App DX (SDK/components) | **25** | 80 | 45 | **98** | **90** |
| Spring/JVM fit | **95** | 20 | 50 | 10 | 5 |
| Time-to-first-token | **90** | 80 | 40 | 95 hosted | 90 |

### 4.3 Per-competitor

| vs | AAAX wins | AAAX loses |
|----|-----------|------------|
| **Logto** | JVM/Spring AS; SMS Kafka/webhook for in-house notify | Orgs, passkeys, social pack, JS SDK, console polish |
| **Keycloak** | 10× smaller; clone DX | SAML IdP, LDAP, fine RBAC, federation surface |
| **Clerk** | Self-host, no seats | Hosted components, SDK, passkeys UX |
| **better-auth** | Real OIDC server + clients | Next/TS drop-in DX |
| **Auth0/Cognito** | Cost, control, OSS | Enterprise checklist, SLA, ecosystem |

### 4.4 Honest AAAX state

| ✅ Solid | 🟡 Thin | ❌ Out / later |
|----------|---------|----------------|
| OIDC AS + JWK | Social (Google + GitHub optional) | Passkeys |
| Admin portal `/admin` | SAML **SP only** | SAML **IdP** |
| Users/clients admin | RBAC = roles+scopes | Orgs / multi-tenant |
| TOTP MFA | Audit = basic | JS/mobile SDK |
| OTP console/mail/kafka/sms-webhook | Login UI basic | Drop-in components |
| Bootstrap, booklet, examples | — | LDAP |

### 4.5 Gap priority (if closing on Logto)

1. Multi-tenant orgs (product gate)
2. SAML IdP (product gate)
3. Device trust policy (see [ROADMAP.md](./ROADMAP.md)) — **QR login shipped**
4. Official JS/React SDK / BFF quickstart
5. Broader social (Apple/Microsoft) when needed

Detail: OTP/SMS/SAML → [SMS_SAML.md](./SMS_SAML.md) · Events → [IDENTITY_EVENTS.md](./IDENTITY_EVENTS.md) · Social → [SOCIAL.md](./SOCIAL.md) · Passkeys → [PASSKEYS.md](./PASSKEYS.md)

### 4.6 How we win (deliberate wedge)

**Do not try to out-Clerk Clerk or out-Keycloak Keycloak.**

| We double down | Why competitors are weaker here |
|----------------|----------------------------------|
| **Identity Event Bus** | CloudEvents-ish lifecycle → Kafka / webhook. Platform owns notification-service. |
| **Caller-owned SMS** | `kafka` + `sms` webhook modes — no Twilio tax inside the IdP |
| **Spring/JVM native AS** | Logto is Node; better-auth is app-lib; KC is heavy |
| **Clone → token DX** | Central/public deps, booklet, `/admin`, examples |

Primary story:

> **AAAX authenticates. Your mesh notifies.**  
> Best IdP for teams that already run Kafka + notification-service + Spring.

Spec: [IDENTITY_EVENTS.md](./IDENTITY_EVENTS.md)

Detail: OTP/SMS/SAML ops → [SMS_SAML.md](./SMS_SAML.md) · qs/uaa core parity → [PARITY_QS_UAA.md](./PARITY_QS_UAA.md)

---

## 5. Principles

1. Self-host first  
2. OIDC-grade core  
3. X is mandatory (docs/quickstarts)  
4. Greenfield honesty  
5. Secrets never in git  
6. Product GH org later  

---

## 6. Scope (v0.4)

**In (supported):** Accounts; password + OTP + magic link; OAuth2/OIDC; TOTP MFA; sessions; Identity Event Bus; admin portal; clients/users; hosted sign-in/up/user; SAML SP (opt); Google/GitHub social (opt); SMS via kafka/webhook; Compose + kafka-notify example.  
**Orgs:** **single-realm** only.  
**Opt-in passkeys:** `aaax.passkeys.enabled=true` — registration/assertion verified with **webauthn4j** (still treat as advanced ops; RP ID / origin must match).  
**Out:** SAML IdP, multi-tenant orgs, React SDK, LDAP, every social.

---

## 7. Current status (0.5.0)

| Area | State |
|------|--------|
| Accounts + register + DB login + roles | ✅ |
| JDBC OAuth clients + authorizations | ✅ |
| File-backed RSA JWK | ✅ |
| OTP request/verify + passwordless + magic link | ✅ |
| OTP `console` \| `mail` \| `kafka` \| `sms` webhook | ✅ |
| Identity Event Bus + admin Events | ✅ |
| TOTP MFA | ✅ |
| Sessions list/revoke | ✅ |
| Admin portal `/admin` | ✅ |
| Hosted `/sign-in` `/sign-up` `/user` | ✅ |
| Admin clients + users + audit + settings | ✅ |
| First-admin bootstrap | ✅ |
| SAML 2 SP (external IdP) | ✅ optional |
| Google + GitHub social | ✅ optional profile `social` |
| `prod` profile (no demo seeds) | ✅ |
| Kafka notify compose example | ✅ `examples/compose-kafka-notify/` |
| Orgs multi-tenant | ❌ single only |
| SAML IdP | ❌ |
| Passkeys | 🔒 off default · webauthn4j when enabled |
| Redis multi-node OTP | ✅ `aaax.otp.store=redis` |
| Official JS/React SDK | ❌ |

---

## 8. Architecture

```text
 Browser ──► /sign-in · /oauth2/* (AS) · /admin
 API     ──► JWT /v1/api/**
                │
 accounts · oauth2_* · sessions · passkeys(exp) · jwk file
                │
 IdentityEventBus ──► log · audit DB · buffer · Kafka · webhook
                │
 H2 (dev) / PostgreSQL (Compose) · optional Kafka
```

### Filter chains

| Order | Matcher | Role |
|------:|---------|------|
| 1 | AS | token, authorize, jwks, OIDC |
| 2 | `/v1/api/**` | JWT · `SCOPE_api.read` / admin JWT role |
| 3 | default | session · public auth/register · `/v1/admin/**` needs `ROLE_ADMIN` |

### Packages

| Package | |
|---------|--|
| `account` | Entity, register, UserDetails, seeds |
| `client` | Demo client seed + admin client service |
| `config` | Security, JWK, Kafka bridge, SAML/social |
| `otp` | Store, service, senders |
| `events` | Identity Event Bus |
| `session` | Auth session tracking |
| `passkey` | Experimental WebAuthn store |
| `web` | REST + hosted forwards |

---

## 9. Repo map

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

## 10. Stack & run

- **Java 21** · **Spring Boot 4.1** · Spring Security **7** Authorization Server  
- JPA Accounts · JDBC OAuth · File JWK · H2 / Postgres · Mail  
- Jackson **3** · Maven Central only (enforcer bans private deps)  

```bash
# requires JDK 21+
mvn test
mvn spring-boot:run
```

```bash
cp .env.example .env
mvn -DskipTests package
docker compose up --build
```

---

## 11. Configuration

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

## 12. Demo credentials (local seeds only)

| | |
|--|--|
| User | `demo` / `demo1234` |
| Admin | `admin` / `admin12345` (`ROLE_ADMIN`) |
| Client | `aaax-demo` / `aaax-demo-secret` |

---

## 13. HTTP API

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

## 14. OAuth2 / OIDC

| Endpoint | |
|----------|--|
| `/.well-known/openid-configuration` | Discovery |
| `/oauth2/jwks` | JWKS |
| `/oauth2/token` | code / refresh / client_credentials |
| `/oauth2/authorize` | Auth code |

---

## 15. Happy path (curl)

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

## 16. Security posture

- Report via GitHub Security Advisories  
- Demo seeds are toys — disable in prod  
- `console` OTP logs codes; use `mail` + real SMTP for shared envs  
- Protect `AAAX_JWK_PATH` file (private key)  
- OTP store is in-memory (single node)  

---

## 17. Deploy checklist

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

## 18. Roadmap

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

## 19. Dev workflow

| | |
|--|--|
| Solo | Push `main` directly |
| Docs | Edit **this booklet** first |
| Tests | `mvn test` before push |
| Lane | 🧪 CTO · aaax · not WIP primary |

---

## 20. Glossary

| Term | |
|------|--|
| AS | Authorization Server |
| JWK | Signing key material |
| OtpSender | Delivery SPI (`console` / `mail`) |
| Registered client | OAuth app row |
| `SCOPE_api.read` | JWT authority for `/v1/api/**` |

---

*AAAX booklet — fix drift here first.*
