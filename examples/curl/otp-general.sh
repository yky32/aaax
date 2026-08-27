#!/usr/bin/env bash
# POST /authentications/one-time-passwords/general
set -euo pipefail
BASE="${AAAX_BASE:-http://localhost:8081}"
TO="${1:-user@example.com}"

echo "== POST /authentications/one-time-passwords/general $TO =="
code=$(curl -sS -o /tmp/aaax-otp.json -w "%{http_code}" -X POST "$BASE/authentications/one-time-passwords/general" \
  -H 'content-type: application/json' \
  -d "{\"to\":\"$TO\",\"usecase\":\"OTP_GENERAL\",\"type\":\"DIGIT\"}")
echo "HTTP $code (expect 200)"
command -v jq >/dev/null && jq . /tmp/aaax-otp.json || cat /tmp/aaax-otp.json
echo
