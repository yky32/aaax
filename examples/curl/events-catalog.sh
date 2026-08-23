#!/usr/bin/env bash
# Admin login -> identity events catalog (expected: 200, 200)
set -euo pipefail
BASE="${AAAX_BASE:-http://localhost:8081}"
CJ="${TMPDIR:-/tmp}/aaax-events.cj"
rm -f "$CJ"
echo "== admin login =="
code=$(curl -sS -c "$CJ" -o /tmp/aaax-admin-login.json -w "%{http_code}" -X POST "$BASE/v1/auth/login" \
  -H 'content-type: application/json' \
  -d '{"username":"admin","password":"admin12345"}')
echo "HTTP $code (expect 200)"
echo "== GET /v1/admin/events =="
code=$(curl -sS -b "$CJ" -o /tmp/aaax-events.json -w "%{http_code}" "$BASE/v1/admin/events?limit=20")
echo "HTTP $code (expect 200)"
command -v jq >/dev/null && jq . /tmp/aaax-events.json || cat /tmp/aaax-events.json
echo
