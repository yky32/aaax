# AAAX

**Accounts · Authentication · Authorization · eXperiences**

### Identity you run. Signals you own.

Self-host **OIDC** for **Spring / platform teams** — without SaaS seat tax, without Keycloak weight, without private Maven.

**Primary win:** [Identity Event Bus](./docs/booklet.md#15-identity-event-bus)  
→ login · MFA · OTP · clients as CloudEvents → **your** Kafka / webhook / notification-service  
→ **you keep SMS** (no Twilio lock-in)

```text
clone → mvn test → spring-boot:run → token
                 ↘ events → your notify mesh
```

```mermaid
sequenceDiagram
  participant User
  participant AAAX
  participant Bus as IdentityEventBus
  participant Mesh as Your notify / Kafka

  User->>AAAX: login / OTP / magic / social
  AAAX->>AAAX: FinishAuthenticatedSession
  AAAX->>Bus: AUTH_LOGIN / OTP_DISPATCH / …
  Bus->>Mesh: Kafka topic or webhook
  Note over Mesh: You send SMS/email<br/>AAAX does not embed Twilio
  AAAX-->>User: session + OIDC tokens for apps
```

> **ICP:** JVM teams that already run (or want) Kafka + notification-service and need a lean OIDC issuer.  
> Not a Clerk SaaS clone — see [Clerk parity (honest)](./docs/booklet.md#24-clerk--qs-uaa-parity-honest).

[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](./LICENSE)
[![Java](https://img.shields.io/badge/Java-21+-orange.svg)](./pom.xml)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1-green.svg)](./pom.xml)
[![Release](https://img.shields.io/badge/release-v0.6.0-success.svg)](https://github.com/yky32/aaax/releases/tag/v0.6.0)

| | |
|--|--|
| **Docs** | **[Booklet](./docs/booklet.md)** (single SoT) · [Changelog](./CHANGELOG.md) |
| **Examples** | [**Mesh golden path**](./examples/compose-mesh/) · [examples/](./examples/) · [Resource server](./examples/resource-server-boot4/) |
| **Version** | **`v0.6.0`** (Boot 4.1 / JDK 21) |
| **Maven** | Central + Shibboleth OpenSAML (public) — no private packages |

---

## 5-minute path

**Need:** JDK **21**+, Maven 3.9+

```bash
git clone https://github.com/yky32/aaax.git
cd aaax
git checkout v0.6.0   # or main
mvn test
mvn spring-boot:run
```

**Reading the code (OSS tour):** [docs/booklet.md#8-code-map-clone-tour](./docs/booklet.md#8-code-map-clone-tour) — SecurityConfig → login use cases → Event Bus.

**Another terminal:**

```bash
curl -sS http://localhost:8081/actuator/health
./examples/curl/get-token-and-hello.sh
./examples/curl/login-admin-and-events.sh
```

**Docker (Postgres):**

```bash
mvn -DskipTests package
docker compose up --build
```

**Mesh golden path (Postgres + Redis OTP + Kafka + HMAC webhook):**

```bash
mvn -DskipTests package
cd examples/compose-mesh && docker compose up --build
```

---

## What v0.6.0 commits to

| ✅ Supported | 🧪 Opt-in | ❌ Out of 0.6 |
|--------------|----------------|---------------|
| OIDC AS (discover / JWKS / code / refresh / client_credentials) | Passkeys (**off** default; webauthn4j) | Multi-tenant orgs |
| Accounts · password · OTP · magic link · **QR login** | — | SAML IdP |
| TOTP MFA · **trusted devices** · sessions | — | Official React SDK |
| Identity Event Bus **catalog v1.0** · HMAC webhook · Kafka | — | LDAP |
| Admin `/admin/` · hosted sign-in/up/user | — | |
| Social Google + GitHub (optional) | — | |
| Redis OTP store · resource-server example · `com.aaax.core` | — | |
| SAML SP (optional) | — | |
| SMS via **kafka event** or **webhook** (your provider) | — | |

---

## Hosted UI (same jar)

```text
/sign-in/   password · magic · OTP · social
/sign-up/
/user/      profile · sessions · passkeys (if enabled)
/admin/     ops console (admin / admin12345 demo)
```

---

## OTP / SMS (no carrier SDK)

| `AAAX_OTP_CHANNEL` | Behavior |
|--------------------|----------|
| `console` | Log codes (default) |
| `mail` | SMTP |
| `kafka` | Event for **your** notification-service |
| `sms` | HTTP webhook to **your** SMS adapter |

Lifecycle events: `AAAX_EVENTS_KAFKA_ENABLED=true` → topic `aaax.identity.events`.

---

## Social (optional)

```bash
export SPRING_PROFILES_ACTIVE=social
export GOOGLE_CLIENT_ID=...
export GOOGLE_CLIENT_SECRET=...
# optional GitHub — see docs/booklet.md#16-otp--sms--saml--social
```

---

## Demo credentials (local seeds only)

| | |
|--|--|
| User | `demo` / `demo1234` |
| Admin | `admin` / `admin12345` |
| OAuth client | `aaax-demo` / `aaax-demo-secret` |

Prod: `SPRING_PROFILES_ACTIVE=prod` or `AAAX_DEMO_SEED_*=false`.

---

## License

Apache-2.0
