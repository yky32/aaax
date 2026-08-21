# Examples

| Path | |
|------|--|
| [curl/](./curl/) | health, token, register, OTP, **admin events** |
| [identity-events/](./identity-events/) | Kafka consumer script |
| [compose-kafka-notify/](./compose-kafka-notify/) | **v0.4 path** — AAAX + Kafka + sample notify |
| [redis-otp-store.md](./redis-otp-store.md) · [compose-redis-otp/](./compose-redis-otp/) | Multi-node OTP/magic (`aaax.otp.store=redis`) |
| [**resource-server-boot4/**](./resource-server-boot4/) | **External** JWT resource server (Boot 4.1) |
| [resource-call.md](./resource-call.md) | Resource server JWT sketch |

```bash
./examples/curl/get-token-and-hello.sh
./examples/curl/login-admin-and-events.sh

# external API validated by AAAX JWKS:
# terminal1: mvn spring-boot:run
# terminal2: cd examples/resource-server-boot4 && mvn spring-boot:run
# ./examples/resource-server-boot4/call.sh
```

## Prerequisites

```bash
# from repo root
mvn spring-boot:run
```

Demo client: `aaax-demo` / `aaax-demo-secret`  
Demo user: `demo` / `demo1234`

## 60-second happy path

```bash
./examples/curl/get-token-and-hello.sh
./examples/curl/register-user.sh alice alice@example.com 'password123'
./examples/curl/otp-login-session.sh demo   # code printed in server logs (console channel)
```

Full product docs: [../docs/booklet.md](../docs/booklet.md)
