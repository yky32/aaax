# AAAX

**Accounts · Authentication · Authorization · eXperiences**

### Identity you run.

Self-host **OIDC-grade** auth for your apps — without SaaS seat tax, without Keycloak weight, without private Maven monorepos.

```text
clone → mvn test → spring-boot:run → token → call API
```

[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](./LICENSE)
[![Java](https://img.shields.io/badge/Java-21+-orange.svg)](./pom.xml)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1-green.svg)](./pom.xml)

| | |
|--|--|
| **Docs** | [Booklet](./docs/AAAX_BOOKLET.md) · [Parity](./docs/PARITY_QS_UAA.md) · [SMS/SAML](./docs/SMS_SAML.md) · [Changelog](./CHANGELOG.md) |
| **Examples** | [examples/](./examples/) |
| **Version** | `v0.4.0-SNAPSHOT` (Boot 4.1 / JDK 21) |
| **Maven** | Central + Shibboleth OpenSAML (public) — no private packages |

---

## 5-minute path

**Need:** JDK **21**+, Maven 3.9+

```bash
git clone https://github.com/yky32/aaax.git
cd aaax
mvn test                 # Central only — no private tokens
mvn spring-boot:run
```

**Another terminal:**

```bash
# health
curl -sS http://localhost:8081/actuator/health

# client_credentials → protected API
./examples/curl/get-token-and-hello.sh

# or one-liners:
TOKEN=$(curl -sS -u 'aaax-demo:aaax-demo-secret' \
  -X POST http://localhost:8081/oauth2/token \
  -d 'grant_type=client_credentials&scope=api.read' | jq -r .access_token)
curl -sS http://localhost:8081/v1/api/hello -H "Authorization: Bearer $TOKEN" | jq
```

**Docker alternative:**

```bash
mvn -DskipTests package
docker compose up --build
```

---

## Admin portal

Open after `mvn spring-boot:run`:

**http://localhost:8081/admin/**

- Sign in: `admin` / `admin12345` (demo seed)  
- Users · OAuth clients · TOTP MFA · Audit · Settings  
- First-time (no admin): in-portal **bootstrap** form  

Static UI ships in the jar (`src/main/resources/static/admin`) — clone & run, no Node build.

### OTP / SMS modes (no Twilio lock-in)

| `AAAX_OTP_CHANNEL` | Behavior |
|--------------------|----------|
| `console` | Log codes (default) |
| `mail` | SMTP |
| `kafka` | **Mode 1** — publish `OtpDispatchEvent` to Kafka; caller owns notification-service/SMS |
| `sms` | **Mode 2** — HTTP webhook to your provider URL (`AAAX_OTP_SMS_WEBHOOK_URL`) |

### SAML 2 SP

```bash
export AAAX_SAML_ENABLED=true
export AAAX_SAML_IDP_METADATA_URI=https://idp.example.com/metadata
mvn spring-boot:run
# Login: /saml2/authenticate/idp
```

Orgs: **single-realm** only. Passkeys: later.

---

## What you get

| Area | Capability |
|------|------------|
| **Accounts** | Register, me, change/forgot/reset password, admin users |
| **Authentication** | Form login, OTP (+ mail channel), OIDC authorize/token/jwks |
| **Authorization** | Roles, OAuth clients admin, scopes, sample protected API |
| **X · DX** | Booklet, curl pack, qs/uaa-ish public aliases, standalone clone |

**Not** a Quinsic dump. **Not** Keycloak-in-a-box. Core identity for builders.

---

## Demo credentials (local seeds only)

| | |
|--|--|
| User | `demo` / `demo1234` |
| Admin | `admin` / `admin12345` |
| OAuth client | `aaax-demo` / `aaax-demo-secret` |

Disable seeds in prod: `SPRING_PROFILES_ACTIVE=prod` or `AAAX_DEMO_SEED_*=false`.

---

## Who is this for?

- Java/Spring teams who want OIDC without Keycloak ops  
- Indie / small B2B products that must **own** identity data  
- Platform eng wiring a few SPAs + APIs behind one issuer  

**Job-to-be-done:** *login + token + protect an API in one day, on my DB.*

---

## Docs map

| Doc | |
|-----|--|
| [docs/AAAX_BOOKLET.md](./docs/AAAX_BOOKLET.md) | **Source of truth** (API, deploy, architecture) |
| [docs/PARITY_QS_UAA.md](./docs/PARITY_QS_UAA.md) | Core parity vs production UAA (honest gaps) |
| [examples/README.md](./examples/README.md) | Curl recipes + resource-call sample |
| [CHANGELOG.md](./CHANGELOG.md) | Releases |

---

## Project hygiene

```bash
./scripts/verify-standalone.sh   # Central-only + tests + no private deps
```

Apache-2.0 · Report security issues via GitHub Security Advisories · [SECURITY.md](./SECURITY.md)

---

*AAAX — four letters, one job: identity you can run and ship with.*
