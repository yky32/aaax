#!/usr/bin/env bash
# Get AAAX token → call this resource server /api/hello
set -euo pipefail
AAAX_BASE="${AAAX_BASE:-http://localhost:8081}"
RS_BASE="${RS_BASE:-http://localhost:8082}"
CLIENT_ID="${AAAX_CLIENT_ID:-aaax-demo}"
CLIENT_SECRET="${AAAX_CLIENT_SECRET:-aaax-demo-secret}"

echo "== AAAX health =="
curl -sf "$AAAX_BASE/actuator/health" | (command -v jq >/dev/null && jq . || cat)
echo

echo "== RS health =="
curl -sf "$RS_BASE/actuator/health" | (command -v jq >/dev/null && jq . || cat)
echo

echo "== token =="
TOKEN=$(curl -sf -u "$CLIENT_ID:$CLIENT_SECRET" \
  -X POST "$AAAX_BASE/oauth2/token" \
  -d 'grant_type=client_credentials&scope=api.read' | \
  (command -v jq >/dev/null && jq -r .access_token || python3 -c 'import sys,json;print(json.load(sys.stdin)["access_token"])'))
echo "token_len=${#TOKEN}"

echo "== GET $RS_BASE/api/hello =="
curl -sf "$RS_BASE/api/hello" -H "Authorization: Bearer $TOKEN" | (command -v jq >/dev/null && jq . || cat)
echo
