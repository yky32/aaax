# Compose mesh — AAAX golden path (v0.6)

**Identity Event Bus wedge, production-shaped:**

```text
Browser / API
    ↓
AAAX (Postgres + Redis OTP store)
    ↓ IdentityEventBus catalog v1.0
    ├─► Kafka aaax.identity.events  → sample-notify (your SMS stand-in)
    └─► Webhook + HMAC              → webhook_server.py (verify signature)
```

## Run

```bash
# repo root
mvn -DskipTests package
cd examples/compose-mesh
docker compose up --build
```

| Service | Port |
|---------|------|
| AAAX | http://localhost:8081 |
| Postgres | localhost:5433 |
| Redis | localhost:6379 |
| Kafka | localhost:9092 |
| Webhook verifier | localhost:8099 |
| sample-notify | logs only |

## Smoke

```bash
curl -sS http://localhost:8081/actuator/health

# login → com.aaax.auth.login on Kafka + signed webhook
curl -sS -c /tmp/aaax.cj -X POST http://localhost:8081/v1/auth/login \
  -H 'content-type: application/json' \
  -d '{"username":"admin","password":"admin12345"}'

# OTP → com.aaax.otp.dispatch (channel=kafka; bus delivers)
curl -sS -X POST http://localhost:8081/v1/otp/request \
  -H 'content-type: application/json' \
  -d '{"username":"demo"}'

docker compose logs -f sample-notify webhook
```

Expect:

- `[notify] com.aaax.auth.login … catalog=1.0`
- `[webhook] OK type=com.aaax.auth.login …`
- OTP: `would SMS/email` with code in notify logs

## HA defaults (this compose)

| Concern | Setting |
|---------|---------|
| DB | Postgres |
| OTP / magic multi-node | `AAAX_OTP_STORE=redis` |
| Events | Kafka + webhook HMAC |
| Single-node dev | omit Redis/Kafka; defaults memory + log sink |

## Webhook verify (your code)

```text
sig = headers["X-AAAX-Signature"]  # sha256=<hex>
expected = "sha256=" + hmac_sha256_hex(secret, raw_body)
assert sig == expected
idempotency_key = headers["X-AAAX-Delivery-Id"]  # == event id
```

Catalog: `GET /v1/admin/events/catalog` (admin session).  
Booklet: [docs/booklet.md §15](../../docs/booklet.md#15-identity-event-bus).

## Stop

```bash
docker compose down -v
```
