# Overnight log

## 2026-08-21 — Batch B (`go` = overnight deep)

### Done
- **B1–B3** `OtpCodeStore` + `MagicLinkTokenStore` SPI; memory default
- **B3** Redis when `aaax.otp.store=redis` (`RedisTokenStoreConfig`; Boot Redis auto excluded)
- **B4** `examples/redis-otp-store.md` + `examples/compose-redis-otp/`
- **E** `mvn clean test` **23/23**

### Verify
- Default path: no Redis connection required
- HTTP API unchanged

### Commits
- (see tip after push)

### Morning next
- `go overnight dx` · `go passkey crypto`

## 2026-08-20 — Batch A + E (`go overnight`)

### Done
- **A1** Social + SAML → `FinishAuthenticatedSession`
- **A2** Magic link folded into `MagicLinkUseCase`
- **A3–A4** README mermaid + CODEMAP
- **E** tests + verify-standalone

### Commits
- `e5d0de8` / `365669a`
