# Changelog

All notable changes to **AAAX** are documented here.

## [0.4.0-SNAPSHOT] — 2026-08-20

### Admin portal
- Built-in UI at `/admin/` (static SPA in jar — no Node build)
- Dashboard, users, OAuth clients, TOTP MFA, audit, settings

### Competitor-level core
- First-admin bootstrap API
- TOTP MFA (RFC 6238)
- Session login API with MFA step-up
- Audit events + admin settings / feature flags
- Optional Google OIDC (`application-google.yml`)
- Decision blockers listed in settings API

### Stack
- Spring Boot 4.1 + Java 21 + Security 7 AS
- Maven Central only

## [0.3.0] — 2026-08-20
- Developer product launch, examples, standalone identity core

## [0.2.0] — 2026-08-20
- Greenfield rewrite

## [0.1.0] — 2026-08-20
- Skeleton
