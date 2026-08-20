# Architecture — application layer (UseCase pattern)

AAAX uses a **lightweight UseCase style** for write/workflow paths.  
Not full Quinsic/DDD ceremony — no interface-per-use-case, no DtoWrapper.

## Layers

```text
web/*Controller          HTTP adapt only
  ↓
*.application.*UseCase   one user intent = execute/handle
AccountQueries           read models
  ↓
Repository / SPI         JPA, OtpSender, EventBus sinks
config/*                 Security, SAML, Social glue
```

## Packages

| Package | Role |
|---------|------|
| `account.application` | Register, bootstrap, password, admin user, TOTP MFA, federate |
| `auth.application` | Password/OTP/magic login, logout, **FinishAuthenticatedSession** |
| `otp.application` | RequestOtpUseCase (wraps OtpService) |
| `otp` / `events` / `session` | Domain helpers + infrastructure |

## Rules

1. Controllers do not open transactions or assemble SecurityContext (except via UseCase).
2. All successful logins go through `FinishAuthenticatedSession` (session + track + event).
3. New write features → Prefer `*UseCase` over growing a GodService.
4. Reads may stay on `AccountQueries` / thin services.
5. Keep concrete `@Component` classes (no mandatory interface).

## Deleted

- `AccountService` GodService (~400 LOC) → split use cases
