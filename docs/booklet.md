# AAAX Booklet

**Accounts · Authentication · Authorization · eXperiences**

| | |
|--|--|
| **This file** | Product + eng source of truth — **code wins if this drifts** |
| **Repo** | https://github.com/yky32/aaax |
| **Version** | `0.9.0-SNAPSHOT` on `main` |
| **Stack** | JDK **21** (build) · Java **17+** · Spring Boot **3.1.0** · Apache-2.0 |
| **Local** | `~/Documents/git/personal/aaax` |
| **Updated** | 2026-08-26 |

> Root `README.md` = shop window (five-minute local).  
> Other files under `docs/` are stubs that point here.

---

## 1. What this is

**AAAX** is **qs/uaa + app-core in one public Maven jar**.

- Packages: `com.aaax.core` (foundation) · `com.aaax.server` (IdP)
- Main: `com.aaax.server.App`
- Identity: `User` 1:n `Authentication` (`loginType` + `identifier`)
- Errors: `BizException(Response)` → `BaseGlobalExceptionHandler` → `R` / `Result`

It is **not** a Clerk/Logto clone, **not** a Boot 4 rewrite, **not** a private Quinsic dump with `app-core` Maven.

**ICP:** Spring/JVM teams that already know uaa-shaped services and want that craft without private packages.

**Bet:** self-host OIDC-grade AAA with the same layering as qs/uaa.

---

## 2. Honest status (0.9)

| | |
|--|--|
| Single jar, Central Maven, no private `app-core` | ✅ |
| Postgres + Redis local (compose) | ✅ |
| OIDC discovery / JWKS / `/oauth2/token` | ✅ |
| Custom grants wired (see §5) | ✅ |
| Google + Apple idToken (third-party grant) | ✅ |
| Register / OTP / forgot-password (qs paths) | ✅ |
| OSS mesh strip: GrandPay / Onboarding / Profile / Tenant / IDV HTTP | ✅ |
| Discord blank = no-op · Util gated · Kafka consumers default **off** | ✅ |
| Demo JKS **not** in the jar | ✅ ephemeral RSA if env unset; file via `AAAX_JWK_KEYSTORE` |
| Hosted `/admin` · `/sign-in` · Event Bus catalog · `/v1/accounts` | ❌ stale greenfield — **not in this tree** |
| Passkeys · SAML IdP · orgs | ❌ |
| Boot **4.1** | ❌ later lane — parent is **3.1.0** (OSS EOL) |
| `mvn test` | ✅ unit + Testcontainers IT (Docker CLI IT excluded from default surefire) |

**Spring Boot 3.1 OSS support ended 2024-06.** This pin matches upstream qs/uaa. Do not claim production IdP hardening on 3.1. Upgrade is an explicit later lane — not silent.

---

## 3. Layout

```text
src/main/java/com/aaax/
├── core/      ← app-core (BizException, R/Result, AuditEntity, …)
└── server/    ← uaa (endpoint, usecase, entity/po/<domain>, OIDC)
    └── App.java
```

Layering: Endpoint → UseCase → Repository → Entity. Copy qs/uaa; do not invent a parallel tree.

---

## 4. HTTP (qs paths)

Public (resource chain): register `/users/registrations` · `/users` · `/ext/users` · OTP `/authentications/one-time-passwords/**` · forgot `/users/credentials/**` · `/keys/public-keys` · `/ws/**` · actuator/swagger.

Auth’d JWT: `/users/me` · profiles · devices · preferences · metadata · permissions · RBAC templates · clients · system-configurations · api-keys · mgt · verification **query**.

OAuth/OIDC: `/oauth2/*` · discovery · JWKS. Issuer default `http://localhost:8081`.

There is **no** `/v1/accounts` API on this tree.

---

## 5. Grants

**Wired** on `/oauth2/token`:

| `grant_type` | Notes |
|--------------|--------|
| `custom-password-grant` | Primary password |
| `custom-password-grant:e` | Encrypted password |
| `urn:ietf:params:oauth:grant-type:custom_code` | Custom code |
| `refresh-token` | Custom refresh |
| `ext-password-grant` | Ext password |
| `third-party-grant` | Google / Apple idToken |
| authorization_code / client_credentials | SAS defaults |

**On disk, not wired:** `qrcode/` and `sms/` grant converters. QR has `POST /devices/qr-code-login` + WS.

**LoginType enum:** `USERNAME · MOBILE · EMAIL · GOOGLE · FACEBOOK · APPLE · LINE · OTP` · `GRANDPAY` reserved. Social **verify** path = Google + Apple only.

---

## 6. Run locally

See README **Five minutes**. First empty DB: `JPA_DDL_AUTO=update` and `LIQUIBASE_ENABLED=false`.

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home
docker compose up -d
cp .env.example .env && set -a && source .env && set +a
mvn -Dmaven.test.skip=true package
java -jar target/aaax-0.9.0-SNAPSHOT.jar
./scripts/quickstart-smoke.sh
# token (env required — no client/user seed on empty DB):
# AAAX_CLIENT_ID=… AAAX_CLIENT_SECRET=… AAAX_USERNAME=… AAAX_CREDENTIALS=… ./scripts/token-smoke.sh
```

Smoke identities (tests, not auto-inserted): `uaa.smoke.primary@aaax.local` / `SmokePrimary!1` · client `client`/`secret`. Token grant: `custom-password-grant` + form field `credentials` (not `password`). Response field: `data.accessToken`.

---

## 7. Configuration

One `application.yml`. Secrets **env only**.

| Knob | Default |
|------|---------|
| `SERVER_PORT` | `8081` |
| `AS_ISSUER` | `http://localhost:8081` |
| `AAAX_UTIL_ENABLED` | `false` |
| Kafka consumers | all `false` |
| `AAAX_JWK_KEYSTORE` | empty → **ephemeral RSA** (tokens die on restart) |
| `AAAX_ENCRYPTION_KEYSTORE` | empty → ephemeral RSA |

File keystores: set path **and** password **and** alias. Nothing ships in the jar.

---

## 8. Security posture

- No demo JKS in the classpath. Unset env = ephemeral keys for local clone only.
- Production: `AAAX_JWK_KEYSTORE` (+ password/alias) pointing at a file you control.
- Discord / ELK webhooks no-op when id/token blank.
- CSRF is **disabled** on the resource chain (uaa copy).
- Report vulns via GitHub Security Advisories (`SECURITY.md`).

---

## 9. OSS strip

**Removed HTTP mesh (do not re-add without an explicit ask):** GrandPay · Onboarding · Profile · Tenant · IDV.

**Kept local:** `User` / `Authentication` · `UserRoute` (opaque `tenantRoleRouteId`, no tenant-service call) · `UserVerification` list/get/patch (external IDV start throws) · Util client gated · Uaa Retrofit client (placeholder URL, unused on register).

---

## 10. Out of scope until asked

- Boot 4.x upgrade
- Wiring QR/SMS grants into `tokenEndpoint`
- Product web (`aaax-www`) claims beyond this booklet
- Re-adding Tenant/IDV/GrandPay mesh
- Inventing a greenfield exception stack or `/v1` overlay
