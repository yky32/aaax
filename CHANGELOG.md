# Changelog

All notable changes to **AAAX** are documented here.

## [0.3.0] — 2026-08-20

### Developer product

- README positioned as IT/developer product (5-minute path, ICP, promise table)
- `examples/curl/*` scripts + `examples/resource-call.md`
- Centralized docs: booklet + qs/uaa parity matrix
- `scripts/verify-standalone.sh` — Central-only Maven + no private deps
- GitHub topics for discoverability

### Standalone / OSS

- `.mvn/settings.xml` + `maven.config` force **Maven Central only**
- CI guards against `app-core` / `com.quinsic` dependencies
- No private monorepo libraries required to build or run

### Identity core

- Accounts: register, me, change password, forgot/reset password
- OTP request/verify + passwordless session login (`/v1/auth/otp/login`)
- OTP channels: `console` (default) and `mail` (SMTP)
- Admin: OAuth clients CRUD, users list/get/enable
- OAuth2/OIDC Authorization Server (JDBC clients + authorizations)
- File-backed RSA JWK (stable across restarts)
- Protected sample API `GET /v1/api/hello`
- qs/uaa-inspired public compat paths (register / OTP / reset)
- `prod` profile disables demo seeds

### Docs

- `docs/AAAX_BOOKLET.md` — single source of truth
- `docs/PARITY_QS_UAA.md` — honest capability matrix vs production UAA

## [0.2.0] — 2026-08-20

- Greenfield `com.aaax` rewrite (removed legacy UAA tree)
- JDBC OAuth + JWK file + OTP log sender + protected API
- Public repo Apache-2.0

## [0.1.0] — 2026-08-20

- Initial greenfield skeleton + accounts register
