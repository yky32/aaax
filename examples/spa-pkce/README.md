# SPA PKCE example (thin browser client)

Minimal **public** OIDC client for AAAX — no npm, no framework.

| | |
|--|--|
| Client id | `aaax-spa` (seeded when `aaax.demo.seed-client=true`) |
| Auth | Authorization code + **PKCE S256** · no client secret |
| Helper | [`aaax.js`](./aaax.js) — `Aaax.create` / `login` / `handleRedirectCallback` / `fetchJson` |

## Run

```bash
# terminal 1 — AAAX
cd ../.. && mvn spring-boot:run

# terminal 2 — static SPA on :4173 (must match redirect_uri)
cd examples/spa-pkce
python3 -m http.server 4173
```

Open http://127.0.0.1:4173/ → **Sign in with AAAX** → login as `demo` / `demo1234` → **GET /v1/api/hello**.

## Helper API

```js
const aaax = Aaax.create({
  issuer: "http://localhost:8081",
  clientId: "aaax-spa",
  redirectUri: location.origin + "/callback.html", // optional
  scopes: "openid profile api.read",               // optional
});

await aaax.login();                    // redirect to /oauth2/authorize
await aaax.handleRedirectCallback(); // on callback page
aaax.getAccessToken();
await aaax.fetchJson("/v1/api/hello");
aaax.logoutLocal();
```

Copy `aaax.js` into your app, or later publish as `@aaax/browser` (same surface).

## Server notes

- Demo seed registers `aaax-spa` with `requireProofKey=true` and `ClientAuthenticationMethod.NONE`.
- CORS allows `http://localhost:*` / `127.0.0.1:*` on `/oauth2/**` and `/v1/**`.
- For production: create your own public client + exact redirect URIs; set `AAAX_DEMO_SEED_CLIENT=false`.

Booklet: [docs/booklet.md](../../docs/booklet.md).
