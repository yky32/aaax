# Changelog

All notable changes to **AAAX** are documented here.

## [0.8.0-SNAPSHOT] — unreleased

### Config
- **Single `application.yml` only** (removed `application-prod|social|google.yml`)
- Social + prod knobs via **env / Helm** (qs/uaa style)


Development follows **v0.7.0**.

## [0.7.0] — 2026-08-23

### Highlights
- **Layer-first** package layout (qs/uaa neat): `endpoint/` · `usecase/` · `entity/po|model|dto` · `repository/` · `spi/`
- **AuditEntity** + **AuditEntityWithIsActive** (`@Version`, `createDt`/`updateDt`/`createdBy`/`updatedBy`) + JPA auditing
- **DTO convention:** `*RequestDto` / `Get*|…*ResponseDto` · no bag classes · `BaseResponseDto`
- **QR Redis store** · **SPA PKCE** public client `aaax-spa` + `examples/spa-pkce/`
- **Event Bus P1** (from 0.6 line): catalog v1.0 · webhook HMAC + retry · audit `eventId`
- **Mesh golden path:** `examples/compose-mesh/`
- Booklet §7 structure bar · CONTRIBUTING aligned

### Structure
- `entity/po` = JPA only · `entity/model` = non-JPA (e.g. QR session)
- Business logic in `usecase/*` · `service/` = UDS / Totp / AuditService / seeds only
- Bare `@Entity`/`@Column` (no default `name=`)

### App DX
- Public client `aaax-spa` (PKCE, no secret)
- CORS on `/oauth2/**` · thin helper `examples/spa-pkce/aaax.js`

### Breaking for integrators (vs ad-hoc 0.6 snapshots)
- Package moves: use `endpoint.*` / `usecase.*` / `entity.dto.*` (not `web.*` / `*.application`)
- Account JSON audit field preference: **`createDt`** (and full audit on `GetAccountResponseDto`)

## [0.6.0] — 2026-08-21

### Highlights
- **QR code login** — desktop QR / code, phone approve, consume
- **Trusted devices** — `AAAX_DEVICE` cookie, optional TOTP skip
- **`com.aaax.core`** foundation (pre-AuditEntity rename)
- **HTTP `*Endpoint` naming** (not Controller)
- **Event Bus P1** — catalog v1.0, webhook HMAC + retry, audit `eventId`
- **Docs** — single SoT `docs/booklet.md`
- **Mesh golden path** — `examples/compose-mesh/`
- CI green (enforcer + Central-only checks)

### Event Bus
- `GET /v1/admin/events/catalog`
- `dataschema`, `data.eventId`, `data.catalogVersion`
- Webhook: `X-AAAX-Signature: sha256=` · delivery id headers · retries

### Upgrade from 0.5
- See prior notes in git history for QR / devices / Endpoint rename.
