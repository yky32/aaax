# Changelog

## [0.9.0-SNAPSHOT]

### Wave 6
- RFC 8414: `/.well-known/oauth-authorization-server` is public (AS chain no longer requires auth); OIDC discovery enabled
- CI / `scripts/quickstart-smoke.sh` require 8414 + JWKS + OIDC discovery (no more “WARN: no discovery”)
- Local seed public client `aaax-pkce` (`requireProofKey=true`, no secret). `scripts/pkce-smoke.sh` checks authorize rejects missing `code_challenge`. Not RFC 8252 (no hosted browser login)

### Wave 5
- App JSON is Jackson **3** (`JsonMapper` / `tools.jackson`): `JSONUtil`, Redis (`GenericJacksonJsonRedisSerializer`), MVC exception mapping
- Dropped `spring-boot-jackson2` and Retrofit `converter-jackson` (Jackson 2). Retrofit uses a local `Jackson3ConverterFactory` until Square ships one
- Instant fields use built-in Java time; no jsr310 `InstantSerializer` on POs

### Wave 4
- Spring Boot **4.1.1** (Framework 7 / Security 7 / Hibernate 7); **Java 21** required
- Starters: `webmvc`, `security-oauth2-authorization-server`, `security-oauth2-resource-server`; Kafka via `spring-boot-starter-kafka`
- Hibernate 7: `@SnowflakeId` (`@IdGeneratorType`) replaces `@GenericGenerator`; JSON columns use `@JdbcTypeCode(SqlTypes.JSON)`
- Authorization server filter chain uses `HttpSecurity.oauth2AuthorizationServer` (Security 7); `DaoAuthenticationProvider` takes `UserDetailsService` in the constructor
- Jackson 3 landed in Wave 5 (this tree used `spring-boot-jackson2` during Wave 4)

### Wave 3
- Snowflake IDs use a 12-bit sequence cap (`1 << 12`), not XOR (`2 ^ 12` → 14)
- Password policy: default min 8 chars (`aaax.security.password-patterns`); system config still overrides
- Login lockout: `aaax.security.max-login-attempts` (default 5), incremented in-process (Kafka off does not skip it)
- Device binding **OFF** on login; `TRUST_LATEST` only applies to `POST /user-devices`
- Stub QR/SMS/`custom_code` grants refuse to mint tokens (no hardcoded `123`)
- Java types, packages, and error codes: `UAA*` → `AAAX*` (e.g. occupied register is `AAAX0409`)

### Wave 2
- `/oauth2/token` returns RFC 6749 JSON (`access_token`, `token_type`, `expires_in`, `refresh_token`). `/users` and admin APIs keep the AAAX Result envelope
- Refresh grant is `refresh_token`
- Dropped unused `spring-security-oauth2` 2.5, `jjwt`, and `spring-security-jwt`
- Removed `ext-password-grant` and `third-party-grant` from `/oauth2/token` (classes deleted). Google/Apple idToken verify/link remains on `SocialAuthenticationUseCase`.

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

- Booklet / README / SECURITY aligned to the live tree (no v0.7 buffet)
- JWK + encryption: **no JKS in the jar**; ephemeral RSA when env unset; file path+password+alias otherwise
- Smoke mixed-case email domain `@aaax.local`
- Drop unused core leftovers (`CardBrand`, empty tenant PaymentGateway metadata)
- Register no longer imports unused `AaaxApiClient`
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
