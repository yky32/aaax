# AAAX roadmap (post-v0.5)

Honest backlog — not a commitment schedule.

## Shipped (v0.5)

- OIDC AS · accounts · OTP · magic link · TOTP · sessions
- Identity Event Bus · SMS dual-mode · SAML SP · Google/GitHub
- UseCase layer · Redis-optional OTP store · resource-server example
- Passkeys (webauthn4j) **opt-in**

## Not built (needs product `go`)

| Item | Notes |
|------|--------|
| **QR code login** | Phone approves desktop session (poll or WS). qs/uaa had grants/WS; AAAX does not. |
| **Device binding (policy)** | Trusted device registry, MFA skip, force re-auth. Distinct from passkeys. |
| Multi-tenant **orgs** | Single-realm only today. |
| **SAML IdP** | SP only. |
| Official **React / Next SDK** | Hosted pages + JWT RS example only. |
| Apple / Microsoft social | Google + GitHub first. |

## Near-term engineering (safe / no new ICP)

- More MockMvc coverage (passkeys-off 404, social flags)
- Hardening: rate-limit SPI, session↔Spring tighter bind
- Booklet/CODEMAP drift watch

## Explicit non-goals (near term)

- Keycloak kitchen-sink federation dump
- Clerk SaaS clone / seat billing
- Private Quinsic mesh libs

When Wayne says `go qr-login` or `go trusted-device`, that is a 0.6 feature lane.
