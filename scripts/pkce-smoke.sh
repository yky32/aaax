#!/usr/bin/env bash
# Proves seed public client aaax-pkce requires PKCE (RFC 7636).
# SAS validates code_challenge before login. Unauthenticated *with* PKCE → /login.
# Token exchange is hosted-authorize-smoke.sh.
set -euo pipefail
BASE="${AAAX_BASE:-http://localhost:8081}"
CLIENT="${AAAX_PKCE_CLIENT_ID:-aaax-pkce}"
REDIRECT="${AAAX_PKCE_REDIRECT_URI:-http://127.0.0.1:8081/authorized}"
USER="${AAAX_SMOKE_USER:-smoke.primary@aaax.local}"
PASS="${AAAX_SMOKE_PASSWORD:-SmokePrimary!1}"
MISSING_URL="${BASE}/oauth2/authorize?response_type=code&client_id=${CLIENT}&redirect_uri=${REDIRECT}&scope=openid"
CHALLENGE="E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM"
WITH_URL="${MISSING_URL}&code_challenge=${CHALLENGE}&code_challenge_method=S256"

missing_unauth=$(curl -sS -o /dev/null -w '%{redirect_url}' -H 'Accept: text/html' "$MISSING_URL")
if ! printf '%s' "$missing_unauth" | grep -qi 'code_challenge'; then
  echo "FAIL: unauthenticated authorize without PKCE did not mention code_challenge (Location=${missing_unauth})" >&2
  exit 1
fi
echo "OK: missing PKCE rejected before login"

loc=$(curl -sS -o /dev/null -w '%{redirect_url}' -H 'Accept: text/html' "$WITH_URL")
if ! printf '%s' "$loc" | grep -q '/login'; then
  echo "FAIL: unauthenticated authorize with PKCE did not go to /login (Location=${loc})" >&2
  exit 1
fi
echo "OK: unauthenticated authorize with PKCE goes to /login"

COOKIE=$(mktemp)
trap 'rm -f "$COOKIE"' EXIT

html=$(curl -sS -c "$COOKIE" -b "$COOKIE" "$BASE/login")
csrf=$(printf '%s' "$html" | sed -n 's/.*name="_csrf"[^>]*value="\([^"]*\)".*/\1/p' | head -1)
if [ -z "$csrf" ]; then
  csrf=$(printf '%s' "$html" | sed -n 's/.*value="\([^"]*\)"[^>]*name="_csrf".*/\1/p' | head -1)
fi
if [ -z "$csrf" ]; then
  echo "FAIL: no _csrf on /login" >&2
  exit 1
fi
curl -sS -c "$COOKIE" -b "$COOKIE" -o /dev/null \
  -X POST "$BASE/login" \
  --data-urlencode "username=${USER}" \
  --data-urlencode "password=${PASS}" \
  --data-urlencode "_csrf=${csrf}"

missing_loc=$(curl -sS -o /dev/null -w '%{redirect_url}' -c "$COOKIE" -b "$COOKIE" -H 'Accept: text/html' "$MISSING_URL")
if ! printf '%s' "$missing_loc" | grep -qi 'code_challenge'; then
  echo "FAIL: logged-in authorize without PKCE did not mention code_challenge (Location=${missing_loc})" >&2
  exit 1
fi
echo "OK: missing PKCE rejected after login"

with_loc=$(curl -sS -o /dev/null -w '%{redirect_url}' -c "$COOKIE" -b "$COOKIE" -H 'Accept: text/html' "$WITH_URL")
if printf '%s' "$with_loc" | grep -qi 'code_challenge'; then
  echo "FAIL: logged-in authorize with PKCE still complained about code_challenge (Location=${with_loc})" >&2
  exit 1
fi
if ! printf '%s' "$with_loc" | grep -q 'code='; then
  echo "FAIL: logged-in authorize with PKCE did not return a code (Location=${with_loc})" >&2
  exit 1
fi
echo "OK: PKCE present is accepted after login"
