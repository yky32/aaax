# Passkeys (WebAuthn)

## Status (v0.5.0)

| | |
|--|--|
| Default | **Disabled** (`aaax.passkeys.enabled=false`) |
| Library | [webauthn4j-core](https://github.com/webauthn4j/webauthn4j) 0.31.9 |
| Verify | Registration (`attestationObject` + `clientDataJSON`) and assertion (`authenticatorData` + `signature` + …) |

## Enable

```bash
export AAAX_PASSKEYS_ENABLED=true
export AAAX_PASSKEYS_RP_ID=localhost          # must match browser host
# origin taken from aaax.issuer (e.g. http://localhost:8081)
mvn spring-boot:run
```

Then open `/user/` while signed in → Add passkey.

## API

| Method | Path | |
|--------|------|--|
| GET | `/v1/passkeys/register/options` | session required |
| POST | `/v1/passkeys/register` | body: `clientDataJSON`, `attestationObject`, `label?` (base64url) |
| GET | `/v1/passkeys` | list |
| DELETE | `/v1/passkeys/{id}` | |
| GET | `/v1/passkeys/authenticate/options?username=` | |
| POST | `/v1/passkeys/authenticate` | body: `challengeKey`, `credentialId`, `authenticatorData`, `clientDataJSON`, `signature`, `userHandle?` |

When disabled, all paths return **404**.

## Ops notes

- `rp-id` and `aaax.issuer` origin must match the site the browser uses.
- Use HTTPS (or localhost) for real authenticators.
- Counter updates on successful assertion.
