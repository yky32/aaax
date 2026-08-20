#!/usr/bin/env bash
# Client-credentials token → GET /v1/api/hello
set -euo pipefail
BASE="${AAAX_BASE:-http://localhost:8081}"
CLIENT_ID="${AAAX_CLIENT_ID:-aaax-demo}"
CLIENT_SECRET="${AAAX_CLIENT_SECRET:-aaax-demo-secret}"

echo "== health =="
curl -sf "$BASE/actuator/health" | (command -v jq >/dev/null && jq . || cat)
echo

echo "== token (client_credentials) =="
TOKEN=$(curl -sf -u "$CLIENT_ID:$CLIENT_SECRET" \
  -X POST "$BASE/oauth2/token" \
  -d 'grant_type=client_credentials&scope=api.read' | tee /dev/stderr | \
  (command -v jq >/dev/null && jq -r .access_token || python3 -c 'import sys,json;print(json.load(sys.stdin)["access_token"])'))
echo
echo "token_len=${#TOKEN}"

echo "== GET /v1/api/hello =="
curl -sf "$BASE/v1/api/hello" -H "Authorization: Bearer $TOKEN" | (command -v jq >/dev/null && jq . || cat)
echo
