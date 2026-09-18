# AGENTS.md

Instructions for AI coding agents working in **yky32/aaax**.

## What this repo is

Self-host **OpenID Connect authorization server** — one Spring Boot jar (`com.aaax.server.App`).  
**Not** Clerk, Logto, or Keycloak. ICP: Spring/JVM teams, self-host, full control.

## Read first

| Priority | File | Why |
|----------|------|-----|
| 1 | [docs/booklet.md §1–§2](docs/booklet.md#1-what-this-is) | Scope + honest shipped status |
| 2 | [CONTRIBUTING.md](CONTRIBUTING.md) | Layering and hard rules |
| 3 | [docs/booklet.md §3–§8](docs/booklet.md#3-layout) | HTTP, grants, config, security (when changing code) |
| 4 | [README.md](README.md) | Local run + OAuth AS usage |

Full doc index: [docs/README.md](docs/README.md).

## Hard rules (summary)

- Layering: `Endpoint` → `UseCase` → `Repository` → `Entity` — no parallel trees
- HTTP: `*Endpoint` under `endpoint/<domain>/`, not `*Controller`
- Writes in `usecase` only — no new business `@Service`
- Behavior change → update `docs/booklet.md` + `CHANGELOG.md`
- Do not claim or build features listed ❌ in [booklet §2](docs/booklet.md#2-honest-status-09) unless explicitly requested

## Verify changes

```bash
mvn test
# OAuth / HTTP surface:
./scripts/quickstart-smoke.sh
```

## Out of scope (default)

Passkeys, SAML, orgs, admin UI, `/v1/accounts`, Event Bus HTTP catalog, MCP Protected Resource Metadata on this jar — see [booklet §10](docs/booklet.md#10-out-of-scope-until-asked).
