#!/usr/bin/env bash
# Login as admin → print identity event bus buffer
set -euo pipefail
BASE="${AAAX_BASE:-http://localhost:8081}"
CJ="${TMPDIR:-/tmp}/aaax-admin.cj"
rm -f "$CJ"

curl -sf -c "$CJ" -X POST "$BASE/v1/auth/login" \
  -H 'content-type: application/json' \
  -d '{"username":"admin","password":"admin12345"}' >/dev/null

echo "== settings.identityEventBus =="
curl -sf -b "$CJ" "$BASE/v1/admin/settings" | (command -v jq >/dev/null && jq .identityEventBus || cat)
echo
echo "== recent events =="
curl -sf -b "$CJ" "$BASE/v1/admin/events?limit=20" | (command -v jq >/dev/null && jq . || cat)
echo
