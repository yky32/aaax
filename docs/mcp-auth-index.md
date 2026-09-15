# MCP Auth Index

**Discovery page for people wiring Model Context Protocol OAuth.**  
AAAX is a **self-host OIDC Authorization Server** you can put *behind* MCP resource servers — not an MCP tool host, not an MCP gateway, and **not** RFC 9728 Protected Resource Metadata on this jar.

| | |
|--|--|
| **Product** | [yky32/aaax](https://github.com/yky32/aaax) |
| **Site** | https://aaax-www.vercel.app/ |
| **This index** | https://github.com/yky32/aaax/blob/main/docs/mcp-auth-index.md |
| **Eng SoT** | [booklet.md](./booklet.md) (code wins if it drifts) |

---

## Why this page exists

People search: `mcp auth`, `mcp oauth`, `mcp oidc`, `self-host mcp authorization server`.

MCP remote servers are **OAuth Resource Servers**. Clients discover an **Authorization Server**, run OAuth 2.1 (+ PKCE), then call tools with a Bearer token.

**What AAAX ships today** (see [booklet §2](./booklet.md#2-honest-status-09)):

- OIDC discovery + JWKS  
- Auth code + **PKCE** (local seed public client `aaax-pkce`; not production-ready as-is)  
- Confidential clients + `custom-password-grant` for API login  
- Hosted `/login` for browser authorize  
- Spring Boot **4.1.1** / Java **21**

**Not shipped:** MCP Protected Resource Metadata (RFC 9728) on AAAX, an MCP gateway, an events catalog HTTP API, or Kafka as a required dependency (`AAAX_KAFKA_ENABLED` defaults **off**).

---

## Spec & tutorials (upstream)

| Resource | What |
|----------|------|
| [MCP Authorization (spec)](https://modelcontextprotocol.io/specification/2025-11-25/basic/authorization) | MUST: Protected Resource Metadata, AS discovery |
| [MCP Authorization tutorial](https://modelcontextprotocol.io/docs/2026-07-28/tutorials/security/authorization) | Practical OAuth 2.1 flow |
| [Auth0: MCP auth June 2025](https://auth0.com/blog/mcp-specs-update-all-about-auth/) | MCP as OAuth RS + resource indicators |
| [Aaron Parecki: OAuth for MCP](https://aaronparecki.com/2025/04/03/15/oauth-for-model-context-protocol) | Why AS ≠ MCP server |

---

## Pattern: AAAX as AS for MCP

```text
MCP Client (Cursor / Claude / custom)
    │  1) GET MCP URL → 401 + resource_metadata
    │  2) PRM → authorization_servers: [ https://aaax.example ]
    ▼
AAAX (OIDC AS)  ←── PKCE / client credentials
    │  3) token (aud/resource per your policy)
    ▼
MCP Resource Server / gateway  ←── Bearer JWT validate via AAAX JWKS
    │
    ▼
tools / resources
```

### Minimal wiring checklist

1. Run AAAX on `main` or tag `v0.9.0`: issuer = public URL of AAAX (`AS_ISSUER`).  
2. Register an OAuth client (JDBC / seed for local only) — **public + PKCE** for desktop agents when possible; confidential for gateways.  
3. On the **MCP HTTP host**, publish Protected Resource Metadata pointing `authorization_servers` at AAAX issuer (AAAX does not publish PRM for your MCP surface).  
4. Validate access tokens against AAAX JWKS (`{issuer}/oauth2/jwks`).

Product docs: [booklet §4 HTTP](./booklet.md#4-http) · [§5 Grants](./booklet.md#5-grants) · [README](../README.md) five-minute local.

---

## Related self-host / ecosystem notes

| Piece | Role vs AAAX |
|-------|----------------|
| **Keycloak** | Full IdP + realms; heavier. AAAX = lean Spring AS jar. |
| **Auth0 / WorkOS / Descope** | Hosted AS / enterprise IdP. AAAX = you run the jar. |
| **MCP gateways** | Terminate OAuth once, mint short-lived tool tokens. AAAX can be the upstream IdP. |

---

## Clone AAAX (local)

```bash
git clone https://github.com/yky32/aaax.git && cd aaax
docker compose up -d
cp .env.example .env && set -a && source .env && set +a
mvn -Dmaven.test.skip=true package
java -jar target/aaax-0.9.1-SNAPSHOT.jar
# issuer default http://localhost:8081
./scripts/quickstart-smoke.sh
```

Site: https://aaax-www.vercel.app/ · Issues/PRs welcome on honest docs and MCP wiring examples (PRM on *your* MCP host).

---

## Changelog of this index

| Date | Note |
|------|------|
| 2026-09-10 | Align with booklet §2 — drop Event Bus, aaax-spa, v0.7, fake booklet §14/§15 |
| 2026-08-24 | First publish — discovery + AS pattern, honest gap list |

Apache-2.0 · Maintained with AAAX · Not affiliated with Anthropic / MCP org.
