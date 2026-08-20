# AAAX

**Accounts · Authentication · Authorization · eXperiences**

| | |
|--|--|
| **Booklet** | [docs/AAAX_BOOKLET.md](./docs/AAAX_BOOKLET.md) |
| **Repo** | https://github.com/yky32/aaax |
| **Version** | `0.3.0-SNAPSHOT` |

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

**New in 0.3:** mail OTP channel · passwordless OTP login · admin clients API · prod profile · deploy checklist.

Full docs → **[docs/AAAX_BOOKLET.md](./docs/AAAX_BOOKLET.md)**

### Demo (local seeds)

| | |
|--|--|
| User | `demo` / `demo` |
| Admin | `admin` / `admin12345` |
| Client | `aaax-demo` / `aaax-demo-secret` |
