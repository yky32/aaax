# Changelog

All notable changes to **AAAX** are documented here.

## [0.5.0-SNAPSHOT] — unreleased

### Overnight batch C (2026-08-21) — resource-server DX
- `examples/resource-server-boot4/` — Boot 4.1 JWT RS + `call.sh`
- Fix curl hello script Bearer token; link from `resource-call.md`

### Overnight batch B (2026-08-21) — Redis-optional OTP/magic store
- `OtpCodeStore` + `MagicLinkTokenStore` SPI
- Default `aaax.otp.store=memory`; `redis` via `RedisTokenStoreConfig` (no broker unless enabled)
- Docs: `examples/redis-otp-store.md`, `examples/compose-redis-otp/`

### Overnight batch A (2026-08-20)
- All login paths (password/OTP/magic/social/SAML/passkey) → `FinishAuthenticatedSession`
- Magic link logic folded into `MagicLinkUseCase` (no `MagicLinkService`)
- README sequence diagram (mermaid) for Event Bus wedge
- CODEMAP + OVERNIGHT_LOG

### Application layer (UseCase)
- Split `AccountService` → `account.application.*UseCase` + `AccountQueries`
- Auth flows → `auth.application`
- Controllers thin; docs: `ARCHITECTURE.md` + `CODEMAP.md`
- Candidates still open: Redis OTP store, passkey crypto, resource-server example

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
