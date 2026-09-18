# Documentation

**Three files.** Everything else was removed — no stub maze.

| Read this | When |
|-----------|------|
| **[booklet.md](./booklet.md)** | Product + engineering source of truth (status, HTTP, grants, config, security). **Code wins if this drifts.** |
| **[mcp-auth-index.md](./mcp-auth-index.md)** | Wiring AAAX as an OIDC Authorization Server behind MCP resource servers |
| **[../README.md](../README.md)** | Five-minute local clone, smoke scripts, OAuth AS quick reference |
| **[../CONTRIBUTING.md](../CONTRIBUTING.md)** | Layout rules, PR bar, Maven Central maintainers |

---

## For humans

1. **Try it:** [README § Five minutes](../README.md#five-minutes-local)
2. **What ships in 0.9:** [booklet §2](./booklet.md#2-honest-status-09)
3. **What we will not build unless asked:** [booklet §10](./booklet.md#10-out-of-scope-until-asked)
4. **HTTP / curl examples:** [examples/curl/](../examples/curl/)

---

## For AI agents

**Read order (stop when you have enough context):**

1. [booklet §1](./booklet.md#1-what-this-is) — what AAAX is and is not (not Clerk, not Keycloak)
2. [booklet §2](./booklet.md#2-honest-status-09) — honest feature matrix before assuming capabilities
3. [CONTRIBUTING § Hard rules](../CONTRIBUTING.md#hard-rules) — if editing code
4. [booklet §3–§5](./booklet.md#3-layout) — layout, HTTP, grants (implementation work)
5. [booklet §8](./booklet.md#8-security-posture) — security / CSRF / public routes

**Do not** invent features absent from booklet §2 (no `/v1/accounts`, no admin UI, no Event Bus catalog, no MCP PRM on this jar).

**Repo layout:**

```text
docs/
├── README.md           ← you are here
├── booklet.md          ← single SoT (§1–§10)
└── mcp-auth-index.md   ← MCP OAuth discovery page only
```
