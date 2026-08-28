#!/usr/bin/env bash
# custom-password-grant → GET /users/me
# Defaults match AAAX_LOCAL_SEED (client/secret + smoke.primary@aaax.local).
set -euo pipefail
BASE="${AAAX_BASE:-http://localhost:8081}"
CLIENT_ID="${AAAX_CLIENT_ID:-client}"
CLIENT_SECRET="${AAAX_CLIENT_SECRET:-secret}"
USERNAME="${AAAX_USERNAME:-smoke.primary@aaax.local}"
CREDENTIALS="${AAAX_CREDENTIALS:-SmokePrimary!1}"

echo "== POST /oauth2/token =="
code=$(curl -sS -o /tmp/aaax-tok.json -w "%{http_code}" \
  -u "${CLIENT_ID}:${CLIENT_SECRET}" \
  -X POST "${BASE}/oauth2/token" \
  -H 'content-type: application/x-www-form-urlencoded' \
  -d 'grant_type=custom-password-grant' \
  -d "username=${USERNAME}" \
  -d "credentials=${CREDENTIALS}")
echo "HTTP $code (expect 200)"
command -v jq >/dev/null && jq . /tmp/aaax-tok.json || cat /tmp/aaax-tok.json
echo

TOKEN=""
if command -v jq >/dev/null 2>&1; then
  TOKEN="$(jq -r '.data.accessToken // .accessToken // .access_token // empty' /tmp/aaax-tok.json)"
else
  TOKEN="$(sed -n 's/.*"accessToken":"\([^"]*\)".*/\1/p' /tmp/aaax-tok.json | head -1)"
fi
if [[ -z "$TOKEN" || "$TOKEN" == "null" ]]; then
  echo "FAIL — no data.accessToken"
  exit 1
fi

echo "== GET /users/me =="
code=$(curl -sS -o /tmp/aaax-me.json -w "%{http_code}" \
  -H "Authorization: Bearer ${TOKEN}" \
  "${BASE}/users/me")
echo "HTTP $code (expect 200)"
command -v jq >/dev/null && jq . /tmp/aaax-me.json || cat /tmp/aaax-me.json
echo
