# Changelog

## [0.9.0-SNAPSHOT]

### Structure
- **Single project**: qs/uaa + app-core merged under `src/`
  - `com.aaax.core` — app-core
  - `com.aaax.*` — uaa
- No multi-module (`aaax-core` / `aaax-server` removed)
- Spring Boot **3.1.0** · product name **AAAX**
- Credentials scrubbed from `application.yml` (env-only)
