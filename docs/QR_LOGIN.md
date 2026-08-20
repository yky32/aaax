# QR code login

Desktop shows a QR (or short code). A **phone already signed in** to AAAX approves → desktop session is created.

```text
Desktop                         Phone (session)
   | POST /v1/auth/qr/sessions        |
   |← sessionId, approveUrl, userCode |
   | poll GET …/sessions/{id}         |
   |                                  | open approveUrl (scan QR)
   |                                  | POST …/sessions/{id}/approve
   | status=APPROVED                  |
   | POST …/sessions/{id}/consume     |
   |← FinishAuthenticatedSession      |
```

## API

| Method | Path | Auth |
|--------|------|------|
| `POST` | `/v1/auth/qr/sessions` | public — create |
| `GET` | `/v1/auth/qr/sessions/{id}` | public — poll |
| `POST` | `/v1/auth/qr/sessions/{id}/approve` | **session** — phone |
| `POST` | `/v1/auth/qr/approve-code` | **session** — body `{ "userCode": "…" }` |
| `POST` | `/v1/auth/qr/sessions/{id}/consume` | public — desktop finishes login |

## UI

- Desktop: `/sign-in/` → tab **QR**
- Phone: `/sign-in/qr-approve.html?sid={sessionId}` (encoded in QR)

## Config

```yaml
aaax:
  qr:
    ttl-seconds: 120   # AAAX_QR_TTL_SECONDS
```

Store is **in-memory** (single node). Multi-node: same pattern as OTP Redis later.

## Events

- `com.aaax.auth.qr.created`
- `com.aaax.auth.qr.approved`
- `com.aaax.auth.login` (`method=qr`) on consume

## Security notes

- Approve requires an existing authenticated session on the phone.
- Session is single-use after consume; TTL default 120s.
- Do not approve unknown QR prompts.
