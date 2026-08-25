# Changelog

## [0.9.0-SNAPSHOT]

### Structure
- Single Maven module
- `com.aaax.core` — app-core
- `com.aaax.server` — uaa (main: `com.aaax.server.App`)

### OSS hygiene (A+B)
- Scrubbed secrets from main + test `application.yml` (Discord/Kafka/DB defaults empty)
- Neutral brand: `AAAX` invoker, `@aaax.local` accounts, issuer port **8081**
- Removed Quinsic-only HTTP clients: **GrandPay**, **Onboarding**, **Profile**
- Kept optional mesh clients: tenant / idv / util / uaa / discord (placeholder base when unset)
- `JPA_DDL_AUTO` default **validate** (liquibase owns schema)
- Partner OTP template routing simplified to one env template
