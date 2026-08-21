# `com.aaax.core` — foundation layer

Public OSS stand-in for what private stacks put in **app-core**.

Inspired by ledger-engine’s `com.altech.core`, kept **thin** (no Lombok dump, no R/Response mega-API).

## Layout

```text
com.aaax.core
├── entity/AuditableEntity.java   # created_at / updated_at MappedSuperclass
├── exception/BizException.java   # HTTP-aware business errors + code
├── id/Ids.java                   # UUID helpers
└── web/GlobalExceptionHandler.java
```

## What lives here

| Type | Role |
|------|------|
| `AuditableEntity` | Shared timestamp fields — `Account`, `PasskeyCredential`, `TrustedDevice` extend it |
| `BizException` | Prefer in use cases; `AccountException` extends it |
| `Ids.uuid()` | Consistent id generation |
| `GlobalExceptionHandler` | JSON `{ status, code, message, fields? }` |

## What does **not** live here

- Account / OAuth / passkey domain logic
- Quinsic packages
- Multi-module hexagon

## vs private app-core

| app-core (private) | AAAX core |
|--------------------|-----------|
| BaseEntity + tenant | `AuditableEntity` (timestamps only) |
| BizException + R | `BizException` + plain JSON map |
| Shared across monorepo jars | Same module, clear package |

Domain packages **may** depend on `core`. Core must not depend on domain.
