# Changelog

All notable changes to **AAAX** are documented here.

## [0.4.0-SNAPSHOT] — 2026-08-20

### Stack upgrade
- **Spring Boot 4.1.0** + **Java 21** (was Boot 3.3.5 / Java 17)
- OAuth2 AS starter → `spring-boot-starter-security-oauth2-authorization-server` (Security 7)
- Jackson 3 (`tools.jackson`) + `spring-boot-starter-jackson`
- Test stack: `spring-boot-starter-webmvc-test`
- Maven Enforcer: JDK 21+, ban `com.quinsic.*` / `app-core`
- Dockerfile: Temurin **21** JRE, non-root user
- CI: Java 21

### Security 7 API
- `OAuth2AuthorizationServerConfigurer` under `spring-security-config`
- `PathPatternRequestMatcher` replaces Ant path matcher usage

## [0.3.0] — 2026-08-20

### Developer product
- README hero, examples/curl, booklet product chapter, GitHub release `v0.3.0`
- Standalone Central-only Maven, parity matrix, identity core features

## [0.2.0] — 2026-08-20
- Greenfield `com.aaax` rewrite

## [0.1.0] — 2026-08-20
- Initial greenfield skeleton
