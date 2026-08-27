#!/usr/bin/env bash
# POST /users/registrations → (OTP from logs) → /users/verifications → /users
set -euo pipefail
BASE="${AAAX_BASE:-http://localhost:8081}"
USER="${1:-user@example.com}"
PASS="${2:-Password1!}"
CODE="${3:-}"

echo "== POST /users/registrations $USER =="
code=$(curl -sS -o /tmp/aaax-reg.json -w "%{http_code}" -X POST "$BASE/users/registrations" \
  -H 'content-type: application/json' \
  -d "{\"username\":\"$USER\",\"credentials\":\"$PASS\"}")
echo "HTTP $code (expect 200, or 409 if taken)"
command -v jq >/dev/null && jq . /tmp/aaax-reg.json || cat /tmp/aaax-reg.json
echo

if [[ -z "$CODE" ]]; then
  echo "Pass OTP as arg 3 after reading the local OTP log, then re-run:"
  echo "  $0 $USER '$PASS' <code>"
  exit 0
fi

echo "== POST /users/verifications =="
code=$(curl -sS -o /tmp/aaax-ver.json -w "%{http_code}" -X POST "$BASE/users/verifications" \
  -H 'content-type: application/json' \
  -d "{\"username\":\"$USER\",\"code\":\"$CODE\"}")
echo "HTTP $code (expect 200)"
command -v jq >/dev/null && jq . /tmp/aaax-ver.json || cat /tmp/aaax-ver.json
echo

echo "== POST /users =="
code=$(curl -sS -o /tmp/aaax-user.json -w "%{http_code}" -X POST "$BASE/users" \
  -H 'content-type: application/json' \
  -d "{\"username\":\"$USER\",\"credentials\":\"$PASS\"}")
echo "HTTP $code (expect 200)"
command -v jq >/dev/null && jq . /tmp/aaax-user.json || cat /tmp/aaax-user.json
echo
