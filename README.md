# AAAX

**Accounts · Authentication · Authorization · eXperiences**

Self-host identity that feels good to run and integrate.

| | |
|--|--|
| **Repo** | https://github.com/yky32/aaax |
| **Vision** | [VISION.md](./VISION.md) |
| **Roadmap** | [ROADMAP.md](./ROADMAP.md) |
| **Happy path** | [docs/HAPPY_PATH.md](./docs/HAPPY_PATH.md) |
| **Security** | [SECURITY.md](./SECURITY.md) |
| **License** | [Apache-2.0](./LICENSE) |

## Status

**`0.2.0-SNAPSHOT`** — Accounts, JDBC OAuth clients, stable JWK file, OTP (log sender), protected API sample.

## Quick start

```bash
mvn test
mvn spring-boot:run
```

```bash
# register
curl -sS -X POST http://localhost:8081/v1/accounts/register \
  -H 'content-type: application/json' \
  -d '{"username":"alice","email":"alice@example.com","password":"password123"}'

# client_credentials → API
TOKEN=$(curl -sS -u 'aaax-demo:aaax-demo-secret' \
  -X POST http://localhost:8081/oauth2/token \
  -d 'grant_type=client_credentials&scope=api.read' | jq -r .access_token)
curl -sS http://localhost:8081/v1/api/hello -H "Authorization: Bearer $TOKEN"
```

Full curls → [docs/HAPPY_PATH.md](./docs/HAPPY_PATH.md)

### Demo

| | |
|--|--|
| User | `demo` / `demo` |
| Admin | `admin` / `admin12345` |
| Client | `aaax-demo` / `aaax-demo-secret` |

### Docker

```bash
cp .env.example .env
mvn -DskipTests package
docker compose up --build
```

## Stack

- Java 17 · Spring Boot 3.3 · Spring Authorization Server  
- JPA Accounts · JDBC registered clients + authorizations  
- File-backed RSA JWK · H2 default / Postgres via Compose  

## What’s in

- `POST /v1/accounts/register` · `GET /v1/accounts/me`
- `POST /v1/otp/request` · `POST /v1/otp/verify` (pluggable `OtpSender`)
- `GET /v1/api/hello` (JWT + `SCOPE_api.read`)
- OIDC discovery · `/oauth2/token` · `/oauth2/jwks`
