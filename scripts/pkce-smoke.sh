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

dump_auth() {
  local label="$1" hdr="$2" body="$3" code="$4"
  echo "${label}: HTTP ${code}" >&2
  echo '--- headers ---' >&2
  cat "$hdr" >&2 || true
  echo '--- body ---' >&2
  head -c 400 "$body" >&2 || true
  echo >&2
}

loc_from() {
  grep -i '^location:' "$1" | awk '{print $2}' | tr -d '\r' | head -1
}

missing_hdr=$(mktemp)
missing_body=$(mktemp)
missing_code=$(curl -sS -D "$missing_hdr" -o "$missing_body" -w '%{http_code}' -H 'Accept: text/html' "$MISSING_URL")
missing_loc=$(loc_from "$missing_hdr")
if ! printf '%s\n%s' "$missing_loc" "$(cat "$missing_body")" | grep -qi 'code_challenge\|invalid_request'; then
  echo "FAIL: unauthenticated authorize without PKCE did not mention code_challenge (HTTP ${missing_code} Location=${missing_loc})" >&2
  dump_auth 'unauth no PKCE' "$missing_hdr" "$missing_body" "$missing_code"
  exit 1
fi
echo "OK: missing PKCE rejected before login"

with_hdr=$(mktemp)
with_body=$(mktemp)
with_code=$(curl -sS -D "$with_hdr" -o "$with_body" -w '%{http_code}' -H 'Accept: text/html' "$WITH_URL")
with_loc=$(loc_from "$with_hdr")
if ! printf '%s\n%s' "$with_loc" "$(cat "$with_body")" | grep -q '/login'; then
  echo "FAIL: unauthenticated authorize with PKCE did not go to /login (HTTP ${with_code} Location=${with_loc})" >&2
  dump_auth 'unauth with PKCE' "$with_hdr" "$with_body" "$with_code"
  exit 1
fi
echo "OK: unauthenticated authorize with PKCE goes to /login"

COOKIE=$(mktemp)
trap 'rm -f "$COOKIE" "$missing_hdr" "$missing_body" "$with_hdr" "$with_body"' EXIT

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

login_missing_hdr=$(mktemp)
login_missing_body=$(mktemp)
missing_code2=$(curl -sS -D "$login_missing_hdr" -o "$login_missing_body" -w '%{http_code}' -c "$COOKIE" -b "$COOKIE" -H 'Accept: text/html' "$MISSING_URL")
missing_loc=$(loc_from "$login_missing_hdr")
if ! printf '%s\n%s' "$missing_loc" "$(cat "$login_missing_body")" | grep -qi 'code_challenge\|invalid_request'; then
  echo "FAIL: logged-in authorize without PKCE did not mention code_challenge (HTTP ${missing_code2} Location=${missing_loc})" >&2
  dump_auth 'login no PKCE' "$login_missing_hdr" "$login_missing_body" "$missing_code2"
  exit 1
fi
echo "OK: missing PKCE rejected after login"

login_with_hdr=$(mktemp)
login_with_body=$(mktemp)
with_code2=$(curl -sS -D "$login_with_hdr" -o "$login_with_body" -w '%{http_code}' -c "$COOKIE" -b "$COOKIE" -H 'Accept: text/html' "$WITH_URL")
with_loc=$(loc_from "$login_with_hdr")
if printf '%s\n%s' "$with_loc" "$(cat "$login_with_body")" | grep -qi 'code_challenge'; then
  echo "FAIL: logged-in authorize with PKCE still complained about code_challenge (HTTP ${with_code2} Location=${with_loc})" >&2
  dump_auth 'login with PKCE' "$login_with_hdr" "$login_with_body" "$with_code2"
  exit 1
fi
if ! printf '%s' "$with_loc" | grep -q 'code='; then
  echo "FAIL: logged-in authorize with PKCE did not return a code (HTTP ${with_code2} Location=${with_loc})" >&2
  dump_auth 'login with PKCE' "$login_with_hdr" "$login_with_body" "$with_code2"
  exit 1
fi
echo "OK: PKCE present is accepted after login"
