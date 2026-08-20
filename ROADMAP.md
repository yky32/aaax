# AAAX roadmap

Greenfield from `0.1.0-SNAPSHOT`. North star: [VISION.md](./VISION.md).

## Done

- [x] Public repo + Apache-2.0 + SECURITY
- [x] Brand-new `com.aaax` codebase
- [x] Spring Authorization Server boots
- [x] Demo OAuth client + ephemeral JWK + OIDC discovery
- [x] `mvn test` + GitHub Actions CI
- [x] Docker Compose (Postgres + Redis + app)
- [x] **Accounts** table + `POST /v1/accounts/register`
- [x] DB-backed form login (`AccountUserDetailsService`)
- [x] `GET /v1/accounts/me`

## Now → v0.2 continued

- [ ] Persist **OAuth clients** (replace in-memory repository)
- [ ] authorization_code happy path documented (curl end-to-end)
- [ ] Stable issuer JWKS across restarts (file or DB-backed keys)

## Next → v0.3

- [ ] OTP path (email provider pluggable)
- [ ] RBAC baseline + protected resource API sample
- [ ] Redis-backed authorization service option
- [ ] Cold `docker compose up` verified on clean machine

## Later

- [ ] Passkeys / richer MFA
- [ ] Social providers pack
- [ ] Admin UX
- [ ] Product GitHub org
- [ ] v1.0 when stranger: clone → compose → register → token → call API

## Non-goals (v1)

- Clerk dashboard clone
- Shipping Quinsic/tgt business APIs in this repo
