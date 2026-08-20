# AAAX ↔ qs/uaa feature parity

Source of truth companion to [AAAX_BOOKLET.md](./AAAX_BOOKLET.md).  
**Policy:** AAAX is **standalone open-source**. We align on **core identity capabilities**, not a dump of Quinsic-only product APIs.

| | |
|--|--|
| **qs/uaa** | Production Quinsic service (`com.quinsic.uaa` + private `app-core`) |
| **aaax** | Public greenfield (`com.aaax`, Maven Central only) |

---

## Standalone guarantee

| Check | Status |
|-------|--------|
| No `app-core` / private Maven deps in `pom.xml` | ✅ |
| Project Maven settings (`.mvn/settings.xml`) = **Central only** | ✅ |
| `.mvn/maven.config` forces `-s .mvn/settings.xml` | ✅ |
| Clone → `mvn test` without GH packages token | ✅ |

```bash
git clone https://github.com/yky32/aaax.git && cd aaax
mvn test          # uses .mvn/settings.xml automatically
mvn spring-boot:run
```

---

## Capability matrix

Legend: ✅ in aaax · 🟡 partial · ❌ not in OSS aaax (by design or later) · 🔒 qs-only (needs private stack)

### A — Accounts

| Capability | qs/uaa | aaax |
|------------|--------|------|
| Register user | ✅ `/users/registrations`, `/users` | ✅ `/v1/accounts/register` + compat `/users/registrations` |
| Get me | ✅ `/users/me` | ✅ `/v1/accounts/me` |
| Change password | ✅ credentials APIs | ✅ `PUT /v1/accounts/me/password` |
| Forgot / reset password | ✅ `/users/credentials/reset*` | ✅ `/v1/accounts/password/*` + compat paths |
| Admin list/get/disable users | ✅ `/mgt/users*` | ✅ `/v1/admin/users*` |
| Soft delete / bulk internal testing | ✅ | ❌ later |
| Username rename admin | ✅ | ❌ later |
| Ext registration / IDV | 🔒 onboarding/idv | ❌ never (product-specific) |

### B — Authentication

| Capability | qs/uaa | aaax |
|------------|--------|------|
| OAuth2/OIDC AS | ✅ | ✅ Spring AS |
| Password form login | ✅ | ✅ |
| OTP issue/verify | ✅ `/authentications/one-time-passwords/*` | ✅ `/v1/otp/*` + compat paths |
| OTP passwordless session login | custom grants | ✅ `/v1/auth/otp/login` |
| custom-password / encrypted / SMS / QR grants | ✅ many | ❌ QR/device grants — roadmap (`docs/ROADMAP.md`) |
| Social (Google/Apple) | ✅ | 🟡 Google + GitHub optional (`profile=social`) |
| Device binding | ✅ | ❌ policy binding — roadmap; passkeys = authenticator-bound when enabled |
| Passkeys / WebAuthn | 🔒 | 🟡 opt-in webauthn4j (`aaax.passkeys.enabled`) |
| Redis multi-node OTP | ✅ prod | ✅ `aaax.otp.store=redis` |
| Refresh token (OAuth) | ✅ | ✅ standard AS |

### C — Authorization

| Capability | qs/uaa | aaax |
|------------|--------|------|
| Roles on user | ✅ permissions/routes | ✅ account `roles` (USER/ADMIN) |
| OAuth scopes | ✅ | ✅ `openid profile api.read` |
| RBAC templates / tenant routes | 🔒 tgt multi-tenant | ❌ never as qs dump |
| Client CRUD | ✅ `/clients` | ✅ `/v1/admin/clients` |
| Protected resource sample | internal | ✅ `/v1/api/hello` |

### D — Platform (explicitly NOT ported)

| qs/uaa area | Why not in aaax OSS |
|-------------|---------------------|
| Kafka listeners / events | private ops bus |
| Redis authz service (prod multi-node) | optional later, not required for clone |
| profile-svc / tenant-svc / util-svc / idv clients | Quinsic mesh |
| Discord/ELK webhooks | ops |
| WebSocket QR login | product-specific — see ROADMAP |
| Housekeeping tokens / stats dashboards | ops |
| `app-core` BaseEntity, BizException codes | private lib — aaax has own types |

---

## Path map (compat)

| qs/uaa-style | aaax preferred | Notes |
|--------------|----------------|-------|
| `POST /users/registrations` | `POST /v1/accounts/register` | both work |
| `POST /authentications/one-time-passwords/general` | `POST /v1/otp/request` | both |
| `POST .../verifications` | `POST /v1/otp/verify` | both |
| `POST /users/credentials/reset` | `POST /v1/accounts/password/forgot` | both |
| `PUT .../reset/one-time-passwords` | `POST /v1/accounts/password/reset` | both |
| `POST /clients` | `POST /v1/admin/clients` | admin session |
| `/oauth2/*` | `/oauth2/*` | standard |

---

## Implementation phases

| Phase | Goal |
|-------|------|
| **0.3 (now)** | Standalone + core accounts/authn/authz + password reset + admin users/clients + UAA-ish public aliases |
| **0.4** | Social login pack, refresh hardening, Redis OTP option |
| **0.5** | RBAC resource catalog (generic, not tgt routes) |
| **never** | Blind port of tenant/IDV/Kafka/Quinsic mesh |

---

*Parity means **usable identity server** for open source — not a Quinsic binary clone.*
