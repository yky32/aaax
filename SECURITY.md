# Security Policy — AAAX

Report vulnerabilities via **GitHub Security Advisories** on this repo, or contact the maintainer on the GitHub profile.  
Do not open public issues for active exploits.

## Supported versions

| Version | Supported |
|---------|-----------|
| `main` (`0.9.0-SNAPSHOT`) | Yes — best effort |

Spring Boot **3.1.0** is **OSS EOL**. Treat this pin as qs/uaa parity, not as a currently patched Boot line.

## Demo / local keys

The jar does **not** ship a JKS. Unset env generates ephemeral RSA for local clone.  
Production: `AAAX_JWK_KEYSTORE` / `AAAX_ENCRYPTION_KEYSTORE` (and matching password/alias env).

## Details

Posture (demo secrets, JWK file, OTP logging, rotation) →  
**[docs/booklet.md](./docs/booklet.md)** (§8 Security posture)

Edit the booklet for security narrative; keep this file as the GitHub security policy entry point.
