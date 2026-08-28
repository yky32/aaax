# Security Policy — AAAX

Report vulnerabilities via **GitHub Security Advisories** on this repo, or contact the maintainer on the GitHub profile.  
Do not open public issues for active exploits.

## Supported versions

| Version | Supported |
|---------|-----------|
| `main` (`0.9.0-SNAPSHOT`) | Yes — best effort |

`main` tracks Spring Boot **4.1.1**. Production still needs a file JWK (`AAAX_JWK_KEYSTORE`); do not treat ephemeral local keys as a production baseline.

## Keys

The jar does **not** ship a JKS. Unset env generates ephemeral RSA for **local clone only** (tokens invalid after restart).

**Production must set** `AAAX_JWK_KEYSTORE` / `AAAX_ENCRYPTION_KEYSTORE` (and matching password/alias env). There is no HTTP API to export the private key.

## Details

Posture (demo secrets, JWK file, OTP logging, rotation) →  
**[docs/booklet.md](./docs/booklet.md)** (§8 Security posture)

Edit the booklet for security narrative; keep this file as the GitHub security policy entry point.
