# AAAX vs Clerk — parity map (honest)

Clerk is a **hosted SaaS UX + SDK company**. AAAX is a **self-host OIDC appliance**.  
“Same as Clerk” here means **Clerk-class auth experiences you run** — not cloning seat tax, React components CDN, or multi-tenant billing.

## Positioning

| | Clerk | AAAX |
|--|-------|------|
| Deploy | Their cloud | **Your** jar / K8s |
| Data | Their DB | **Yours** |
| Notify/SMS | Their stack | **Your mesh** (Event Bus) |
| UI | Drop-in React components | Hosted pages in jar + your app |
| Win | DX components | Events + Spring OIDC + ownership |

## Feature parity (current)

| Clerk surface | AAAX | Status |
|---------------|------|--------|
| Hosted sign-in | `/sign-in/` | ✅ password · magic · OTP · social |
| Hosted sign-up | `/sign-up/` | ✅ |
| User profile | `/user/` | ✅ |
| Password | yes | ✅ |
| Email code / OTP | yes | ✅ |
| Magic link | yes | ✅ `/v1/auth/magic/*` |
| Social (Google…) | many | 🟡 Google + GitHub |
| Passkeys | yes | 🧪 **experimental** — ceremony + store; not production MFA yet |
| TOTP MFA | yes | ✅ |
| Sessions list / revoke | yes | ✅ `/v1/sessions` |
| Organizations | yes | ❌ single-realm (decision) |
| `<SignIn/>` React SDK | yes | ❌ use hosted pages or build BFF |
| Bot protection / enterprise dashboard | yes | ❌ |
| Webhooks | yes | ✅ **stronger** Identity Event Bus |
| OIDC for your APIs | limited framing | ✅ first-class AS |

## URLs

```text
http://localhost:8081/sign-in/
http://localhost:8081/sign-up/
http://localhost:8081/user/
http://localhost:8081/admin/          # ops console (not end-user)
```

## Still not “full Clerk”

1. Official `@aaax/react` / Next middleware package  
2. Multi-tenant Organizations + invitations  
3. Passkey assertion full WebAuthn crypto verify  
4. Apple / Microsoft social  
5. Hosted component theming API  
6. Fraud / bot suite  

## What we refuse to fake

- Pretending SaaS drop-in widgets = done without shipping an SDK  
- Orgs multi-tenant without product decision  
- Dropping Identity Event Bus wedge to chase UI alone  

Primary story remains:

> **AAAX authenticates. Your mesh notifies.**  
> Now with **Clerk-class hosted sign-in/up + sessions + magic link + passkeys path**.
