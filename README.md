# AAAX

**Accounts · Authentication · Authorization · eXperiences**

| | |
|--|--|
| **Booklet** | [docs/AAAX_BOOKLET.md](./docs/AAAX_BOOKLET.md) |
| **qs/uaa parity** | [docs/PARITY_QS_UAA.md](./docs/PARITY_QS_UAA.md) |
| **Repo** | https://github.com/yky32/aaax |
| **Version** | `0.3.0-SNAPSHOT` |

## Clone → run (standalone, Maven Central only)

```bash
git clone https://github.com/yky32/aaax.git
cd aaax
mvn test                 # uses .mvn/settings.xml → Central ONLY (no private packages)
mvn spring-boot:run
```

No `app-core`. No GitHub Packages. No Quinsic credentials required.

## Quick API check

```bash
curl -sS http://localhost:8081/actuator/health
TOKEN=$(curl -sS -u 'aaax-demo:aaax-demo-secret' \
  -X POST http://localhost:8081/oauth2/token \
  -d 'grant_type=client_credentials&scope=api.read' | jq -r .access_token)
curl -sS http://localhost:8081/v1/api/hello -H "Authorization: Bearer $TOKEN"
```

Demo: `demo`/`demo1234` · admin `admin`/`admin12345` · client `aaax-demo`/`aaax-demo-secret`

Docs → **[docs/AAAX_BOOKLET.md](./docs/AAAX_BOOKLET.md)** · Parity → **[docs/PARITY_QS_UAA.md](./docs/PARITY_QS_UAA.md)**
