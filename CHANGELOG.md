# Changelog

All notable changes to **AAAX** are documented here.

## [0.6.0-SNAPSHOT] — unreleased

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
