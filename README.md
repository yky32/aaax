# AAAX

**Accounts · Authentication · Authorization · eXperiences**

Self-host identity that feels good to run and integrate.

| | |
|--|--|
| **Repo** | https://github.com/yky32/aaax |
| **Vision** | [VISION.md](./VISION.md) |
| **Roadmap** | [ROADMAP.md](./ROADMAP.md) |
| **Security** | [SECURITY.md](./SECURITY.md) |
| **License** | [Apache-2.0](./LICENSE) |

## Status

**Greenfield · `0.1.0-SNAPSHOT`** — Accounts store + register API live.

## Quick start

```bash
# JDK 17+
mvn test
mvn spring-boot:run
```

| URL | |
|-----|--|
| Meta | http://localhost:8081/ |
| Health | http://localhost:8081/actuator/health |
| OIDC | http://localhost:8081/.well-known/openid-configuration |

### Register an account

```bash
curl -sS -X POST http://localhost:8081/v1/accounts/register \
  -H 'content-type: application/json' \
  -d '{"username":"alice","email":"alice@example.com","password":"password123"}'
```

### Who am I (session)

After browser form login at `/login`:

```bash
curl -sS http://localhost:8081/v1/accounts/me --cookie '...'
```

### Demo login (dev seed)

| | |
|--|--|
| User | `demo` / `demo` (seeded if DB empty) |
| OAuth client | `aaax-demo` / `aaax-demo-secret` |
| Redirect | `http://localhost:3000/login/oauth2/code/aaax` |

### Docker

```bash
cp .env.example .env
mvn -DskipTests package
docker compose up --build
```

## Stack

- Java 17 · Spring Boot 3.3 · Spring Authorization Server · JPA  
- H2 default · Postgres via Compose/env  
- Ephemeral RSA JWK (dev)

## Next

See [ROADMAP.md](./ROADMAP.md).
