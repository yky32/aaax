#!/usr/bin/env bash
# Register -> password login -> GET /me (expected: 201, 200, 200)
set -euo pipefail
BASE="${AAAX_BASE:-http://localhost:8081}"
USER="${1:-devuser$RANDOM}"
EMAIL="${2:-$USER@example.com}"
PASS="${3:-password123}"
CJ="${TMPDIR:-/tmp}/aaax-reg-login.cj"
rm -f "$CJ"
echo "== register $USER =="
code=$(curl -sS -o /tmp/aaax-reg.json -w "%{http_code}" -X POST "$BASE/v1/accounts/register" \
  -H 'content-type: application/json' \
  -d "{\"username\":\"$USER\",\"email\":\"$EMAIL\",\"password\":\"$PASS\"}")
echo "HTTP $code (expect 201)"
command -v jq >/dev/null && jq . /tmp/aaax-reg.json || cat /tmp/aaax-reg.json
echo
echo "== password login =="
code=$(curl -sS -c "$CJ" -o /tmp/aaax-login.json -w "%{http_code}" -X POST "$BASE/v1/auth/login" \
  -H 'content-type: application/json' \
  -d "{\"username\":\"$USER\",\"password\":\"$PASS\"}")
echo "HTTP $code (expect 200)"
command -v jq >/dev/null && jq . /tmp/aaax-login.json || cat /tmp/aaax-login.json
echo
echo "== GET /v1/accounts/me =="
code=$(curl -sS -b "$CJ" -o /tmp/aaax-me.json -w "%{http_code}" "$BASE/v1/accounts/me")
echo "HTTP $code (expect 200)"
command -v jq >/dev/null && jq . /tmp/aaax-me.json || cat /tmp/aaax-me.json
echo
