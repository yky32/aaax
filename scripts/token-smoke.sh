#!/usr/bin/env bash
# POST /oauth2/token (custom-password-grant). Exit 0 only when the body has a token.
# Empty DB does NOT seed a client or user — set env (see README Five minutes).
set -euo pipefail
BASE="${AAAX_BASE:-http://localhost:8081}"
CLIENT_ID="${AAAX_CLIENT_ID:-}"
CLIENT_SECRET="${AAAX_CLIENT_SECRET:-}"
USERNAME="${AAAX_USERNAME:-}"
CREDENTIALS="${AAAX_CREDENTIALS:-}"

usage() {
  cat <<'EOF'
AAAX token smoke — needs a JDBC registered client + user already in the AS.

This tree does not seed either on first boot. Export:

  export AAAX_CLIENT_ID=...
  export AAAX_CLIENT_SECRET=...
  export AAAX_USERNAME=...
  export AAAX_CREDENTIALS=...

Then:

  curl -sS -u "$AAAX_CLIENT_ID:$AAAX_CLIENT_SECRET" \
    -X POST "${AAAX_BASE:-http://localhost:8081}/oauth2/token" \
    -H 'content-type: application/x-www-form-urlencoded' \
    -d 'grant_type=custom-password-grant' \
    -d "username=$AAAX_USERNAME" \
    -d "credentials=$AAAX_CREDENTIALS"

Expect R envelope: data.accessToken (camelCase). Test fixtures (not auto-inserted):
  LoginSmokeAccounts — client/secret + uaa.smoke.primary@aaax.local / SmokePrimary!1
EOF
}

if [[ -z "$CLIENT_ID" || -z "$CLIENT_SECRET" || -z "$USERNAME" || -z "$CREDENTIALS" ]]; then
  usage
  exit 1
fi

echo "== POST /oauth2/token (custom-password-grant) =="
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
# Live AS wraps Jwt as data.accessToken. Also accept RFC access_token.
if grep -Eq '"accessToken"|"access_token"' "$body"; then
  echo "OK — token present"
  exit 0
fi
echo "FAIL — no accessToken / access_token in body"
exit 1
