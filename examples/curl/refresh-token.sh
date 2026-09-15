#!/usr/bin/env bash
# custom-password-grant → grant_type=refresh_token → new access_token
# Defaults match AAAX_LOCAL_SEED (client/secret + smoke.primary@aaax.local).
set -euo pipefail
BASE="${AAAX_BASE:-http://localhost:8081}"
CLIENT_ID="${AAAX_CLIENT_ID:-client}"
CLIENT_SECRET="${AAAX_CLIENT_SECRET:-secret}"
USERNAME="${AAAX_USERNAME:-smoke.primary@aaax.local}"
CREDENTIALS="${AAAX_CREDENTIALS:-SmokePrimary!1}"

echo "== POST /oauth2/token (custom-password-grant) =="
code=$(curl -sS -o /tmp/aaax-login-rt.json -w "%{http_code}" \
  -u "${CLIENT_ID}:${CLIENT_SECRET}" \
  -X POST "${BASE}/oauth2/token" \
  -H 'content-type: application/x-www-form-urlencoded' \
  -d 'grant_type=custom-password-grant' \
  -d "username=${USERNAME}" \
  -d "credentials=${CREDENTIALS}")
echo "HTTP $code (expect 200)"
command -v jq >/dev/null && jq . /tmp/aaax-login-rt.json || cat /tmp/aaax-login-rt.json
echo

REFRESH=""
if command -v jq >/dev/null 2>&1; then
  REFRESH="$(jq -r '.refresh_token // empty' /tmp/aaax-login-rt.json)"
else
  REFRESH="$(sed -n 's/.*"refresh_token":"\([^"]*\)".*/\1/p' /tmp/aaax-login-rt.json | head -1)"
fi
if [[ -z "$REFRESH" || "$REFRESH" == "null" ]]; then
  echo "FAIL — no refresh_token"
  exit 1
fi

echo "== POST /oauth2/token (grant_type=refresh_token) =="
code=$(curl -sS -o /tmp/aaax-refresh.json -w "%{http_code}" \
  -u "${CLIENT_ID}:${CLIENT_SECRET}" \
  -X POST "${BASE}/oauth2/token" \
  -H 'content-type: application/x-www-form-urlencoded' \
  -d 'grant_type=refresh_token' \
  --data-urlencode "refresh_token=${REFRESH}")
echo "HTTP $code (expect 200)"
command -v jq >/dev/null && jq . /tmp/aaax-refresh.json || cat /tmp/aaax-refresh.json
echo
