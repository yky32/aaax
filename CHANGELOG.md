# Changelog

## [0.9.0-SNAPSHOT]

### Wave 2
- `/oauth2/token` returns RFC 6749 JSON (`access_token`, `token_type`, `expires_in`, `refresh_token`). `/users` and admin APIs keep the AAAX Result envelope
- Refresh grant is `refresh_token` (legacy `refresh-token` still accepted)
- Dropped unused `spring-security-oauth2` 2.5, `jjwt`, and `spring-security-jwt`
- CORS origins from `AAAX_CORS_ORIGINS` (localhost by default; `*` disables credentials). CSRF remains off for this API-only tree

### Wave 1
- Empty-DB clone: `JPA_DDL_AUTO=update` + Liquibase `oauth2_registered_client` (env, no Spring profile)
- Optional seed (`AAAX_LOCAL_SEED`): OAuth client `client`/`secret` + `smoke.primary@aaax.local`
- Kafka **off** by default (`AAAX_KAFKA_ENABLED=false`) — first clone only needs Postgres + Redis
- `application.yml` grouped into `spring` / `aaax` / `ext`
- `scripts/token-smoke.sh` / `examples/curl/login-me.sh` default to that seed

### Wave 0
- Public docs: AAAX branding only
- Removed `GET /keys/private-keys` and `POST /keys/decryption`
- Google/Apple signup no longer creates a password login (`1234`)
- Unwired stub token grants (`custom_code`, `ext-password-grant`); QR/SMS grants stay off `/oauth2/token`
- JWK: ephemeral RSA documented as local-only; production requires `AAAX_JWK_KEYSTORE`

### Removed
- Stub API-key stack (`ApiKey` PO, `/api-keys` endpoint, use case, repository, create DTO)

### Honest OSS

- Booklet / README / SECURITY aligned to the live tree (Boot 3.1, no v0.7 buffet)
- JWK + encryption: **no JKS in the jar**; ephemeral RSA when env unset; file path+password+alias otherwise
- Smoke mixed-case email domain `@aaax.local`
- Drop unused core leftovers (`CardBrand`, empty tenant PaymentGateway metadata)
- Register no longer imports unused `UaaApiClient`
- Testcontainers on `mvn test` (IT); Docker CLI IT excluded from default surefire

### Structure
- Single Maven module
- `com.aaax.core` — app-core
- `com.aaax.server` — authentication server (main: `com.aaax.server.App`)

### Local DX (C)
- `docker-compose.yml` — Postgres 16 + Redis 7
- `.env.example` — first-boot friendly (`JPA_DDL_AUTO=update`)
- `scripts/quickstart-smoke.sh` — discovery/JWKS probe
- `scripts/token-smoke.sh` — `custom-password-grant`; defaults match local seed
- `examples/curl/` — HTTP recipes (register / OTP / login / me). No events catalog on this tree.
- README **Five minutes** section

### Batch D (standalone OSS)
- Discord webhooks no-op when token blank (`DiscordWebhookSupport`)
- Util CDN/ref-data gated by `AAAX_UTIL_ENABLED=false`
- Kafka consumers **default false**
- OTP notify logs payload for local dev when Kafka absent
- keyID `aaax`; empty `ext/` package removed

### OSS trim
- Removed **Tenant** + **IDV** mesh clients, tenant endpoints, IDV registration/webhook hooks
- User routes stay **local** (opaque `tenantRoleRouteId` optional)

### OSS hygiene (A+B)
- Scrubbed secrets from main + test `application.yml` (Discord/Kafka/DB defaults empty)
- Neutral brand: `AAAX` invoker, `@aaax.local` accounts, issuer port **8081**
- Removed Quinsic-only HTTP clients: **GrandPay**, **Onboarding**, **Profile**, **Tenant**, **IDV**
- Optional leftover: Util (gated) + loopback Retrofit placeholder
- `JPA_DDL_AUTO` default **validate** (liquibase owns schema)
- Partner OTP template routing simplified to one env template
