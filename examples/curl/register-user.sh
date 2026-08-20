#!/usr/bin/env bash
# Register a user via preferred AAAX API
set -euo pipefail
BASE="${AAAX_BASE:-http://localhost:8081}"
USER="${1:-devuser}"
EMAIL="${2:-devuser@example.com}"
PASS="${3:-password123}"

curl -sf -X POST "$BASE/v1/accounts/register" \
  -H 'content-type: application/json' \
  -d "{\"username\":\"$USER\",\"email\":\"$EMAIL\",\"password\":\"$PASS\"}" \
  | (command -v jq >/dev/null && jq . || cat)
echo
echo "OK registered $USER (also available: POST /users/registrations compat path)"
