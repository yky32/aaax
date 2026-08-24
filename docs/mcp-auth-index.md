# MCP Auth Index

**Traffic / discovery page for Model Context Protocol authentication.**  
AAAX is a **self-host OIDC Authorization Server** you can put *behind* MCP resource servers — not an MCP tool host itself.

| | |
|--|--|
| **Product** | [yky32/aaax](https://github.com/yky32/aaax) |
| **Site** | https://aaax-www.vercel.app/ |
| **This index** | https://github.com/yky32/aaax/blob/main/docs/mcp-auth-index.md |
| **Landing mirror** | https://aaax-www.vercel.app/mcp-auth |

---

## Why this page exists

People search: `mcp auth`, `mcp oauth`, `mcp oidc`, `keycloak mcp`, `self-host mcp authorization server`.

MCP remote servers are **OAuth Resource Servers**. Clients discover an **Authorization Server**, run OAuth 2.1 (+ PKCE), then call tools with a Bearer token.

AAAX already ships:

- OIDC discovery + JWKS  
- Auth code + **PKCE** (public SPA client `aaax-spa`)  
- Confidential clients  
- Layer-first Spring Boot 4 / JDK 21  
- Identity Event Bus (login/lifecycle → your Kafka/webhook)

**Gap (honest):** AAAX does **not** yet ship first-class MCP Protected Resource Metadata (RFC 9728) wiring or an MCP gateway. Use AAAX as the **IdP / AS**; put PRM on your MCP HTTP surface (or gateway).

---

## Spec & tutorials (upstream)

| Resource | What |
|----------|------|
| [MCP Authorization (spec)](https://modelcontextprotocol.io/specification/2025-11-25/basic/authorization) | MUST: Protected Resource Metadata, AS discovery |
| [MCP Authorization tutorial](https://modelcontextprotocol.io/docs/2026-07-28/tutorials/security/authorization) | Practical OAuth 2.1 flow |
| [Auth0: MCP auth June 2025](https://auth0.com/blog/mcp-specs-update-all-about-auth/) | MCP as OAuth RS + resource indicators |
| [Aaron Parecki: OAuth for MCP](https://aaronparecki.com/2025/04/03/15/oauth-for-model-context-protocol) | Why AS ≠ MCP server |
| [Descope: MCP auth spec](https://www.descope.com/blog/post/mcp-auth-spec) | PRM + ASM overview |
| [WorkOS: MCP auth providers](https://workos.com/blog/best-mcp-server-authentication-providers) | Vendor landscape (incl. Keycloak) |

### Keywords this index targets

`mcp oauth` · `mcp oidc` · `mcp authorization server` · `mcp protected resource metadata` · `rfc9728 mcp` · `self-host mcp idp` · `spring boot mcp auth` · `oidc for mcp servers`

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

1. Run AAAX (`v0.7.0+`): issuer = public URL of AAAX.  
2. Register an OAuth client (admin UI or seed) — prefer **public + PKCE** for desktop agents when possible; confidential for gateways.  
3. On the **MCP HTTP host**, publish Protected Resource Metadata pointing `authorization_servers` at AAAX issuer.  
4. Validate access tokens against AAAX JWKS (`{issuer}/oauth2/jwks`).  
5. Optional: subscribe to AAAX Identity Event Bus for login/audit side effects (not required for MCP).

See product docs: [booklet §14 OAuth2/OIDC](./booklet.md#14-oauth2--oidc) · [§15 Event Bus](./booklet.md#15-identity-event-bus).

---

## Related self-host / ecosystem notes

| Piece | Role vs AAAX |
|-------|----------------|
| **Keycloak** | Full IdP + realms; heavier. AAAX = lean Spring AS + events. |
| **Auth0 / WorkOS / Descope** | Hosted AS / enterprise IdP. AAAX = you run the jar. |
| **MCP gateways** (e.g. community gateways) | Terminate OAuth once, mint short-lived tool tokens. AAAX can be the upstream IdP. |
| **Keycloak MCP *management* servers** | NL admin of Keycloak — different problem (manage IdP, not *be* AS for tools). |

---

## Clone AAAX

```bash
git clone https://github.com/yky32/aaax.git && cd aaax
git checkout v0.7.0
mvn test && mvn spring-boot:run
# issuer default http://localhost:8081
```

Site: https://aaax-www.vercel.app/ · Issues/PRs welcome on MCP PRM / audience examples.

---

## Changelog of this index

| Date | Note |
|------|------|
| 2026-08-24 | First publish — discovery + AS pattern, honest gap list |

Apache-2.0 · Maintained with AAAX · Not affiliated with Anthropic / MCP org.
