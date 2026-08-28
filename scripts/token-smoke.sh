#!/usr/bin/env bash
# POST /oauth2/token (custom-password-grant). Exit 0 only when the body has a token.
# Defaults match AAAX_LOCAL_SEED (client/secret + smoke.primary@aaax.local).
set -euo pipefail
BASE="${AAAX_BASE:-http://localhost:8081}"
CLIENT_ID="${AAAX_CLIENT_ID:-client}"
CLIENT_SECRET="${AAAX_CLIENT_SECRET:-secret}"
USERNAME="${AAAX_USERNAME:-smoke.primary@aaax.local}"
CREDENTIALS="${AAAX_CREDENTIALS:-SmokePrimary!1}"

echo "== POST /oauth2/token (custom-password-grant) as ${USERNAME} =="
body="$(mktemp)"
trap 'rm -f "$body"' EXIT
code="$(curl -sS -o "$body" -w "%{http_code}" \
  -u "${CLIENT_ID}:${CLIENT_SECRET}" \
  -X POST "${BASE}/oauth2/token" \
  -H 'content-type: application/x-www-form-urlencoded' \
  -d 'grant_type=custom-password-grant' \
  -d "username=${USERNAME}" \
  -d "credentials=${CREDENTIALS}")"
echo "HTTP $code"
if command -v jq >/dev/null 2>&1; then
  jq . "$body" || cat "$body"
else
  cat "$body"
fi
echo
if grep -Eq '"accessToken"|"access_token"' "$body"; then
  echo "OK — token present"
  exit 0
fi
echo "FAIL — no accessToken / access_token in body (need AAAX_LOCAL_SEED=true and JPA_DDL_AUTO=update)"
exit 1
