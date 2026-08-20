# AAAX roadmap (post-v0.5)

Honest backlog — not a commitment schedule.

## Shipped

### v0.5
- OIDC AS · accounts · OTP · magic link · TOTP · sessions
- Identity Event Bus · SMS dual-mode · SAML SP · Google/GitHub
- UseCase layer · Redis-optional OTP store · resource-server example
- Passkeys (webauthn4j) **opt-in**

### v0.6-SNAPSHOT (in progress)
- **QR code login** — [QR_LOGIN.md](./QR_LOGIN.md)
- **Trusted devices** — remember browser / skip TOTP — [TRUSTED_DEVICES.md](./TRUSTED_DEVICES.md)

## Not built (needs product `go`)

| Item | Notes |
|------|--------|
| **Strict device allow-list only** | Current trust is remember/skip-MFA, not “block unknown devices” |
| Multi-tenant **orgs** | Single-realm only today. |
| **SAML IdP** | SP only. |
| Official **React / Next SDK** | Hosted pages + JWT RS example only. |
| Apple / Microsoft social | Google + GitHub first. |
| QR multi-node store | QR sessions still in-memory (like default OTP). |

## Explicit non-goals (near term)

- Keycloak kitchen-sink federation dump
- Clerk SaaS clone / seat billing
- Private Quinsic mesh libs

When Wayne says `go trusted-device`, that is the next device-policy lane.
