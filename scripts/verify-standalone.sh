#!/usr/bin/env bash
# Verify stranger clone path: JDK21 + Central-only Maven + tests.
set -euo pipefail
cd "$(dirname "$0")/.."

if ! java -version 2>&1 | head -1 | grep -qE '"2[1-9]|"[3-9][0-9]'; then
  echo "WARN: prefer JDK 21+. Current:" >&2
  java -version 2>&1 | head -1 || true
fi

echo "== effective settings (must be aaax-central-only) =="
mvn -B -q help:effective-settings | grep -E 'aaax-central-only|repo1.maven.org|quinsic' || true
if mvn -B -q help:effective-settings | grep -q quinsic; then
  echo "FAIL: private quinsic repo still active" >&2
  exit 1
fi

echo "== enforcer + tests =="
mvn -B clean test

echo "== dependency tree private scan =="
mvn -B -q dependency:tree -DoutputFile=target/deps.txt
if grep -E 'com\.quinsic|app-core' target/deps.txt; then
  echo "FAIL: private dep" >&2
  exit 1
fi
echo "OK standalone (Boot 4.1 / JDK 21 baseline)"
