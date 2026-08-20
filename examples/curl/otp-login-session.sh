#!/usr/bin/env bash
# Request OTP + passwordless login (console channel: read code from server logs)
set -euo pipefail
BASE="${AAAX_BASE:-http://localhost:8081}"
USER="${1:-demo}"
CJ="${TMPDIR:-/tmp}/aaax-otp.cj"
rm -f "$CJ"

echo "== request OTP for $USER =="
curl -sf -c "$CJ" -X POST "$BASE/v1/otp/request" \
  -H 'content-type: application/json' \
  -d "{\"username\":\"$USER\"}" | (command -v jq >/dev/null && jq . || cat)
echo
echo ">> Look at aaax-server logs for: AAAX OTP for ... => CODE"
read -r -p "Paste OTP code: " CODE

echo "== otp login =="
curl -sf -c "$CJ" -b "$CJ" -X POST "$BASE/v1/auth/otp/login" \
  -H 'content-type: application/json' \
  -d "{\"username\":\"$USER\",\"code\":\"$CODE\"}" | (command -v jq >/dev/null && jq . || cat)
echo

echo "== /v1/accounts/me =="
curl -sf -b "$CJ" "$BASE/v1/accounts/me" | (command -v jq >/dev/null && jq . || cat)
echo
echo "cookie jar: $CJ"
