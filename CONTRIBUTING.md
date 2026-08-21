# Contributing to AAAX

Thanks for interest. AAAX is a small Spring Boot OIDC product — keep changes focused.

## Dev setup

- JDK **21+**, Maven **3.9+**
- `mvn test` must pass
- No private Maven deps (`com.quinsic`, `app-core` banned)

## Guidelines

1. Read **[docs/booklet.md](./docs/booklet.md)** (single SoT).
2. HTTP types: `*Endpoint` (not `*Controller`).
3. Writes: `*UseCase` under feature `application` packages.
4. Shared entity timestamps / errors: `com.aaax.core`.
5. Identity events: use `IdentityEvent.Types` + catalog v1.0 (additive types OK; renames need catalog bump).
6. Do not commit secrets.

## PR / push

Solo maintainer may push `main` directly. External contributors: open a PR against `main` with:

- What / why
- Test plan (`mvn test` + curl if API)
- Docs touch `docs/booklet.md` when behavior changes

## Good first issues

- Improve admin Events UI (filter by type)
- Redis-backed QR session store
- Extra social provider behind optional profile
- More curl examples under `examples/curl/`

## Security

Report vulnerabilities via GitHub Security Advisories — do not open public issues for secrets/vulns.
