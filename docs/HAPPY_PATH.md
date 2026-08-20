# AAAX happy path (curl)

Base URL default: `http://localhost:8081`

## 0. Start

```bash
mvn spring-boot:run
# or
mvn -DskipTests package && docker compose up --build
```

## 1. Register

```bash
curl -sS -X POST http://localhost:8081/v1/accounts/register \
  -H 'content-type: application/json' \
  -d '{"username":"alice","email":"alice@example.com","password":"password123"}' | jq
```

## 2. Client-credentials token → protected API

```bash
TOKEN=$(curl -sS -u 'aaax-demo:aaax-demo-secret' \
  -X POST http://localhost:8081/oauth2/token \
  -d 'grant_type=client_credentials&scope=api.read' | jq -r .access_token)

curl -sS http://localhost:8081/v1/api/hello \
  -H "Authorization: Bearer $TOKEN" | jq
```

## 3. OTP (dev: code printed in server logs)

```bash
curl -sS -X POST http://localhost:8081/v1/otp/request \
  -H 'content-type: application/json' \
  -d '{"username":"demo"}' | jq

# copy code from log line: AAAX OTP for ...
curl -sS -X POST http://localhost:8081/v1/otp/verify \
  -H 'content-type: application/json' \
  -d '{"username":"demo","code":"123456"}' | jq
```

## 4. Authorization code (browser)

1. Open login: http://localhost:8081/login  
   Demo user: `demo` / `demo`
2. Authorize (demo client has consent disabled):

```text
http://localhost:8081/oauth2/authorize?response_type=code&client_id=aaax-demo&redirect_uri=http://127.0.0.1:3000/login/oauth2/code/aaax&scope=openid%20profile%20api.read&state=xyz
```

3. Exchange code:

```bash
curl -sS -u 'aaax-demo:aaax-demo-secret' \
  -X POST http://localhost:8081/oauth2/token \
  -d 'grant_type=authorization_code' \
  -d 'code=PASTE_CODE' \
  -d 'redirect_uri=http://127.0.0.1:3000/login/oauth2/code/aaax' | jq
```

## 5. OIDC discovery / JWKS

```bash
curl -sS http://localhost:8081/.well-known/openid-configuration | jq
curl -sS http://localhost:8081/oauth2/jwks | jq
```

JWK file (stable across restarts): `./data/aaax-jwk.json` (`AAAX_JWK_PATH`).

## Demo credentials

| Kind | Value |
|------|--------|
| User | `demo` / `demo` |
| Admin user | `admin` / `admin12345` (ROLE_ADMIN) |
| OAuth client | `aaax-demo` / `aaax-demo-secret` |
