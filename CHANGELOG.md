# Changelog

All notable changes to **AAAX** are documented here.

## [0.6.0-SNAPSHOT] — unreleased

### P1 Event Bus production-grade
- Frozen **catalog v1.0** (`IdentityEventCatalog`) + `GET /v1/admin/events/catalog`
- Events carry `dataschema`, `data.eventId`, `data.catalogVersion`
- Webhook: **HMAC** (`AAAX_EVENTS_WEBHOOK_SECRET`), delivery id headers, retries
- Audit rows store **`eventId`** correlated to bus id
- OTP `com.aaax.otp.dispatch` remains single OTP signal (kafka channel = bus-only delivery)

### Core foundation package
- `com.aaax.core` — `AuditableEntity`, `BizException`, `Ids`, `GlobalExceptionHandler`
- **Docs centralized:** `docs/booklet.md` (single SoT; other `docs/*` are stubs)

### Naming
- HTTP classes renamed `*Controller` → `*Endpoint` (Spring annotations unchanged)

### CI
- Fixed false-positive ban on enforcer `<exclude>` lines mentioning quinsic/app-core
- Enforcer runs execution `enforce-java-and-oss`

### Trusted devices
- Cookie `AAAX_DEVICE` + hashed store; optional skip TOTP on password login
- `/v1/devices` CRUD; sign-in checkbox; `/user/` management

### QR code login
- `POST/GET /v1/auth/qr/sessions/*` + approve/consume
- Hosted `/sign-in/` QR tab + `qr-approve.html`

Development follows **v0.5.0**.

## [0.5.0] — 2026-08-21

### Highlights
- **UseCase application layer** (no GodService)
- **Pluggable OTP/magic store** — `memory` (default) | `redis`
- **All logins** → `FinishAuthenticatedSession` (password/OTP/magic/social/SAML/passkey)
- **Resource server example** — Boot 4.1 JWT RS (`examples/resource-server-boot4/`)
- **Passkeys** — webauthn4j registration + assertion verify; **off by default** (`AAAX_PASSKEYS_ENABLED`)
- OSS tour: CODEMAP, mermaid Event Bus path, overnight A–D polish

### Supported
- OIDC AS, accounts, password, OTP, magic link, TOTP MFA, sessions
- Identity Event Bus (log/audit/buffer/Kafka/webhook)
- Hosted `/sign-in` `/sign-up` `/user` `/admin`
- Google/GitHub social (opt), SAML SP (opt)
- SMS dual-mode (Kafka | webhook)

### Opt-in
- Passkeys when `aaax.passkeys.enabled=true` (webauthn4j verified; RP/origin must match)
- Redis OTP store when `aaax.otp.store=redis`

### Out of 0.5
- Multi-tenant orgs, SAML IdP, official React SDK, LDAP

## [0.4.0] — 2026-08-20

Event Bus wedge + hosted experiences + Kafka notify example. See git tag `v0.4.0`.
