# Architecture — how the code is organized

For a **clone → open files in order** tour, read **[CODEMAP.md](./CODEMAP.md)** first.

## Goals (open source)

1. A stranger can find **login / register / token / events** in under 10 minutes.
2. One user action ≈ one `*UseCase` class (grep-friendly).
3. No private frameworks — plain Spring Boot.

## Layers

```text
web/*Endpoint            HTTP adapt only
  ↓
*.application.*UseCase   one user intent
AccountQueries           reads
  ↓
Repository / SPI         JPA, OtpSender, EventBus sinks
config/*                 Security, SAML, Social
```

## Pattern choice

| We use | We avoid (OSS noise) |
|--------|----------------------|
| Feature packages (`account`, `auth`, `events`) | Deep enterprise hexagon with 12 modules |
| `*UseCase` for writes | Interface + Impl pair for every action |
| `*Endpoint` for HTTP (not `*Controller`) | GodService |
| `package-info.java` + CODEMAP | Tribal knowledge only |
| SPI (`OtpSender`, `IdentityEventSink`) | Hard-coded Twilio inside core |

## Rules

1. Endpoints do not open transactions or build SecurityContext (use `FinishAuthenticatedSession`).
2. New write features → `*UseCase` under the feature’s `application` package.
3. Reads may use `AccountQueries` / small services.
4. Keep concrete `@Component` / `@RestController` classes (annotation stays Spring; **class names** use Endpoint).


## Related

- [CODEMAP.md](./CODEMAP.md) — start-here file list
- [IDENTITY_EVENTS.md](./IDENTITY_EVENTS.md) — event bus product surface
