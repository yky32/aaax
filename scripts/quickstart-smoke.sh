#!/usr/bin/env bash
# Minimal smoke after AAAX is up on :8081
set -euo pipefail
BASE="${AAAX_BASE:-http://localhost:8081}"

echo "== health / actuator (if exposed) =="
curl -sS -o /dev/null -w "GET /  -> %{http_code}\n" "$BASE/" || true

echo "== OIDC discovery =="
if curl -sf "$BASE/.well-known/openid-configuration" | head -c 400; then
  echo
else
  echo "(no discovery yet — app may still be starting or path differs)"
fi

echo
echo "== JWKS =="
if curl -sf "$BASE/oauth2/jwks" | head -c 200; then
  echo
else
  echo "(jwks not ready)"
fi

echo
echo "OK — if discovery/jwks return JSON, AS is up."
echo "Token (needs client + user already in DB): ./scripts/token-smoke.sh"
