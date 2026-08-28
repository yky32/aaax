#!/usr/bin/env bash
# Proves seed public client aaax-pkce requires PKCE (RFC 7636). Hosted login is scripts/hosted-authorize-smoke.sh.
set -euo pipefail
BASE="${AAAX_BASE:-http://localhost:8081}"
CLIENT="${AAAX_PKCE_CLIENT_ID:-aaax-pkce}"
REDIRECT="${AAAX_PKCE_REDIRECT_URI:-http://127.0.0.1:8081/authorized}"

missing=$(curl -sS -o /tmp/aaax-pkce-missing.txt -w '%{http_code}' \
  "$BASE/oauth2/authorize?response_type=code&client_id=${CLIENT}&redirect_uri=${REDIRECT}&scope=openid")
if ! grep -qi 'code_challenge' /tmp/aaax-pkce-missing.txt; then
  echo "FAIL: authorize without PKCE did not mention code_challenge (HTTP ${missing})" >&2
  cat /tmp/aaax-pkce-missing.txt >&2
  exit 1
fi
echo "OK: missing PKCE rejected (HTTP ${missing})"

# RFC 7636 example S256 challenge (does not need the verifier for this check)
with=$(curl -sS -o /tmp/aaax-pkce-with.txt -w '%{http_code}' \
  "$BASE/oauth2/authorize?response_type=code&client_id=${CLIENT}&redirect_uri=${REDIRECT}&scope=openid&code_challenge=E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM&code_challenge_method=S256")
if grep -qi 'code_challenge' /tmp/aaax-pkce-with.txt; then
  echo "FAIL: authorize with PKCE still complained about code_challenge (HTTP ${with})" >&2
  cat /tmp/aaax-pkce-with.txt >&2
  exit 1
fi
echo "OK: PKCE present is accepted at authorize (HTTP ${with}; login may follow — see hosted-authorize-smoke.sh)"
