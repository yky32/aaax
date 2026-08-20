#!/usr/bin/env bash
# Run Maven for this example with repo-root .mvn/settings.xml (Shibboleth not needed here,
# but root maven.config points at a relative settings path that breaks in subdirs).
set -euo pipefail
DIR="$(cd "$(dirname "$0")" && pwd)"
ROOT="$(cd "$DIR/../.." && pwd)"
export JAVA_HOME="${JAVA_HOME:-/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home}"
export PATH="${JAVA_HOME}/bin:${PATH}"
exec mvn -s "$ROOT/.mvn/settings.xml" "$@"
