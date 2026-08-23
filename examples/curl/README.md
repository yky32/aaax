# curl recipes

Local AAAX is assumed on `http://localhost:8081` (override with `AAAX_BASE`).
Demo users: `admin` / `admin12345`, `demo` / `demo1234`. Confidential client: `aaax-demo` / `aaax-demo-secret`.

| Script | Flow | Expected HTTP |
|--------|------|----------------|
| `register-user.sh` | `POST /v1/accounts/register` | **201 Created** (conflict **409** if taken) |
| `otp-login-session.sh` | OTP request + login + `/me` | **200** / **200** / **200** |
| `login-admin-and-events.sh` | admin login + events | **200** / **200** |
| `get-token-and-hello.sh` | client_credentials + hello | **200** / **200** |
| `register-login-me.sh` | register then password login then `/me` | **201** / **200** / **200** |
| `events-catalog.sh` | admin session events buffer | **200** |

## Manual recipes

### Register (expect 201)

```bash
curl -sS -o /tmp/aaax-reg.json -w "HTTP %{http_code}\n" \
  -X POST "${AAAX_BASE:-http://localhost:8081}/v1/accounts/register" \
  -H 'content-type: application/json' \
  -d '{"username":"devuser","email":"devuser@example.com","password":"password123"}'
```

### OTP request + login (expect 200)

```bash
BASE="${AAAX_BASE:-http://localhost:8081}"
CJ=/tmp/aaax-otp.cj
curl -sS -c "$CJ" -w "HTTP %{http_code}\n" -X POST "$BASE/v1/otp/request" \
  -H 'content-type: application/json' -d '{"username":"demo"}'
# read OTP from server logs when channel=console, then:
CODE=123456
curl -sS -c "$CJ" -b "$CJ" -w "HTTP %{http_code}\n" -X POST "$BASE/v1/auth/otp/login" \
  -H 'content-type: application/json' \
  -d "{\"username\":\"demo\",\"code\":\"$CODE\"}"
curl -sS -b "$CJ" -w "HTTP %{http_code}\n" "$BASE/v1/accounts/me"
```

### Events catalog as admin (expect 200)

```bash
BASE="${AAAX_BASE:-http://localhost:8081}"
CJ=/tmp/aaax-admin.cj
curl -sS -c "$CJ" -w "HTTP %{http_code}\n" -X POST "$BASE/v1/auth/login" \
  -H 'content-type: application/json' \
  -d '{"username":"admin","password":"admin12345"}'
curl -sS -b "$CJ" -w "HTTP %{http_code}\n" "$BASE/v1/admin/events?limit=20"
```

```bash
./examples/curl/register-user.sh myuser myuser@example.com 'password123'
./examples/curl/register-login-me.sh
./examples/curl/events-catalog.sh
./examples/curl/otp-login-session.sh demo
```
