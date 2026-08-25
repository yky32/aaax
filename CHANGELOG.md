# Changelog

## [0.9.0-SNAPSHOT]

### Structure
- Single Maven module
- Packages:
  - `com.aaax.core` — qs app-core
  - `com.aaax.server` — qs uaa (entity / endpoint / usecase / …)
- Main: `com.aaax.server.App` (`scanBasePackages` = core + server)
- Product name **AAAX** · Boot **3.1.0**
- Credentials scrubbed from `application.yml` (env-only)
