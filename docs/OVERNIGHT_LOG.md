# Overnight log

## STOP — nothing further without product decisions

Autonomous overnight queue **A → D exhausted**. Remaining needs Wayne:

| Need you | Why |
|----------|-----|
| `go passkey crypto` | webauthn4j / full verify |
| `go v0.5` | release tag + freeze |
| Orgs / SAML IdP / React SDK | product gates |

### Commits (tip `9955f48`)
| Batch | Tip | |
|-------|-----|--|
| A | `e5d0de8` | FinishSession unify |
| B | `d7e070e` | Redis OTP store |
| C | `25b23c1` / `54fce92` | Resource server example |
| D | `9955f48` | Passkeys off by default |

## 2026-08-21 — Batch D

- `aaax.passkeys.enabled=false` default
- API 404 when disabled; `/user` hides UI
- Meta/settings + docs honesty

## 2026-08-21 — Batch C

- `examples/resource-server-boot4/` E2E

## 2026-08-21 — Batch B

- `OtpCodeStore` / `MagicLinkTokenStore` memory|redis

## 2026-08-20 — Batch A

- FinishAuthenticatedSession + magic merge + mermaid
