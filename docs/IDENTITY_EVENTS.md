# Identity Event Bus — how AAAX wins

## The wedge

Competitors sell **hosted UX** (Clerk) or **kitchen-sink IdP** (Keycloak) or **Node OIDC** (Logto).

AAAX wins for **platform / Spring shops that already own a notification-service**:

> **Identity is a signal bus.**  
> AAAX authenticates; *you* message users (SMS/email/push) on your rails.

No Twilio lock-in. No “buy our messaging add-on”.

## Shape (CloudEvents-ish JSON)

```json
{
  "specversion": "1.0",
  "id": "uuid",
  "source": "http://localhost:8081",
  "type": "com.aaax.auth.login",
  "time": "2026-08-20T12:00:00Z",
  "subject": "demo",
  "data": { "method": "password" }
}
```

## Types

| type | when |
|------|------|
| `com.aaax.account.registered` | register |
| `com.aaax.auth.login` | password / otp login |
| `com.aaax.auth.login.mfa` | password + TOTP |
| `com.aaax.auth.logout` | logout |
| `com.aaax.mfa.totp.enabled` / `.disabled` | MFA |
| `com.aaax.account.password.changed` / `.reset` | password |
| `com.aaax.otp.dispatch` | OTP (includes `code` when channel needs delivery) |
| `com.aaax.client.created` / `.deleted` | OAuth clients |
| `com.aaax.admin.bootstrap` | first admin |
| `com.aaax.admin.user.status` / `.roles` | admin user ops |

## Sinks

| Sink | Enable |
|------|--------|
| **Log** | always |
| **Audit DB** | always (`/v1/admin/audit`) |
| **Kafka** | `AAAX_EVENTS_KAFKA_ENABLED=true` **or** `AAAX_OTP_CHANNEL=kafka` |
| **HTTP webhook** | `AAAX_EVENTS_WEBHOOK_URL=...` (+ optional `AAAX_EVENTS_WEBHOOK_AUTH`) |

Kafka topic default: `aaax.identity.events`  
OTP-only path can still use `aaax.otp.dispatch` topic via `AAAX_OTP_KAFKA_TOPIC` / events topic override.

## Caller pattern

```text
AAAX ──event──► Kafka / webhook ──► your notification-service
                                      ├─ SMS (Twilio/Aliyun/…)
                                      ├─ email
                                      └─ push / Slack
```

## Why this beats “add Twilio in the IdP”

1. You already pay for and operate SMS in **one** place.  
2. Compliance: PII messaging stays in your mesh.  
3. Multi-product: same bus for tgt / indie apps later.  
4. Spring-native producer — fits JVM platform teams.

See also [SMS_SAML.md](./SMS_SAML.md).
