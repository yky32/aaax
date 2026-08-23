# Examples

| Path | |
|------|--|
| [**spa-pkce/**](./spa-pkce/) | **Browser PKCE** — thin `aaax.js` + public client `aaax-spa` |
| [**compose-mesh/**](./compose-mesh/) | **v0.6 golden path** — Postgres + Redis + Kafka + HMAC webhook + notify |
| [curl/](./curl/) | health, token, register, OTP, admin events - see [curl/README.md](./curl/README.md) for status codes |
| [compose-kafka-notify/](./compose-kafka-notify/) | Lighter Kafka-only demo |
| [redis-otp-store.md](./redis-otp-store.md) · [compose-redis-otp/](./compose-redis-otp/) | Redis OTP/magic |
| [**resource-server-boot4/**](./resource-server-boot4/) | External JWT resource server (Boot 4.1) |
| [identity-events/](./identity-events/) | Kafka consumer script |

```bash
./examples/curl/get-token-and-hello.sh
./examples/curl/login-admin-and-events.sh
./examples/curl/register-login-me.sh
./examples/curl/events-catalog.sh

# mesh:
mvn -DskipTests package && cd examples/compose-mesh && docker compose up --build
```

Full product docs: [../docs/booklet.md](../docs/booklet.md)
