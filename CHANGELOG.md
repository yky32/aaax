# Changelog

All notable changes to **AAAX** are documented here.

## [0.4.0] — 2026-08-20

**Release focus:** Identity Event Bus as primary product wedge + production-shaped Kafka path + honest v0.4 scope.

### Supported
- OIDC Authorization Server (Boot 4.1 / JDK 21 / Security 7)
- Accounts, password, OTP (`console`/`mail`/`kafka`/`sms` webhook), magic link
- TOTP MFA, session list/revoke
- **Identity Event Bus** → log · audit · buffer · Kafka · webhook
- Admin portal `/admin/` (users, clients, MFA, events, audit, settings)
- Hosted `/sign-in/` · `/sign-up/` · `/user/`
- Social Google + GitHub (profile `social`)
- SAML 2 SP (optional)
- `examples/compose-kafka-notify/` — AAAX + Kafka + sample notify consumer
- Standalone OSS: Maven Central + Shibboleth OpenSAML only

### Experimental
- **Passkeys** — WebAuthn options + credential store; full assertion crypto verify **not** claimed for production MFA

### Explicitly out of 0.4
- Multi-tenant organizations
- SAML IdP
- Official React/JS SDK
- LDAP

### Docs
- Booklet §4 competitive + §4.6 wedge · `IDENTITY_EVENTS` · `CLERK_PARITY` · `SOCIAL` · `SMS_SAML`

## [0.3.0] — 2026-08-20
- Developer product launch surface

## [0.2.0] — 2026-08-20
- Greenfield rewrite

## [0.1.0] — 2026-08-20
- Skeleton
