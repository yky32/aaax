# AAAX

**Accounts · Authentication · Authorization · eXperiences**

Self-host identity that feels good to run and integrate.

| | |
|--|--|
| **Booklet (source of truth)** | [docs/AAAX_BOOKLET.md](./docs/AAAX_BOOKLET.md) |
| **Repo** | https://github.com/yky32/aaax |
| **License** | [Apache-2.0](./LICENSE) |

## Status

**`0.2.0-SNAPSHOT`** — Accounts · JDBC OAuth clients · stable JWK · OTP · protected API.

## Quick start

```bash
mvn test && mvn spring-boot:run
```

```bash
TOKEN=$(curl -sS -u 'aaax-demo:aaax-demo-secret' \
  -X POST http://localhost:8081/oauth2/token \
  -d 'grant_type=client_credentials&scope=api.read' | jq -r .access_token)
curl -sS http://localhost:8081/v1/api/hello -H "Authorization: Bearer $TOKEN"
```

Full product / API / security / roadmap → **[docs/AAAX_BOOKLET.md](./docs/AAAX_BOOKLET.md)**

### Demo (local only)

| | |
|--|--|
| User | `demo` / `demo` |
| Admin | `admin` / `admin12345` |
| Client | `aaax-demo` / `aaax-demo-secret` |

### Docker

```bash
cp .env.example .env && mvn -DskipTests package && docker compose up --build
```
