# AAAX

**A**ccounts · **A**uthentication · **A**uthorization · e**X**periences

Self-host OpenID Connect as **one Spring jar**. Read the code. Own the keys.

> Identity you run. Signals you own.

For Spring/JVM teams. **Not** Keycloak, Authentik, or Clerk.

| | |
|--|--|
| Site | [aaax-www.vercel.app](https://aaax-www.vercel.app/) |
| Run | `com.aaax.server.App` · **:8081** |
| Stack | Boot **4.1.1** · Java **21** · Postgres · Redis |
| License | Apache-2.0 |
| Portal | [yky32/aaax-portal](https://github.com/yky32/aaax-portal) (separate UI, not in this jar) |
| Docs | [booklet](docs/booklet.md) · [index](docs/README.md) |

```text
com.aaax.core      foundation — BizException, R/Result, audit
com.aaax.server    AS — users, OTP, OIDC, devices, RBAC
```

---

## Run

**IntelliJ:** Docker for Postgres + Redis, JDK 21 on the host.

```bash
git clone https://github.com/yky32/aaax.git && cd aaax
docker compose up -d
cp .env.example .env && set -a && source .env && set +a
# IntelliJ → com.aaax.server.App
```

**Jar in Docker** (local seed, ephemeral RSA — not production):

```bash
docker compose --profile stack up --build
# http://localhost:8081
```

**Portal too** (clone sibling first):

```bash
git clone https://github.com/yky32/aaax-portal.git ../aaax-portal
docker compose --profile stack -f docker-compose.yml -f compose.portal.yml up --build
# http://127.0.0.1:5173
```

Host build:

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home
mvn -Dmaven.test.skip=true package
java -jar target/aaax-0.9.1.jar
./scripts/quickstart-smoke.sh
./scripts/token-smoke.sh
```

`-DskipTests` still **compiles** tests. Use `-Dmaven.test.skip=true`. Avoid JDK 26.

Seed (`AAAX_LOCAL_SEED=true`, **not** production):

| | |
|--|--|
| Client | `client` / `secret` |
| PKCE | `aaax-pkce` · `aaax-portal` |
| User | `smoke.primary@aaax.local` / `SmokePrimary!1` |

---

## Use as an authorization server

Point a resource server at this issuer. Validate JWT against JWKS. `/users/me` is still the `R` envelope — not OIDC UserInfo.

| | |
|--|--|
| Issuer | `http://localhost:8081` (`AS_ISSUER`) |
| RFC 8414 | `GET /.well-known/oauth-authorization-server` |
| OIDC | `GET /.well-known/openid-configuration` |
| JWKS | `GET /oauth2/jwks` |
| Token | `POST /oauth2/token` → RFC `access_token` |
| Login | `GET /oauth2/authorize` → hosted `/login` |

Password grant field is **`credentials`**, not `password`:

```bash
curl -sS -u client:secret -X POST http://localhost:8081/oauth2/token \
  -H 'content-type: application/x-www-form-urlencoded' \
  -d 'grant_type=custom-password-grant' \
  -d 'username=smoke.primary@aaax.local' \
  -d 'credentials=SmokePrimary!1'
```

Public PKCE: client `aaax-pkce` (`none`, proof key required **before** login). Script: `./scripts/hosted-authorize-smoke.sh`.

More HTTP: [`examples/curl/`](examples/curl/).

---

## Production

This compose/seed path is **local**.

- Set `AAAX_JWK_KEYSTORE` + `AAAX_ENCRYPTION_KEYSTORE` (path, password, alias). Unset = ephemeral RSA; tokens die on restart.
- `AAAX_LOCAL_SEED=false`
- Gate `/swagger-ui`, `/v3/api-docs`, `/actuator` — they are permitAll today. Booklet §8.1.
- No in-jar `/admin`. Operator UI is [aaax-portal](https://github.com/yky32/aaax-portal).

Secrets: env only (`.env.example`). Never commit tokens.

---

## Docs

- [docs/README.md](docs/README.md) — index
- [docs/booklet.md](docs/booklet.md) — product + eng SoT (code wins if it drifts)
- [AGENTS.md](AGENTS.md) — for coding agents
- [SECURITY.md](SECURITY.md)
