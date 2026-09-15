#!/usr/bin/env bash
# POST /oauth2/token grant_type=refresh_token after custom-password-grant.
# Defaults match AAAX_LOCAL_SEED (client/secret + smoke.primary@aaax.local).
set -euo pipefail
BASE="${AAAX_BASE:-http://localhost:8081}"
CLIENT_ID="${AAAX_CLIENT_ID:-client}"
CLIENT_SECRET="${AAAX_CLIENT_SECRET:-secret}"
USERNAME="${AAAX_USERNAME:-smoke.primary@aaax.local}"
CREDENTIALS="${AAAX_CREDENTIALS:-SmokePrimary!1}"

extract_json_string() {
  local key="$1" file="$2"
  grep -o "\"${key}\"[[:space:]]*:[[:space:]]*\"[^\"]*\"" "$file" \
    | head -1 \
    | sed 's/.*"\([^"]*\)"$/\1/'
}

login_body="$(mktemp)"
refresh_body="$(mktemp)"
trap 'rm -f "$login_body" "$refresh_body"' EXIT

echo "== POST /oauth2/token (custom-password-grant) as ${USERNAME} =="
login_code="$(curl -sS -o "$login_body" -w '%{http_code}' \
  -u "${CLIENT_ID}:${CLIENT_SECRET}" \
  -X POST "${BASE}/oauth2/token" \
  -H 'content-type: application/x-www-form-urlencoded' \
  -d 'grant_type=custom-password-grant' \
  -d "username=${USERNAME}" \
  -d "credentials=${CREDENTIALS}")"
if [ "$login_code" != "200" ]; then
  echo "FAIL: password grant HTTP ${login_code}" >&2
  cat "$login_body" >&2
  exit 1
fi

refresh_token="$(extract_json_string refresh_token "$login_body")"
if [ -z "$refresh_token" ]; then
  echo "FAIL: password grant body has no refresh_token" >&2
  cat "$login_body" >&2
  exit 1
fi
echo "OK: password grant returned refresh_token"

echo "== POST /oauth2/token (grant_type=refresh_token) =="
refresh_code="$(curl -sS -o "$refresh_body" -w '%{http_code}' \
  -u "${CLIENT_ID}:${CLIENT_SECRET}" \
  -X POST "${BASE}/oauth2/token" \
  -H 'content-type: application/x-www-form-urlencoded' \
  -d 'grant_type=refresh_token' \
  --data-urlencode "refresh_token=${refresh_token}")"
if [ "$refresh_code" != "200" ]; then
  echo "FAIL: refresh grant HTTP ${refresh_code}" >&2
  cat "$refresh_body" >&2
  exit 1
fi
if ! grep -q '"access_token"' "$refresh_body"; then
  echo "FAIL: refresh response has no access_token" >&2
  cat "$refresh_body" >&2
  exit 1
fi
echo "OK: refresh_token grant minted access_token"
