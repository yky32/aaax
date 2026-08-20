# Social login

AAAX supports optional **Google** (OIDC) and **GitHub** (OAuth2) for console / app login.

## Enable

```bash
export GOOGLE_CLIENT_ID=...
export GOOGLE_CLIENT_SECRET=...
export GITHUB_CLIENT_ID=...      # optional
export GITHUB_CLIENT_SECRET=...

# recommended profile (both providers)
export SPRING_PROFILES_ACTIVE=social

mvn spring-boot:run
```

Legacy: `SPRING_PROFILES_ACTIVE=google` still works for Google-only.

## Redirect URIs

| Provider | Redirect URI |
|----------|----------------|
| Google | `{issuer}/login/oauth2/code/google` |
| GitHub | `{issuer}/login/oauth2/code/github` |

Local default issuer: `http://localhost:8081`

## Console

http://localhost:8081/admin/ shows **Continue with Google / GitHub** when configured  
(`GET /v1/auth/social/providers`).

## Behaviour

1. User hits `/oauth2/authorization/{google|github}`
2. Provider callback → link or create `Account`
   - Google → `google_sub`
   - GitHub → `github_id`
   - Match existing by email when possible
3. Session established as AAAX user
4. Identity event: `com.aaax.auth.login.social` (`data.provider`)
5. ADMIN users → `/admin/` · others → `/?social=ok`

## API

```http
GET /v1/auth/social/providers
```

```json
{
  "enabled": true,
  "providers": [
    { "id": "google", "label": "Google", "authorizationUrl": "/oauth2/authorization/google" }
  ]
}
```

## Not included

- Apple / Facebook / Microsoft (add as more Spring registrations later)
- Account linking UI for already-logged-in users
