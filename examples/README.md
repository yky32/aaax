# Examples

Developer-facing recipes for **AAAX** after `mvn spring-boot:run` (default `http://localhost:8081`).

| Path | |
|------|--|
| [curl/](./curl/) | Shell scripts — token, hello API, register, OTP login |
| [resource-call.md](./resource-call.md) | How a backend/BFF should call AAAX-protected APIs |

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
