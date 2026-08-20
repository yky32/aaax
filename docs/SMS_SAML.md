# OTP dispatch · SMS · SAML

## Org model
**single** — one realm per deployment (no multi-tenant orgs yet).

## SMS — two modes (your decision)

AAAX **does not** embed Twilio or any carrier SDK.

### Mode 1 — `AAAX_OTP_CHANNEL=kafka`
Publish `OtpDispatchEvent` JSON to Kafka. Your **notification-service** consumes and sends SMS with your own provider.

```bash
export AAAX_OTP_CHANNEL=kafka
export AAAX_OTP_KAFKA_BOOTSTRAP=localhost:9092
export AAAX_OTP_KAFKA_TOPIC=aaax.otp.dispatch
```

Event shape (`com.aaax.otp.OtpDispatchEvent`):

```json
{
  "eventType": "aaax.otp.dispatch",
  "username": "demo",
  "destination": "+8529...",
  "channel": "kafka",
  "code": "123456",
  "purpose": "otp",
  "expiresAt": "2026-08-20T12:00:00Z",
  "issuer": "http://localhost:8081"
}
```

### Mode 2 — `AAAX_OTP_CHANNEL=sms`
HTTP POST webhook to **your** notification endpoint (any provider behind it).

```bash
export AAAX_OTP_CHANNEL=sms
export AAAX_OTP_SMS_WEBHOOK_URL=https://notify.internal/v1/sms
export AAAX_OTP_SMS_WEBHOOK_AUTH="Bearer ***"
```

JSON body includes `to`, `message`, `code`, `purpose`, `issuer`.

### Also
- `console` — log (dev)
- `mail` — SMTP

## SAML 2.0 Service Provider

Login with an **external IdP** (AAAX is SP).

```bash
export AAAX_SAML_ENABLED=true
export AAAX_SAML_IDP_METADATA_URI=https://idp.example/metadata
# optional:
export AAAX_SAML_SP_ENTITY_ID=http://localhost:8081/saml2/metadata
export AAAX_SAML_REGISTRATION_ID=idp
```

Browser: `/saml2/authenticate/idp` → IdP → ACS `/login/saml2/sso/idp` → account link by NameID/email → session.

**Maven:** OpenSAML is resolved from **Shibboleth public** repo (not private packages). See `.mvn/settings.xml`.

### Not yet
- AAAX as **SAML IdP** (serve SAML apps) — follow-up decision `saml_idp`
- Passkeys — later

## Passkeys
Deferred.
