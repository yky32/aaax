# curl recipes

Assume `AAAX_BASE=http://localhost:8081`. User/admin APIs use the `R` envelope (`code` + `data`). **`POST /oauth2/token` is RFC 6749 JSON** (`access_token`). HTTP is **200** on success unless noted.

There is **no** `/v1/accounts` and **no** events catalog (`/v1/admin/events`) on this tree.

| Recipe | Path | Expect |
|--------|------|--------|
| Register start | `POST /users/registrations` | **200** `SYS0000` + OTP metadata; occupied **409** `UAA0409` |
| Register verify | `POST /users/verifications` | **200** |
| Create user | `POST /users` | **200** |
| General OTP | `POST /authentications/one-time-passwords/general` | **200** |
| Login | `POST /oauth2/token` `custom-password-grant` | **200**, `access_token` |
| Me | `GET /users/me` Bearer | **200** |
| Events catalog | — | **not shipped** |

Scripts (running AS on 8081; login/me need `AAAX_LOCAL_SEED=true`, or your own client/user):

```bash
./examples/curl/register.sh user@example.com 'Password1!'
./examples/curl/otp-general.sh user@example.com
./examples/curl/login-me.sh
```

## Manual

### Register (OTP hold)

```bash
BASE="${AAAX_BASE:-http://localhost:8081}"
curl -sS -w "\nHTTP %{http_code}\n" -X POST "$BASE/users/registrations" \
  -H 'content-type: application/json' \
  -d '{"username":"user@example.com","credentials":"Password1!"}'
# occupied → HTTP 409, code UAA0409
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
