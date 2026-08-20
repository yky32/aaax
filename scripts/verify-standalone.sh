#!/usr/bin/env bash
# Verify stranger clone path: Central-only Maven + tests.
set -euo pipefail
cd "$(dirname "$0")/.."
echo "== effective settings (must be aaax-central-only) =="
mvn -B -q help:effective-settings | grep -E 'aaax-central-only|repo1.maven.org|quinsic' || true
if mvn -B -q help:effective-settings | grep -q quinsic; then
  echo "FAIL: private quinsic repo still active" >&2
  exit 1
fi
echo "== tests =="
mvn -B test
echo "== dependency tree private scan =="
mvn -B -q dependency:tree -DoutputFile=target/deps.txt
if grep -E 'com\.quinsic|app-core' target/deps.txt; then
  echo "FAIL: private dep" >&2
  exit 1
fi
echo "OK standalone"
