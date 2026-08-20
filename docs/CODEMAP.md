# Code map — start here after `git clone`

AAAX is a **single Spring Boot module**. Open these files in order to understand the product.

## 5-minute tour

| Step | Open | Why |
|-----:|------|-----|
| 1 | [`AaaxApplication.java`](../src/main/java/com/aaax/AaaxApplication.java) | Boot entry |
| 2 | [`config/SecurityConfig.java`](../src/main/java/com/aaax/config/SecurityConfig.java) | **How HTTP is secured** — 3 filter chains (OIDC AS · API JWT · session app) |
| 3 | [`auth/application/PasswordLoginUseCase.java`](../src/main/java/com/aaax/auth/application/PasswordLoginUseCase.java) | Login intent |
| 4 | [`auth/application/FinishAuthenticatedSession.java`](../src/main/java/com/aaax/auth/application/FinishAuthenticatedSession.java) | **All logins end here** — password · OTP · magic · social · SAML · passkey |
| 5 | [`events/IdentityEventBus.java`](../src/main/java/com/aaax/events/IdentityEventBus.java) | Product wedge — signals out to Kafka/webhook |
| 6 | [`web/AuthController.java`](../src/main/java/com/aaax/web/AuthController.java) | HTTP surface for `/v1/auth/*` (thin) |

Then optionally:

| Topic | Start |
|-------|--------|
| Register user | `account/application/RegisterAccountUseCase.java` |
| OTP / SMS channels | `otp/OtpService.java` + `OtpSender` + **`OtpCodeStore`** (`memory` \| `redis`) |
| Magic link tokens | `auth/MagicLinkTokenStore` (same store mode as OTP) |
| OAuth clients admin | `client/ClientAdminService.java` |
| Social Google/GitHub | `config/SocialLoginConfig.java` |
| SAML SP | `config/SamlSpConfig.java` |
| Passkeys (experimental) | `passkey/*` — **off unless** `aaax.passkeys.enabled=true` |
| Hosted UI | `src/main/resources/static/sign-in/` · `admin/` |

## Package layout (screaming + simple)

```text
com.aaax
├── config/                 # Spring Security, JWK, Kafka, Social, SAML
├── account/                # User entity + register/password/MFA use cases
│   └── application/
├── auth/                   # Login / logout / magic-link use cases
│   └── application/
├── otp/                    # One-time codes + channel SPI (console/mail/kafka/sms)
├── events/                 # Identity Event Bus (product differentiator)
├── session/                # Tracked sessions for /user
├── passkey/                # Experimental WebAuthn
├── client/                 # OAuth2 registered clients
├── audit/                  # DB audit trail
├── web/                    # REST controllers (HTTP only)
└── compat/                 # Optional qs/uaa-shaped path aliases
```

**Rule of thumb for readers:**

- Want **HTTP contract** → `web/*Controller`
- Want **business steps** → `*/application/*UseCase`
- Want **security wiring** → `config/SecurityConfig`
- Want **product moat** → `events/*`

## Design pattern (what we chose for OSS)

| Choice | Why (for downloaders) |
|--------|------------------------|
| **UseCase for writes** | One class ≈ one user action — easy to grep `Register`, `Login` |
| **No interface-per-use-case** | Less file noise in GitHub UI |
| **No private frameworks** | Pure Spring + Central deps |
| **SPI for OTP/events** | Extension points obvious (`OtpSender`, `IdentityEventSink`) |
| **GodService banned** | `AccountService` removed so logic is not a 400-line maze |

Not Hexagonal full-boat. Not Quinsic monorepo. Readable Spring.

## Main runtime flows

### Password login

```text
POST /v1/auth/login
  → PasswordLoginUseCase
      → PasswordUseCase.authenticatePassword
      → (optional MFA pending in HTTP session)
      → FinishAuthenticatedSession
          → Spring Security context
          → AuthSession row
          → IdentityEventBus AUTH_LOGIN
```

### Social / SAML

```text
OAuth2/SAML success handler
  → FederateAccountUseCase (link/create)
  → FinishAuthenticatedSession
  → event AUTH_LOGIN_SOCIAL (+ provider)
  → redirect /admin or /user
```

### OTP login

```text
POST /v1/otp/request  → OtpService (+ event OTP_DISPATCH + channel send)
POST /v1/auth/otp/login → OtpLoginUseCase → FinishAuthenticatedSession
```

### Client credentials API call

```text
POST /oauth2/token  (AS filter chain)
GET  /v1/api/hello  (JWT filter chain, scope api.read)
```

## Tests as documentation

```bash
mvn test
# AaaxApplicationTests exercises register, token, OTP, magic link, admin
```

## Related docs

- Product booklet: [AAAX_BOOKLET.md](./AAAX_BOOKLET.md)
- Events: [IDENTITY_EVENTS.md](./IDENTITY_EVENTS.md)
- Layering rules: [ARCHITECTURE.md](./ARCHITECTURE.md)
