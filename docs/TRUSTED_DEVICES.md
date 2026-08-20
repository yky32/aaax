# Trusted devices (policy device trust)

Light **remember this browser** — not MDM / cert binding.

When a user completes password (+ optional TOTP) with `rememberDevice=true`, AAAX sets HttpOnly cookie `AAAX_DEVICE` and stores a **SHA-256 hash** of the token.

On later password logins, if TOTP is enabled and the cookie matches an active trusted device for that account, **MFA is skipped**.

## API

| Method | Path | |
|--------|------|--|
| GET | `/v1/devices` | list active |
| POST | `/v1/devices` | trust current browser (`{ "label": "…" }` optional) |
| DELETE | `/v1/devices/{id}` | revoke one |
| POST | `/v1/devices/revoke-all` | revoke all + clear cookie |

## Login body

```json
POST /v1/auth/login
{ "username": "demo", "password": "…", "rememberDevice": true, "deviceLabel": "Laptop" }

POST /v1/auth/mfa/totp
{ "code": "123456", "rememberDevice": true }
```

Response may include `mfaSkipped: true` / `trustedDevice: true`.

## Config

```yaml
aaax:
  devices:
    ttl-days: 30
    cookie-secure: false   # true behind HTTPS
```

## UI

- Sign-in: checkbox **Remember this device**
- Account `/user/`: Trusted devices list

## Security notes

- Cookie is opaque random; only hash is stored.
- Revoke from `/user/` or API when device is lost.
- Distinct from **passkeys** (crypto authenticator) and **QR login** (session pairing).
- Not a full “only allow listed devices” lockdown — that would be a separate product gate.
