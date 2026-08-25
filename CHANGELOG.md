# Changelog

## [0.9.0-SNAPSHOT]

### Structure
- Single Maven module
- `com.aaax.core` — app-core
- `com.aaax.server` — uaa (main: `com.aaax.server.App`)

### Local DX (C)
- `docker-compose.yml` — Postgres 16 + Redis 7
- `.env.example` — first-boot friendly (`JPA_DDL_AUTO=update`)
- `scripts/quickstart-smoke.sh` — discovery/JWKS probe
- README **Five minutes** section

### Batch D (standalone OSS)
- Discord webhooks no-op when token blank (`DiscordWebhookSupport`)
- Util CDN/ref-data gated by `AAAX_UTIL_ENABLED=false`
- Kafka consumers **default false**
- OTP notify logs payload for local dev when Kafka absent
- keyID `altech-uaa` → `aaax`; empty `ext/` package removed

### OSS trim
- Removed **Tenant** + **IDV** mesh clients, tenant endpoints, IDV registration/webhook hooks
- User routes stay **local** (opaque `tenantRoleRouteId` optional)

### OSS hygiene (A+B)
- Scrubbed secrets from main + test `application.yml` (Discord/Kafka/DB defaults empty)
- Neutral brand: `AAAX` invoker, `@aaax.local` accounts, issuer port **8081**
- Removed Quinsic-only HTTP clients: **GrandPay**, **Onboarding**, **Profile**
- Kept optional mesh clients: tenant / idv / util / uaa / discord (placeholder base when unset)
- `JPA_DDL_AUTO` default **validate** (liquibase owns schema)
- Partner OTP template routing simplified to one env template
