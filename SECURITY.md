# Security Policy — AAAX

Report vulnerabilities via GitHub Security Advisories on this repo, or contact the maintainer on the GitHub profile. Do not file public issues for active exploits.

## Supported versions

| Version | Supported |
|---------|-----------|
| `main` (`0.2.x-SNAPSHOT`) | Yes — best effort |

## Current posture

- Demo accounts/clients (`demo`, `admin`, `aaax-demo`) are **local only**.
- JWK private key lives on disk at `aaax.jwk.path` — protect that file in real deploys; generate fresh material per environment.
- Default OTP sender **logs codes** — replace `OtpSender` before any shared environment.
- Secrets via environment only (`.env.example`). Never commit `.env` or prod keystores.

## Secrets

If you find a leaked credential in the repo or a PR, report it and rotate immediately.
