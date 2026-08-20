# Changelog

All notable changes to **AAAX** are documented here.

## [0.4.0-SNAPSHOT] — 2026-08-20

### Social login pack
- Google OIDC + GitHub OAuth2 via profile `social` (legacy `google` still works)
- Console **Continue with …** buttons from `GET /v1/auth/social/providers`
- Session link/create account · event `com.aaax.auth.login.social`
- Docs: `docs/SOCIAL.md`

### Win wedge complete — Identity Event Bus
- CloudEvents-ish lifecycle on every auth path (login/MFA/OTP/clients/admin)
- Sinks: log · audit DB · **in-memory buffer** · optional Kafka · optional webhook
- `GET /v1/admin/events` + Admin portal **Events** tab
- OTP always emits `com.aaax.otp.dispatch` (kafka channel = bus-only delivery)
- Examples: `examples/identity-events/consumer.py`, `curl/login-admin-and-events.sh`
- Docs: IDENTITY_EVENTS.md · booklet §4.6 · README tagline

### Also in 0.4 line
- Boot 4.1 / JDK 21 / Security 7 AS
- Admin portal (designed console), TOTP MFA, bootstrap, SAML SP, SMS kafka/webhook modes
- Standalone OSS (Central + Shibboleth OpenSAML)

## [0.3.0] — 2026-08-20
- Developer product launch surface

## [0.2.0] — 2026-08-20
- Greenfield rewrite

## [0.1.0] — 2026-08-20
- Skeleton
