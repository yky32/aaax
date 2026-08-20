# AAAX roadmap

Greenfield from `0.1.0-SNAPSHOT`. Detail lives here; product north star in [VISION.md](./VISION.md).

## Done

- [x] Public repo + Apache-2.0 + SECURITY
- [x] Brand-new codebase (`com.aaax`) — **no legacy UAA tree**
- [x] Spring Boot 3 + Authorization Server boots
- [x] Demo user/client, ephemeral JWK, health, OIDC discovery
- [x] `mvn test` + GitHub Actions CI
- [x] Docker Compose (Postgres + Redis + app)

## Now → v0.2

- [ ] Persist **Accounts** (User entity + register API)
- [ ] Persist **OAuth clients** (replace in-memory `RegisteredClientRepository`)
- [ ] Password grant-alternative: authorization_code happy path documented (curl)
- [ ] Stable issuer + JWKS across restarts (file or DB-backed keys)

## Next → v0.3

- [ ] OTP path (email provider pluggable)
- [ ] RBAC baseline + one protected resource API sample
- [ ] Redis session / authorization service option
- [ ] `docker compose up` cold path verified on clean machine

## Later

- [ ] Passkeys / richer MFA
- [ ] Social providers pack
- [ ] Admin UX
- [ ] Product GitHub org
- [ ] v1.0 when stranger: clone → compose → register → token → call API

## Non-goals (v1)

- Clerk dashboard clone
- Shipping any Quinsic/tgt business APIs inside this repo
