# Security Policy — AAAX

Report vulnerabilities via GitHub Security Advisories on this repo, or contact the maintainer on the GitHub profile. Do not file public issues for active exploits.

## Supported versions

| Version | Supported |
|---------|-----------|
| `main` (`0.1.x-SNAPSHOT`) | Yes — best effort |

## Current posture (greenfield)

- **Dev demo credentials** (`demo`/`demo`, `aaax-demo`/`aaax-demo-secret`) are for local only. Change before any shared environment.
- **RSA signing keys** are generated in-process and **lost on restart** — not for production.
- Default local DB is **H2 in-memory**; use Postgres via env/Compose for real data.
- No secrets belong in git. Use `.env` (see `.env.example`).

## Secrets

If you find a leaked credential in the repo or a PR, report it and rotate immediately.
