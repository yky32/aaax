# AAAX roadmap

Greenfield. North star: [VISION.md](./VISION.md).

## Done (0.2)

- [x] Public repo + Apache-2.0 + SECURITY
- [x] Brand-new `com.aaax` codebase
- [x] Spring Authorization Server
- [x] Accounts + register + DB login + roles
- [x] JDBC **OAuth clients** + authorization / consent stores
- [x] **Stable JWK file** (`aaax.jwk.path`)
- [x] Protected API sample (`GET /v1/api/hello`)
- [x] OTP path + pluggable `OtpSender` (default logs)
- [x] authorization_code / client_credentials curls → [docs/HAPPY_PATH.md](./docs/HAPPY_PATH.md)
- [x] CI + Compose

## Next → v0.3+

- [ ] Email/SMS `OtpSender` implementations
- [ ] Redis OTP / authorization service option (prod multi-node)
- [ ] First-class passwordless OTP login grant
- [ ] Admin client management API
- [ ] Passkeys / social packs
- [ ] v1.0 stranger cold path polish

## Non-goals (v1)

- Clerk dashboard clone
- Quinsic/tgt business APIs in this repo
