# Examples

| Path | |
|------|--|
| [curl/](./curl/) | health, token, register, OTP, **admin events** |
| [identity-events/](./identity-events/) | Kafka consumer for Identity Event Bus |
| [resource-call.md](./resource-call.md) | Resource server JWT sketch |

```bash
./examples/curl/get-token-and-hello.sh
./examples/curl/login-admin-and-events.sh
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

Full product docs: [../docs/AAAX_BOOKLET.md](../docs/AAAX_BOOKLET.md)
