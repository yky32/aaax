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

**Brand-new greenfield · `0.1.0-SNAPSHOT`**

No legacy UAA dump. Clean Spring Boot 3 Authorization Server skeleton.

## Quick start

```bash
# requires JDK 17+
mvn test
mvn spring-boot:run
```

Open:
- Product meta → http://localhost:8081/
- Health → http://localhost:8081/actuator/health
- OIDC discovery → http://localhost:8081/.well-known/openid-configuration

### Demo login (dev only)

| | |
|--|--|
| User | `demo` / `demo` |
| Client | `aaax-demo` / `aaax-demo-secret` |
| Grants | authorization_code, refresh_token, client_credentials |
| Redirect | `http://localhost:3000/login/oauth2/code/aaax` |

### Docker (Postgres + Redis + app)

```bash
cp .env.example .env
mvn -DskipTests package
docker compose up --build
```

## Stack

- Java 17 · Spring Boot 3.3 · Spring Authorization Server  
- JPA · PostgreSQL (prod path) · H2 (default local/tests)  
- RSA JWK generated in-process (dev)

## What comes next

See [ROADMAP.md](./ROADMAP.md) — Accounts store, real clients DB, OTP path, DX quickstarts.
