# Calling a resource API with AAAX tokens

AAAX issues OAuth2 access tokens. Your APIs validate JWT via JWKS.

## Issuer / JWKS

| | Default local |
|--|--|
| Issuer | `http://localhost:8081` |
| Discovery | `GET /.well-known/openid-configuration` |
| JWKS | `GET /oauth2/jwks` |

## Client credentials (service-to-service)

```bash
TOKEN=$(curl -sS -u 'aaax-demo:aaax-demo-secret' \
  -X POST http://localhost:8081/oauth2/token \
  -d 'grant_type=client_credentials&scope=api.read' | jq -r .access_token)

curl -sS https://your-api.example/v1/things \
  -H "Authorization: Bearer $TOKEN"
```

Required authority on resource server: **`SCOPE_api.read`** (Spring Security default for scope `api.read`).

## Spring Resource Server (sketch)

```xml
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-oauth2-resource-server</artifactId>
</dependency>
```

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: http://localhost:8081
          # or jwk-set-uri: http://localhost:8081/oauth2/jwks
```

```java
http.authorizeHttpRequests(a -> a
    .requestMatchers("/api/**").hasAuthority("SCOPE_api.read")
    .anyRequest().permitAll())
  .oauth2ResourceServer(o -> o.jwt(Customizer.withDefaults()));
```

## Authorization code (user login)

1. Browser: user hits AAAX `/oauth2/authorize?...&client_id=...&redirect_uri=...`
2. Login form (or OTP session) on AAAX
3. Redirect back with `code`
4. Your BFF exchanges code at `/oauth2/token`
5. BFF calls APIs with Bearer access token

See booklet §14 for full authorize URL + token exchange.

## Built-in sample

AAAX itself exposes:

```http
GET /v1/api/hello
Authorization: Bearer <token with scope api.read>
```

Use `./examples/curl/get-token-and-hello.sh` against that endpoint before wiring your own service.

## Runnable external example

Full Boot 4.1 mini app (separate process on **:8082**):

→ [resource-server-boot4/](./resource-server-boot4/)

```bash
cd examples/resource-server-boot4 && mvn spring-boot:run
./examples/resource-server-boot4/call.sh
```
