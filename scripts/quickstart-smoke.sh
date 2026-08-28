#!/usr/bin/env bash
# Minimal smoke after AAAX is up on :8081
set -euo pipefail
BASE="${AAAX_BASE:-http://localhost:8081}"

echo "== health / actuator (if exposed) =="
curl -sS -o /dev/null -w "GET /  -> %{http_code}\n" "$BASE/" || true

echo "== RFC 8414 AS metadata =="
if curl -sf "$BASE/.well-known/oauth-authorization-server" | grep -q '"issuer"'; then
  curl -sf "$BASE/.well-known/oauth-authorization-server" | head -c 400
  echo
else
  echo "FAIL: /.well-known/oauth-authorization-server did not return issuer" >&2
  exit 1
fi

echo
echo "== OIDC discovery =="
if curl -sf "$BASE/.well-known/openid-configuration" | grep -q '"issuer"'; then
  curl -sf "$BASE/.well-known/openid-configuration" | head -c 400
  echo
else
  echo "FAIL: /.well-known/openid-configuration did not return issuer" >&2
  exit 1
fi

echo
echo "== JWKS =="
if curl -sf "$BASE/oauth2/jwks" | grep -q '"keys"'; then
  curl -sf "$BASE/oauth2/jwks" | head -c 200
  echo
else
  echo "FAIL: /oauth2/jwks not ready" >&2
  exit 1
fi

echo
echo "OK — 8414 + OIDC discovery + JWKS."
echo "Token (needs client + user already in DB): ./scripts/token-smoke.sh"
echo "PKCE public client (needs AAAX_LOCAL_SEED): ./scripts/pkce-smoke.sh"
echo "Hosted /login + code (needs seed): ./scripts/hosted-authorize-smoke.sh"
