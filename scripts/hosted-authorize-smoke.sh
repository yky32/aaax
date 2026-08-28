#!/usr/bin/env bash
# Hosted /login + PKCE authorize → loopback code → public-client token (RFC 7636 appendix verifier).
# Loopback other-port (RFC 8252 §7.3): SAS matches 127.0.0.1 except port.
# Not claimed HTTPS / app-store native-app path.
set -euo pipefail
BASE="${AAAX_BASE:-http://localhost:8081}"
CLIENT="${AAAX_PKCE_CLIENT_ID:-aaax-pkce}"
REDIRECT="${AAAX_PKCE_REDIRECT_URI:-http://127.0.0.1:8081/authorized}"
LOOPBACK_OTHER="${AAAX_PKCE_LOOPBACK_OTHER:-http://127.0.0.1:9/authorized}"
USER="${AAAX_SMOKE_USER:-smoke.primary@aaax.local}"
PASS="${AAAX_SMOKE_PASSWORD:-SmokePrimary!1}"
# RFC 7636 appendix B
CHALLENGE="E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM"
VERIFIER="dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk"
AUTH_URL="${BASE}/oauth2/authorize?response_type=code&client_id=${CLIENT}&redirect_uri=${REDIRECT}&scope=openid&code_challenge=${CHALLENGE}&code_challenge_method=S256"

login_html=$(curl -sf "$BASE/login" || true)
if ! printf '%s' "$login_html" | grep -qi 'password'; then
  echo "FAIL: GET /login is not a hosted login page" >&2
  printf '%s\n' "$login_html" | head -c 400 >&2
  exit 1
fi
echo "OK: GET /login"

loc=$(curl -sS -o /dev/null -w '%{redirect_url}' -H 'Accept: text/html' "$AUTH_URL")
if ! printf '%s' "$loc" | grep -q '/login'; then
  echo "FAIL: HTML authorize with PKCE did not redirect to /login (Location=${loc})" >&2
  exit 1
fi
echo "OK: authorize redirects to /login"

COOKIE=$(mktemp)
token_body=$(mktemp)
trap 'rm -f "$COOKIE" "$token_body"' EXIT

curl -sS -c "$COOKIE" -b "$COOKIE" -H 'Accept: text/html' -o /dev/null "$AUTH_URL"
html=$(curl -sS -c "$COOKIE" -b "$COOKIE" "$BASE/login")
csrf=$(printf '%s' "$html" | sed -n 's/.*name="_csrf"[^>]*value="\([^"]*\)".*/\1/p' | head -1)
if [ -z "$csrf" ]; then
  csrf=$(printf '%s' "$html" | sed -n 's/.*value="\([^"]*\)"[^>]*name="_csrf".*/\1/p' | head -1)
fi
if [ -z "$csrf" ]; then
  echo "FAIL: no _csrf on /login" >&2
  exit 1
fi

curl -sS -c "$COOKIE" -b "$COOKIE" -o /tmp/aaax-login-post.body -D /tmp/aaax-login-post.hdr \
  -X POST "$BASE/login" \
  --data-urlencode "username=${USER}" \
  --data-urlencode "password=${PASS}" \
  --data-urlencode "_csrf=${csrf}"

other_loc=$(curl -sS -o /dev/null -w '%{redirect_url}' -c "$COOKIE" -b "$COOKIE" -H 'Accept: text/html' -G \
  "$BASE/oauth2/authorize" \
  --data-urlencode 'response_type=code' \
  --data-urlencode "client_id=${CLIENT}" \
  --data-urlencode "redirect_uri=${LOOPBACK_OTHER}" \
  --data-urlencode 'scope=openid' \
  --data-urlencode "code_challenge=${CHALLENGE}" \
  --data-urlencode 'code_challenge_method=S256')
if printf '%s' "$other_loc" | grep -qi 'invalid.*redirect'; then
  echo "FAIL: loopback other-port treated as invalid redirect_uri (Location=${other_loc})" >&2
  exit 1
fi
if ! printf '%s' "$other_loc" | grep -q '127.0.0.1:9' || ! printf '%s' "$other_loc" | grep -q 'code='; then
  echo "FAIL: loopback other-port did not return a code on :9 (Location=${other_loc})" >&2
  exit 1
fi
echo "OK: loopback other-port redirect (${other_loc%%&*})"

other_code=$(printf '%s' "$other_loc" | sed -n 's/.*[?&]code=\([^&]*\).*/\1/p')
if [ -z "$other_code" ]; then
  echo "FAIL: could not parse code from other-port Location" >&2
  exit 1
fi

token_code=$(curl -sS -o "$token_body" -w '%{http_code}' \
  -X POST "$BASE/oauth2/token" \
  -H 'content-type: application/x-www-form-urlencoded' \
  --data-urlencode 'grant_type=authorization_code' \
  --data-urlencode "code=${other_code}" \
  --data-urlencode "redirect_uri=${LOOPBACK_OTHER}" \
  --data-urlencode "client_id=${CLIENT}" \
  --data-urlencode "code_verifier=${VERIFIER}")
if [ "$token_code" != "200" ] || ! grep -q '"access_token"' "$token_body"; then
  echo "FAIL: public-client token exchange (HTTP ${token_code})" >&2
  cat "$token_body" >&2
  exit 1
fi
echo "OK: authorization_code + code_verifier minted access_token (public client, loopback other-port)"

final_url=$(curl -sS -c "$COOKIE" -b "$COOKIE" -o /tmp/aaax-authorized.html -w '%{url_effective}' \
  -L -H 'Accept: text/html' "$AUTH_URL")
if ! printf '%s' "$final_url" | grep -q 'code='; then
  if grep -q 'authorization_code' /tmp/aaax-authorized.html 2>/dev/null; then
    echo "OK: loopback page shows authorization_code"
    exit 0
  fi
  echo "FAIL: no authorization code after hosted login (url=${final_url})" >&2
  echo '--- login POST headers ---' >&2
  cat /tmp/aaax-login-post.hdr >&2 || true
  echo '--- authorized body ---' >&2
  head -c 600 /tmp/aaax-authorized.html >&2 || true
  exit 1
fi
echo "OK: hosted login completed authorization_code (${final_url%%&*})"
