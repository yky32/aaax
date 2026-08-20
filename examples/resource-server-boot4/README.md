# Resource server example (Spring Boot 4.1 / JDK 21)

Minimal **external** API that trusts JWT from AAAX.

```text
AAAX :8081  →  issues access_token (scope api.read)
this  :8082  →  validates JWT via issuer-uri / JWKS
```

## Prerequisites

1. AAAX running (`mvn spring-boot:run` in repo root), port **8081**
2. JDK 21 + Maven 3.9+

## Run

```bash
# terminal 1 — AAAX
cd /path/to/aaax && mvn spring-boot:run

# terminal 2 — this app (use ./mvn.sh so parent .mvn/settings resolves)
cd examples/resource-server-boot4
chmod +x mvn.sh call.sh
./mvn.sh spring-boot:run
```

Or:

```bash
mvn -s ../../.mvn/settings.xml spring-boot:run
```

Optional:

```bash
export AAAX_ISSUER=http://localhost:8081
export PORT=8082
```

## Call it

```bash
# token from AAAX demo client
TOKEN=$(curl -sS -u 'aaax-demo:aaax-demo-secret' \
  -X POST http://localhost:8081/oauth2/token \
  -d 'grant_type=client_credentials&scope=api.read' | jq -r .access_token)

curl -sS http://localhost:8082/api/hello \
  -H "Authorization: Bearer $TOKEN" | jq .
```

Or use the helper script from **repo root**:

```bash
./examples/resource-server-boot4/call.sh
```

Expected JSON includes `"message":"hello from external resource server"` and `scope` containing `api.read`.

## Copy into your service

| Piece | |
|-------|--|
| Dependency | `spring-boot-starter-oauth2-resource-server` |
| Config | `spring.security.oauth2.resourceserver.jwt.issuer-uri` = your `aaax.issuer` |
| Rule | `.hasAuthority("SCOPE_api.read")` for scope `api.read` |

See also [../resource-call.md](../resource-call.md).

## Notes

- Demo client scopes include `api.read` (seeded as `aaax-demo`).
- Issuer must be reachable from this process (JWKS fetch). Docker: use host.docker.internal or shared network URL.
- This module is **not** published to Maven Central — example only.
