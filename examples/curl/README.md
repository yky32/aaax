# curl recipes

Assume `AAAX_BASE=http://localhost:8081`. User/admin APIs use the `R` envelope (`code` + `data`). **`POST /oauth2/token` is RFC 6749 JSON** (`access_token`). HTTP is **200** on success unless noted.

There is **no** `/v1/accounts` and **no** events catalog (`/v1/admin/events`) on this tree.

| Recipe | Path | Expect |
|--------|------|--------|
| Register start | `POST /users/registrations` | **200** `SYS0000` + OTP metadata; occupied **409** `AAAX0409` |
| Register verify | `POST /users/verifications` | **200** |
| Create user | `POST /users` | **200** |
| General OTP | `POST /authentications/one-time-passwords/general` | **200** |
| RFC 8414 metadata | `GET /.well-known/oauth-authorization-server` | **200** JSON with `issuer` |
| OIDC discovery | `GET /.well-known/openid-configuration` | **200** JSON with `issuer` |
| Login | `POST /oauth2/token` `custom-password-grant` | **200**, `access_token` |
| Refresh | `POST /oauth2/token` `grant_type=refresh_token` | **200**, new `access_token` |
| Hosted login | `GET /login` | HTML form |
| PKCE (seed `aaax-pkce`) | after `/login`, `GET /oauth2/authorize` without `code_challenge` | error mentioning `code_challenge` |
| Hosted PKCE + loopback token | `./scripts/hosted-authorize-smoke.sh` | other-port loopback code + `access_token` (public client) |
| Me | `GET /users/me` Bearer | **200** |
| Events catalog | — | **not shipped** |

Scripts (running AS on 8081; login/me need `AAAX_LOCAL_SEED=true`, or your own client/user):

```bash
./examples/curl/register.sh user@example.com 'Password1!'
./examples/curl/otp-general.sh user@example.com
./examples/curl/login-me.sh
./examples/curl/refresh-token.sh
# PKCE end-to-end (needs AAAX_LOCAL_SEED=true):
./scripts/hosted-authorize-smoke.sh
```

## Manual

### Register (OTP hold)

```bash
BASE="${AAAX_BASE:-http://localhost:8081}"
curl -sS -w "\nHTTP %{http_code}\n" -X POST "$BASE/users/registrations" \
  -H 'content-type: application/json' \
  -d '{"username":"user@example.com","credentials":"Password1!"}'
# occupied → HTTP 409, code AAAX0409
# OTP payload is logged locally; then:
curl -sS -w "\nHTTP %{http_code}\n" -X POST "$BASE/users/verifications" \
  -H 'content-type: application/json' \
  -d '{"username":"user@example.com","code":"123456"}'
curl -sS -w "\nHTTP %{http_code}\n" -X POST "$BASE/users" \
  -H 'content-type: application/json' \
  -d '{"username":"user@example.com","credentials":"Password1!"}'
```

### General OTP

```bash
curl -sS -w "\nHTTP %{http_code}\n" -X POST "${AAAX_BASE:-http://localhost:8081}/authentications/one-time-passwords/general" \
  -H 'content-type: application/json' \
  -d '{"to":"user@example.com","usecase":"OTP_GENERAL","type":"DIGIT"}'
```

### Login + me

```bash
# local seed (`AAAX_LOCAL_SEED=true`) — grant_type=custom-password-grant, field credentials=
./examples/curl/login-me.sh
# or scripts/token-smoke.sh then:
curl -sS -w "\nHTTP %{http_code}\n" \
  -H "Authorization: Bearer $TOKEN" \
  "${AAAX_BASE:-http://localhost:8081}/users/me"
```

### Refresh token

After password grant, exchange `refresh_token` for a new access token (confidential client `client`/`secret` from seed):

```bash
./examples/curl/refresh-token.sh
# or manually:
curl -sS -w "\nHTTP %{http_code}\n" \
  -u 'client:secret' \
  -X POST "${AAAX_BASE:-http://localhost:8081}/oauth2/token" \
  -H 'content-type: application/x-www-form-urlencoded' \
  -d 'grant_type=refresh_token' \
  --data-urlencode "refresh_token=${REFRESH_TOKEN}"
```

### PKCE authorize + token (pointers)

Seed public client **`aaax-pkce`** requires PKCE on `/oauth2/authorize`. AAAX validates `code_challenge` **before** login.

1. Build authorize URL with `code_challenge` + `code_challenge_method=S256` (RFC 7636).
2. Unauthenticated → redirect to **`/login`**; authenticate with form POST (`username`, `password`, `_csrf`).
3. Re-hit authorize → redirect to `redirect_uri?code=…`.
4. **`POST /oauth2/token`** with `grant_type=authorization_code`, `code`, `redirect_uri`, `client_id`, `code_verifier` (no secret for public client).

Full automated smoke (hosted login + loopback on another port): **`./scripts/hosted-authorize-smoke.sh`**.  
PKCE rejection checks only: **`./scripts/pkce-smoke.sh`**.

RFC 7636 appendix test vectors (used in CI):

```text
code_verifier  = dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk
code_challenge = E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM  (S256)
```
