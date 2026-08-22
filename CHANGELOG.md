# Changelog

All notable changes to **AAAX** are documented here.

## [0.7.0-SNAPSHOT] — unreleased

### Structure — qs/uaa align (stop-feature)
- **AuditEntity** + **AuditEntityWithIsActive** (`@Version`, `createDt`/`updateDt`/`createdBy`/`updatedBy`) + `JpaAuditingConfig`
- Bare `@Entity`/`@Column` (no `name=`) — trust Hibernate naming
- DTOs: `entity/dto/request|*RequestDto`, `response|*ResponseDto`, `event|` — **no bag classes**
- Renames: `GetAccountResponseDto`, `RegisterAccountRequestDto`, `RequestOtp*`, …
- Repos order by `createDt`; dropped invented `AuditableEntity`

### Structure (ledger package tree)
- Packages: `endpoint/<domain>`, `usecase/<domain>`, `repository`, `entity/po|dto`, `service`, `spi`, `exception`
- Business services → UseCase: Passkey/Device/Session/Client/OtpOps
- `service/` = UDS / TOTP crypto / audit / seeds only

### SPA / App DX
- Public OAuth client **`aaax-spa`** (PKCE required, no secret)
- Example: `examples/spa-pkce/` + thin helper `aaax.js`
- CORS on `/oauth2/**` for browser token exchange

### QR multi-node
- `aaax.qr.store=memory|redis` (default memory)
- Redis QR sessions share Redis when `AAAX_QR_STORE=redis` (compose-mesh enables it)

Development follows **v0.6.0**.

## [0.6.0] — 2026-08-21

### Highlights
- **QR code login** — desktop QR / code, phone approve, consume
- **Trusted devices** — `AAAX_DEVICE` cookie, optional TOTP skip
- **`com.aaax.core`** foundation — AuditableEntity, BizException, Ids
- **HTTP `*Endpoint` naming** (not Controller)
- **Event Bus P1** — catalog v1.0, webhook HMAC + retry, audit `eventId`
- **Docs** — single SoT `docs/booklet.md`
- **Mesh golden path** — `examples/compose-mesh/` (Postgres + Redis + Kafka + signed webhook)
- CI green (enforcer + Central-only checks)

### Event Bus
- `GET /v1/admin/events/catalog`
- `dataschema`, `data.eventId`, `data.catalogVersion`
- `AAAX_EVENTS_WEBHOOK_SECRET` → `X-AAAX-Signature: sha256=…`
- Delivery id headers + retries on 408/429/5xx

### Auth / UX
- `/sign-in/` QR tab · `/v1/auth/qr/*`
- Remember device checkbox · `/v1/devices`
- Passkeys still opt-in (`AAAX_PASSKEYS_ENABLED`) with webauthn4j

## [0.5.0] — 2026-08-21

### Highlights
- UseCase application layer (no GodService)
- Pluggable OTP/magic store — `memory` | `redis`
- FinishAuthenticatedSession unified login finish
- Passkeys webauthn4j (opt-in, off by default)
- Resource-server Boot 4 example
- Identity Event Bus E2E · SMS dual-mode · SAML SP · Google/GitHub social
- Hosted `/sign-in` `/sign-up` `/user` · admin portal
