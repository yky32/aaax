# AAAX Booklet

**Accounts · Authentication · Authorization · eXperiences**

| | |
|--|--|
| **This file** | **Single source of truth** for product + eng |
| **Repo** | https://github.com/yky32/aaax |
| **Version** | tag **`v0.7.0`** · main **`0.8.0-SNAPSHOT`** |
| **Stack** | JDK **21** · Spring Boot **4.1** · Apache-2.0 |
| **Local** | `~/Documents/git/personal/aaax` |
| **Updated** | 2026-08-21 |

> Root `README.md` = shop window.  
> All other files under `docs/` are **stubs** that point here (keep one place editable).

---

## Table of contents

1. [One-liner & bet](#1-one-liner--bet)
2. [Developer product](#2-developer-product)
3. [Four letters](#3-four-letters)
4. [Competitive frame & wedge](#4-competitive-frame--wedge)
5. [Principles](#5-principles)
6. [Scope & status](#6-scope--status)
7. [Architecture & packages](#7-architecture--packages)
8. [Code map (clone tour)](#8-code-map-clone-tour)
9. [Core foundation](#9-core-foundation)
10. [Stack & run](#10-stack--run)
11. [Configuration](#11-configuration)
12. [Demo credentials](#12-demo-credentials)
13. [HTTP API](#13-http-api)
14. [OAuth2 / OIDC](#14-oauth2--oidc)
15. [Identity Event Bus](#15-identity-event-bus)
16. [OTP · SMS · SAML · Social](#16-otp--sms--saml--social)
17. [Passkeys](#17-passkeys)
18. [QR login](#18-qr-login)
19. [Trusted devices](#19-trusted-devices)
20. [Happy path (curl)](#20-happy-path-curl)
21. [Security](#21-security)
22. [Deploy checklist](#22-deploy-checklist)
23. [Roadmap](#23-roadmap)
24. [Clerk / qs-uaa parity (honest)](#24-clerk--qs-uaa-parity-honest)
25. [Dev workflow](#25-dev-workflow)
26. [Glossary](#26-glossary)

---

## 1. One-liner & bet

> **AAAX** — open AAA with experiences: own your identity stack without giving up UX/DX.

**X is the product bet:** self-host *and* feel good to integrate.

---

## 2. Developer product

AAAX is an **IT / developer product**, not an internal monorepo extract.

| # | Promise | Status |
|---|---------|--------|
| 1 | `git clone && mvn test` — **Maven Central** (+ public Shibboleth OpenSAML) | ✅ |
| 2 | `spring-boot:run` → health + token + API | ✅ |
| 3 | Register → OTP / password → `/me` | ✅ |
| 4 | Admin OAuth clients + sample protected API | ✅ |
| 5 | Examples pack (curl, Kafka notify, resource-server) | ✅ |
| 6 | Hosted `/sign-in` `/sign-up` `/user` + `/admin` | ✅ |

**ICP:** Spring/JVM platform teams that already run (or want) Kafka + notification-service and need a lean OIDC issuer.

**Not:** Auth0 RFP bait · 1:1 qs/uaa dump · Clerk SaaS clone · private Quinsic jars.

---

## 3. Four letters

| | | |
|--|--|--|
| **A** | **Accounts** | People, profiles, identity records |
| **A** | **Authentication** | Password, OTP, OAuth, sessions, tokens |
| **A** | **Authorization** | Roles, clients, scopes |
| **X** | **eXperiences** | Hosted UX + DX + Event Bus |

---

## 4. Competitive frame & wedge

**Positioning:** Spring-native lean self-host OIDC — not Keycloak kitchen-sink, not Clerk hosted UX.

**Legend:** ✅ strong · 🟡 partial / optional · ❌ no/weak · ★ category leader · 🔒 off-by-default

### 4.1 Full capability matrix (2026-08, AAAX v0.5 + 0.6-SNAPSHOT)

| Capability | **AAAX** | **Logto** | **Keycloak** | **Ory** | **Authentik** | **Clerk** | **better-auth** | **Auth0 / Cognito** | **Spring Auth Server alone** |
|------------|:--------:|:---------:|:------------:|:-------:|:-------------:|:---------:|:---------------:|:-------------------:|:----------------------------:|
| Self-host / own data | ✅ | ✅ | ✅ | ✅ | ✅ | ❌ SaaS | ✅ lib | 🟡 | ✅ (you build) |
| OIDC AS (discover/JWKS/code/refresh/CC) | ✅ | ✅★ | ✅★ | ✅★ | ✅ | ✅ | 🟡 | ✅★ | ✅★ |
| Clone → token (minutes) | ✅★ | ✅ | 🟡 heavy | 🟡 multi-svc | 🟡 | n/a hosted | ✅★ app | n/a | 🟡 diy |
| No private / proprietary Maven | ✅★ | ✅ | ✅ | ✅ | ✅ | n/a | ✅ | n/a | ✅ |
| **Spring / JVM native product** | ✅★ | ❌ Node | 🟡 Java heavy | Go | Python | ❌ | ❌ TS | ❌ | ✅ lib only |
| Admin console | ✅ `/admin` | ✅★ | ✅★ | 🟡 | ✅★ | ✅★ | 🟡 | ✅★ | ❌ |
| Hosted sign-in / user | ✅ | ✅★ | 🟡 | 🟡 | ✅ | ✅★ | ✅★ | ✅ | ❌ |
| Password + register | ✅ | ✅ | ✅ | ✅ | ✅ | ✅★ | ✅★ | ✅ | diy |
| Passwordless OTP | ✅ | ✅ | ✅ | ✅ | ✅ | ✅★ | ✅ | ✅ | diy |
| Magic link | ✅ | ✅ | 🟡 | ✅ | ✅ | ✅★ | ✅★ | ✅ | diy |
| SMS without vendor lock-in | ✅★ Kafka/webhook | 🟡 | 🟡 SPI | 🟡 | 🟡 | ❌ their rails | 🟡 | their SMS | diy |
| TOTP MFA | ✅ | ✅ | ✅★ | ✅ | ✅ | ✅★ | 🟡 | ✅★ | diy |
| Passkeys / WebAuthn | 🔒 | ✅ | ✅ | ✅ | ✅ | ✅★ | ✅ | ✅ | diy |
| QR login (phone approve desktop) | ✅ | 🟡 | 🟡 | 🟡 | 🟡 | 🟡 | 🟡 | 🟡 | diy |
| Trusted device / remember MFA | ✅ | ✅ | ✅ | ✅ | ✅ | ✅★ | 🟡 | ✅ | diy |
| Social login pack | 🟡 G+GH | ✅★ | ✅★ | ✅ | ✅ | ✅★ | ✅★ | ✅★ | diy |
| SAML SP | ✅ | ✅ | ✅★ | ✅ | ✅★ | ✅ | ❌ | ✅★ | diy |
| SAML IdP | ❌ | 🟡/✅ | ✅★ | 🟡 | ✅★ | ✅ | ❌ | ✅★ | ❌ |
| Orgs / multi-tenant | ❌ single | ✅★ | ✅★ | ✅ | ✅ | ✅★ | 🟡 | ✅★ | diy |
| Fine RBAC / policies | 🟡 roles+scopes | ✅ | ✅★ | ✅★ | ✅ | ✅ | 🟡 | ✅★ | diy |
| LDAP / AD | ❌ | 🟡 | ✅★ | 🟡 | ✅★ | ❌ | ❌ | ✅ | diy |
| Audit | ✅ basic | 🟡/✅ | ✅★ | ✅ | ✅ | ✅ | ❌ | ✅★ | diy |
| **Outbound identity event bus** | ✅★ first-class | 🟡 webhooks | 🟡 events SPI | ✅★ | 🟡 | 🟡 webhooks | ❌ | ✅ Logs/Hooks | diy |
| Official SPA/mobile SDK | ❌ full SDK · ✅ thin `examples/spa-pkce/aaax.js` + client `aaax-spa` | ✅★ | 🟡 | ✅ | 🟡 | ✅★ | ✅★ | ✅★ | ❌ |
| Seat / MAU tax | ✅★ $0 | ✅ OSS | ✅ | OSS+cloud | ✅ | ❌ | ✅ | ❌ | ✅ |
| Ops weight | Low | Med | **High** | Med–High | Med | none (SaaS) | App-coupled | none (SaaS) | You own all |
| OSS community / stars | Early | Strong | Huge | Strong | Strong | n/a | Strong | n/a | Spring eco |

### 4.2 Scorecard (0–100 = credible pick in that lane)

| Lane | AAAX | Logto | Keycloak | Ory | Authentik | Clerk | better-auth | SAS alone |
|------|-----:|------:|---------:|----:|----------:|------:|------------:|----------:|
| Self-host OIDC core | **78** | 88 | 92 | 90 | 85 | — | 55 | 70 |
| Admin / ops UX | **58** | 85 | 82 | 65 | 88 | 95 | 40 | 15 |
| Auth methods breadth | **68** | 86 | 95 | 85 | 88 | 95 | 78 | 30 |
| Enterprise federation | **42** | 72 | **95** | 80 | 90 | 80 | 15 | 20 |
| App DX (SDK / components) | **28** | 82 | 48 | 75 | 45 | **98** | **92** | 25 |
| **Spring / platform mesh fit** | **95** | 22 | 55 | 40 | 25 | 12 | 8 | 90 |
| Time-to-first-token | **90** | 82 | 42 | 55 | 50 | 96 hosted | 90 | 50 |
| **Event-driven notify mesh** | **92** | 55 | 50 | 78 | 45 | 50 | 20 | 25 |
| Clone honesty (no private deps) | **95** | 90 | 70 | 85 | 85 | — | 95 | 95 |
| Market / community | **15** | 80 | **95** | 78 | 70 | 90 | 82 | 85 (Spring) |

### 4.3 When to pick whom

| If you need… | Pick | Not AAAX because |
|--------------|------|------------------|
| Enterprise IdP + LDAP + every protocol | **Keycloak** / **Authentik** | Federation surface thin |
| Beautiful multi-tenant + JS SDK fast | **Logto** or **Clerk** | Orgs + SDK missing |
| Next.js drop-in session lib | **better-auth** | AAAX is a **server**, not an app lib |
| Cloud SLA + compliance checkbox | **Auth0 / Cognito** | Not a managed service |
| Micro-IAM building blocks | **Ory** | You assemble; AAAX is one jar product |
| **Spring AS + Kafka notify mesh, no seat tax** | **AAAX** | — |

### 4.4 Head-to-head (AAAX wins / loses)

| vs | AAAX wins | AAAX loses |
|----|-----------|------------|
| **Logto** | JVM AS; Event Bus + SMS dual-mode; QR + trusted device in-box | Orgs, passkeys default, JS SDK, console polish, community |
| **Keycloak** | 10× lighter; clone DX; intentional event wedge | SAML IdP, LDAP, fine RBAC, ecosystem, ops mindshare |
| **Ory** | One-process product story; Spring shop fit | Modular depth, cloud option, policy engine |
| **Authentik** | Spring-native; simpler Java mental model for JVM teams | UI maturity, IdP breadth, community |
| **Clerk** | Self-host, $0 seats, your SMS rails | Components, hosted UX perfection, passkeys UX, brand |
| **better-auth** | Real OIDC **server** + clients admin + RS example | TS DX, install-in-app ergonomics |
| **Auth0/Cognito** | Cost, data ownership, OSS auditability | Enterprise checklist, SLA, global edge |
| **SAS alone** | Product shell: accounts, admin, events, hosted UX | You still wrote all of that yourself |

### 4.5 Honest AAAX state (post-0.6-SNAPSHOT)

| ✅ Solid | 🟡 Thin | ❌ Out |
|----------|---------|--------|
| OIDC AS + JWK | Social pack (G+GH only) | Orgs / multi-tenant |
| Event Bus + admin Events | Admin UX polish | SAML IdP |
| OTP + kafka/sms dual | Audit depth | LDAP / AD |
| TOTP + trusted device | Passkeys default-off | Official React SDK |
| Magic link + QR login (+ Redis store) | Passkeys default-off | Drop-in components |
| Hosted sign-in/up/user | RBAC = roles+scopes | Huge community |
| `/admin` clients/users | — | — |
| Resource-server example | — | — |
| Booklet SoT + Central-only | — | — |

### 4.6 How we win (deliberate wedge)

**Do not try to out-Clerk Clerk or out-Keycloak Keycloak.**

| We double down | Why competitors are weaker here |
|----------------|----------------------------------|
| **Identity Event Bus** | Lifecycle → Kafka/webhook first-class; platform owns notify |
| **Caller-owned SMS** | `kafka` + `sms` webhook — no Twilio tax inside IdP |
| **Spring/JVM product** | Logto Node; better-auth app-lib; KC heavy; SAS = bare library |
| **Clone → token DX** | Central/public deps, booklet, examples, CI |

Primary story:

> **AAAX authenticates. Your mesh notifies.**  
> Best lean IdP for teams that already run Kafka + notification-service + Spring.

### 4.7 One-line map

```text
Clerk        ──►  best hosted UX / components
Keycloak     ──►  best enterprise kitchen sink
Logto        ──►  best modern self-host + JS DX
Ory          ──►  best composable cloud-native IAM parts
Authentik    ──►  best all-in-one FOSS IdP UI (non-JVM)
better-auth  ──►  best install-into-Next auth lib
AAAX         ──►  best lean Spring OIDC + event mesh handoff
SAS alone    ──►  best raw Spring Security building block
```

---

## 5. Principles

1. Self-host first  
2. OIDC-grade core  
3. X is mandatory (docs + quickstarts)  
4. Greenfield honesty — no private `app-core` / `com.quinsic`  
5. Secrets only in env  
6. HTTP types named `*Endpoint` (not `*Controller`)  
7. Foundation in `com.aaax.core` (ledger-style, same jar)

---

## 6. Scope & status

### In (supported)

Accounts · password · OTP · magic link · OIDC · TOTP · sessions · Event Bus · admin · hosted UX · SAML SP · Google/GitHub · SMS kafka/webhook · Redis OTP store · resource-server example · passkeys (opt-in) · **QR login** · **trusted devices** · `com.aaax.core`

### Out / later

SAML IdP · multi-tenant orgs · React SDK · LDAP · strict device allow-list · Apple/Microsoft social

### Status table (0.7.0)

| Area | |
|------|--|
| Accounts + roles + bootstrap admin | ✅ |
| OIDC AS + file JWK | ✅ |
| OTP console/mail/kafka/sms + memory\|redis store | ✅ |
| Magic link | ✅ |
| TOTP MFA | ✅ |
| Sessions list/revoke | ✅ |
| Event Bus catalog v1.0 + HMAC webhook + audit eventId | ✅ |
| `/admin` · `/sign-in` · `/sign-up` · `/user` | ✅ |
| SAML SP · social profile | ✅ optional |
| Passkeys webauthn4j | 🔒 `AAAX_PASSKEYS_ENABLED` |
| QR login | ✅ |
| Trusted devices | ✅ |
| `core` foundation | ✅ |
| Mesh compose example | ✅ `examples/compose-mesh/` |
| Orgs / SAML IdP / React SDK | ❌ |

---

## 7. Architecture & packages

```text
 Browser ──► /sign-in · /oauth2/* (AS) · /admin · /user
 API     ──► JWT /v1/api/**
                │
 accounts · oauth2_* · sessions · devices · passkeys · jwk
                │
 IdentityEventBus ──► log · audit DB · buffer · Kafka · webhook
                │
 H2 (dev) / PostgreSQL · optional Redis · optional Kafka
```

### Filter chains

| Order | Matcher | Role |
|------:|---------|------|
| 1 | AS | token, authorize, jwks, OIDC |
| 2 | `/v1/api/**` | JWT · scopes |
| 3 | default | session · public auth · admin needs `ROLE_ADMIN` |

### Layering (layer-first · qs/uaa neat)

```text
endpoint/<domain>/*Endpoint   HTTP only — no business logic
  ↓
usecase/<domain>/*UseCase     one user intent
  ↓
repository/*  ·  spi/*        persistence / ports
entity/po                     JPA only (@Entity + AuditEntity*)
entity/model                  non-JPA domain (e.g. QR session)
entity/dto/request|response   *RequestDto · Get*|…*ResponseDto
core/                         AuditEntity · BaseResponseDto · BizException · Ids
events/ · config/ · exception/ · service/ (UDS|crypto|seeds only)
```

| Rule | |
|------|--|
| **Layer-first** | Not feature-first packs (`auth/` containing Endpoint+Entity+Repo) |
| **PO** | `@Entity` only · bare `@Column` (no `name=`) · extend `AuditEntity*` |
| **model/** | Non-persistent types only — never mix into `po/` |
| **DTO** | One file · `*RequestDto` / `*ResponseDto` · no bags · records OK |
| **Reusable audit on API** | `core.entity.dto.BaseResponseDto.from(entity)` |
| **service/** | Spring Security UDS · TotpService · AuditService · seeds — **no new business @Service** |

**Clone rule:** open `endpoint/X` + matching `usecase/X` + `entity/po` — never invent a parallel tree.

---

## 8. Code map (clone tour)

| Step | Open | Why |
|-----:|------|-----|
| 1 | ``App.java`` | Boot |
| 2 | `config/SecurityConfig.java` | 3 filter chains |
| 3 | `usecase/auth/PasswordLoginUseCase.java` | Login |
| 4 | `usecase/auth/FinishAuthenticatedSession.java` | All logins end here |
| 5 | `events/IdentityEventBus.java` | Wedge |
| 6 | `endpoint/auth/AuthEndpoint.java` | `/v1/auth/*` |
| 7 | `core/entity/AuditEntity.java` | qs/uaa foundation (@Version) |

```text
com.aaax
├── core/              # AuditEntity* · BaseResponseDto · BizException · Ids
├── config/            # + JpaAuditingConfig
├── endpoint/<domain>/
├── usecase/<domain>/
├── repository/
├── entity/
│   ├── po/            # JPA only
│   ├── model/         # non-JPA domain
│   └── dto/request|response|event
├── service/           # UDS · Totp · AuditService · seeds
├── spi/
├── events/
└── exception/
```

---

## 9. Core foundation

Public stand-in for private **app-core** (same module, not a private jar). Inspired by ledger `com.altech.core`, kept thin.

```text
com.aaax.core
├── entity/AuditableEntity      # created_at / updated_at
├── exception/BizException      # status + code + message
├── id/Ids                      # uuid()
└── web/GlobalExceptionHandler  # JSON errors
```

| Type | Used by |
|------|---------|
| `AuditableEntity` | `Account`, `PasskeyCredential`, `TrustedDevice` |
| `BizException` | Prefer in use cases; `AccountException` extends it |

Domain may depend on core; **core never depends on domain**.  
Enforcer still **bans** private `com.quinsic.*` and `app-core` coordinates.

---

## 10. Stack & run

**Config:** one `src/main/resources/application.yml` only (qs/uaa style).  
No `application-prod|social|google.yml` — Argo/Helm injects env values.



```bash
git clone https://github.com/yky32/aaax.git && cd aaax
git checkout v0.7.0   # or main
mvn test
mvn spring-boot:run
```

```bash
mvn -DskipTests package
docker compose up --build
```

**Mesh golden path (Event Bus + Redis OTP + HMAC webhook):**

```bash
mvn -DskipTests package
cd examples/compose-mesh && docker compose up --build
```

Examples: `examples/curl/` · `examples/compose-mesh/` · `examples/compose-kafka-notify/` · `examples/resource-server-boot4/` · `examples/compose-redis-otp/`

### Production / HA notes

| Mode | OTP/magic/QR store | Events | DB |
|------|-------------------|--------|-----|
| **Dev laptop** | `memory` (default) | log + buffer | H2 |
| Multi-node / prod | `AAAX_OTP_STORE=redis` · `AAAX_QR_STORE=redis` | Kafka and/or signed webhook | Postgres |
| **QR sessions** | `memory` default · **redis** for multi-node (`aaax.qr.store`) | | |

See [compose-mesh](../examples/compose-mesh/).

---

## 11. Configuration

| Env | Default | |
|-----|---------|--|
| `AAAX_ISSUER` | `http://localhost:8081` | Issuer / origin |
| `AAAX_JWK_PATH` | `./data/aaax-jwk.json` | Signing key |
| `AAAX_OTP_CHANNEL` | `console` | `console` \| `mail` \| `kafka` \| `sms` |
| `AAAX_OTP_STORE` | `memory` | `memory` \| `redis` |
| `AAAX_EVENTS_KAFKA_ENABLED` | false | Event bus Kafka |
| `AAAX_EVENTS_WEBHOOK_URL` | — | Event bus webhook |
| `AAAX_PASSKEYS_ENABLED` | false | WebAuthn |
| `AAAX_PASSKEYS_RP_ID` | `localhost` | RP ID |
| `AAAX_QR_TTL_SECONDS` | `120` | QR session TTL |
| `AAAX_DEVICES_TTL_DAYS` | `30` | Trusted device cookie |
| `AAAX_DEVICES_COOKIE_SECURE` | false | HTTPS cookie |
| `AAAX_SAML_ENABLED` | false | SAML SP |
| `AAAX_DEMO_SEED_*` | true | **false in prod** |
| Profile `social` | off | Google/GitHub |
| Profile `prod` | — | no seeds, validate DDL |

---

## 12. Demo credentials

| | |
|--|--|
| User | `demo` / `demo1234` |
| Admin | `admin` / `admin12345` |
| Client | `aaax-demo` / `aaax-demo-secret` |

---

## 13. HTTP API

| Method | Path | Auth |
|--------|------|------|
| GET | `/` | public meta |
| POST | `/v1/accounts/register` | public |
| GET | `/v1/accounts/me` | session |
| POST | `/v1/auth/login` | public → session |
| POST | `/v1/auth/mfa/totp` | pending MFA |
| POST | `/v1/auth/logout` | session |
| POST | `/v1/otp/request` | public |
| POST | `/v1/auth/otp/login` | public → session |
| POST | `/v1/auth/magic/*` | public |
| POST/GET | `/v1/auth/qr/sessions/*` | create/poll/consume public; approve session |
| GET/POST/DELETE | `/v1/sessions` | session |
| GET/POST/DELETE | `/v1/devices` | session |
| GET/POST/DELETE | `/v1/passkeys/*` | session (if enabled) |
| GET/POST/DELETE | `/v1/admin/**` | `ROLE_ADMIN` |
| GET | `/v1/api/hello` | Bearer JWT |

Hosted: `/sign-in/` · `/sign-up/` · `/user/` · `/admin/`

---

## 14. OAuth2 / OIDC

| Endpoint | |
|----------|--|
| `/.well-known/openid-configuration` | Discovery |
| `/oauth2/jwks` | JWKS |
| `/oauth2/token` | code / refresh / client_credentials |
| `/oauth2/authorize` | Auth code |

### Demo clients (local seed)

| client_id | Type |
|-----------|------|
| `aaax-demo` | confidential · secret `aaax-demo-secret` |
| `aaax-spa` | **public** · PKCE required · no secret |

### SPA PKCE (browser)

```bash
# AAAX on :8081
cd examples/spa-pkce && python3 -m http.server 4173
# open http://127.0.0.1:4173/
```

Thin helper: `examples/spa-pkce/aaax.js` (`Aaax.create` → `login` → `handleRedirectCallback` → `fetchJson`).

---

## 15. Identity Event Bus

CloudEvents-ish JSON (**catalog v1.0** — frozen types):

```json
{
  "specversion": "1.0",
  "id": "uuid",
  "source": "http://localhost:8081",
  "type": "com.aaax.auth.login",
  "time": "2026-08-20T12:00:00Z",
  "subject": "demo",
  "dataschema": "aaax:events/catalog/1.0#com.aaax.auth.login",
  "data": { "method": "password", "eventId": "uuid", "catalogVersion": "1.0" }
}
```

**Catalog API:** `GET /v1/admin/events/catalog` (admin session).  
**Compatibility:** additive types OK within catalog major; renames bump `IdentityEventCatalog.VERSION`.

### Types (catalog 1.0)

| type | when |
|------|------|
| `com.aaax.account.registered` | register |
| `com.aaax.auth.login` | password / otp / qr / social |
| `com.aaax.auth.login.mfa` | password + TOTP |
| `com.aaax.auth.login.social` | social |
| `com.aaax.auth.logout` | logout |
| `com.aaax.auth.qr.created` / `.approved` | QR |
| `com.aaax.device.trusted` | remember device |
| `com.aaax.otp.dispatch` | OTP (includes `code`, `channel`, `destination`, `purpose`, `expiresAt`) |
| `com.aaax.mfa.totp.*` | MFA |
| `com.aaax.client.*` | OAuth clients |
| `com.aaax.admin.*` | admin ops |
| `com.aaax.account.federated` / password.* | account |

### Sinks

log · audit DB (**`eventId` correlates bus ↔ audit**) · in-memory buffer · Kafka · webhook

### Webhook (prod)

```bash
export AAAX_EVENTS_WEBHOOK_URL=https://notify.example/hooks/aaax
export AAAX_EVENTS_WEBHOOK_SECRET=long-random   # → X-AAAX-Signature: sha256=<hex>
# optional:
export AAAX_EVENTS_WEBHOOK_AUTH="Bearer …"
export AAAX_EVENTS_WEBHOOK_MAX_ATTEMPTS=3
```

Headers: `ce-id`, `ce-type`, `x-aaax-event-id`, `x-aaax-delivery-id` (idempotency = event id), `x-aaax-signature`.  
Retries: 408 / 429 / 5xx + network errors with backoff.

### OTP + bus

`com.aaax.otp.dispatch` is the **single** OTP signal. Channel `kafka` relies on bus sinks only (no second SMS path). Channel `sms`/`mail`/`console` still emit the same event **and** deliver out-of-band.

---

## 16. OTP · SMS · SAML · Social

### OTP channels

| Channel | |
|---------|--|
| `console` | log (dev) |
| `mail` | SMTP |
| `kafka` | your notify mesh consumes |
| `sms` | HTTP webhook to your SMS gateway |

Store: `AAAX_OTP_STORE=memory|redis`.

### SAML SP

```bash
export AAAX_SAML_ENABLED=true
export AAAX_SAML_IDP_METADATA_URI=https://idp.example/metadata
```

OpenSAML from **Shibboleth public** (`.mvn/settings.xml`). AAAX as SAML **IdP** = not built.

### Social

Profile `social` + `GOOGLE_*` / `GITHUB_*` env.

---

## 17. Passkeys

| | |
|--|--|
| Default | **off** |
| Lib | webauthn4j-core |
| Enable | `AAAX_PASSKEYS_ENABLED=true` |

API: `/v1/passkeys/register|authenticate|…` — **404** when disabled. UI: `/user/` when enabled. RP ID + issuer origin must match browser host.

---

## 18. QR login

```text
Desktop POST /v1/auth/qr/sessions → poll GET …/{id}
Phone (session) open approve URL → POST …/approve
Desktop POST …/consume → FinishAuthenticatedSession (method=qr)
```

UI: `/sign-in/` QR tab · `/sign-in/qr-approve.html?sid=…`  
TTL: `AAAX_QR_TTL_SECONDS` (default 120). Store: **in-memory**.

---

## 19. Trusted devices

Cookie `AAAX_DEVICE` (HttpOnly) · DB stores **SHA-256** hash.  
`rememberDevice: true` on login/MFA → later password login can **skip TOTP**.

| API | |
|-----|--|
| `GET/POST /v1/devices` | list / trust this browser |
| `DELETE /v1/devices/{id}` | revoke |
| `POST /v1/devices/revoke-all` | revoke all |

Not MDM · not passkeys · not strict allow-list (unknown devices still login with MFA).

---

## 20. Happy path (curl)

```bash
# health
curl -sS http://localhost:8081/actuator/health

# client credentials → API
TOKEN=$(curl -sS -u 'aaax-demo:aaax-demo-secret' \
  -X POST http://localhost:8081/oauth2/token \
  -d 'grant_type=client_credentials&scope=api.read' | jq -r .access_token)
curl -sS http://localhost:8081/v1/api/hello -H "Authorization: Bearer $TOKEN"

# password session
curl -sS -c /tmp/aaax.cj -X POST http://localhost:8081/v1/auth/login \
  -H 'content-type: application/json' \
  -d '{"username":"demo","password":"demo1234"}'
curl -sS -b /tmp/aaax.cj http://localhost:8081/v1/accounts/me

# scripts
./examples/curl/get-token-and-hello.sh
./examples/curl/login-admin-and-events.sh
```

---

## 21. Security

- Demo seeds off in prod  
- Protect JWK file  
- Prefer Redis OTP/QR multi-node in HA  
- Device cookie Secure behind HTTPS  
- Passkeys need matching RP/origin + HTTPS (or localhost)  
- Report vulns via GitHub Security Advisories  

---

## 22. Deploy checklist

- [ ] Postgres + migrations / schema  
- [ ] `AAAX_ISSUER` = public https URL  
- [ ] Persistent `AAAX_JWK_PATH` mode 600  
- [ ] `AAAX_DEMO_SEED_*=false` · `JPA_DDL_AUTO=validate` · `SQL_INIT_MODE=never` (Helm env)  
- [ ] OTP mail or kafka/sms webhook  
- [ ] TLS terminator · secure cookies  
- [ ] Backup DB + JWK  
- [ ] Smoke: health, discovery, token, login  

---

## 23. Roadmap

### Shipped

- v0.4–0.5: OIDC, Event Bus, MFA, SMS dual-mode, SAML SP, social, UseCase, Redis OTP, passkeys, resource-server  
- v0.6-SNAPSHOT: QR login · trusted devices · `com.aaax.core` · Endpoint naming · CI green  

### Needs product `go`

| Item | |
|------|--|
| Strict device allow-list | block unknown devices |
| Multi-tenant orgs | |
| SAML IdP | |
| Official React/Next SDK | |
| Apple / Microsoft social | |
| QR/OTP multi-node always-on story polish | |

### Non-goals near term

Keycloak dump · Clerk seat billing · private Quinsic mesh libs  

---

## 24. Clerk / qs-uaa parity (honest)

### Clerk-class experiences (self-host)

| Experience | AAAX |
|------------|------|
| Hosted sign-in / sign-up / user | ✅ |
| Magic link | ✅ |
| Sessions list/revoke | ✅ |
| Passkeys | 🔒 opt-in |
| Orgs | ❌ |
| Drop-in React components | ❌ |

Not a SaaS clone — parity of **surfaces**, not billing.

### vs qs/uaa

| | qs/uaa | AAAX |
|--|--------|------|
| Private `app-core` | ✅ | ❌ banned |
| QR login | product WS | ✅ HTTP poll |
| Device binding | full policy | 🟡 remember/skip-MFA |
| Event mesh | private | public Event Bus |

---

## 25. Dev workflow

- Branch: push **`main`** directly (solo OSS)  
- JDK 21 · `mvn test` before push  
- CI: Central settings · tests · package · dep tree · enforcer `@enforce-java-and-oss`  
- New HTTP type: `*Endpoint`  
- New entity timestamps: extend `AuditableEntity`  
- New write flow: `*UseCase`  
- Changelog: root `CHANGELOG.md`  

---

## 26. Glossary

| Term | |
|------|--|
| **AS** | Authorization Server (OIDC issuer) |
| **Event Bus** | Outbound identity signals |
| **FinishAuthenticatedSession** | Single login completion path |
| **core** | Foundation package (not private app-core) |
| **Endpoint** | HTTP adapter class name |
| **Trusted device** | Cookie + hash; optional MFA skip |

---

*End of booklet. Prefer editing this file over adding new top-level docs.*
