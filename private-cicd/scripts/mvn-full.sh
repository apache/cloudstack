#!/usr/bin/env bash
# Full CloudStack Maven build (Phase 1). Mirrors private-cicd/Jenkinsfile build-only stage.

set -euo pipefail

CLOUDSTACK_DIR="${CLOUDSTACK_DIR:?Set CLOUDSTACK_DIR to the CloudStack repo root}"

SKIP_TESTS="${SKIP_TESTS:-false}"
ENABLE_NOREDIST="${ENABLE_NOREDIST:-false}"
THREADS="${MAVEN_THREADS:-$(nproc)}"

skip_flag=""
if [[ "$SKIP_TESTS" == "true" ]]; then
  skip_flag="-DskipTests=true"
fi

noredist_flag=""
if [[ "$ENABLE_NOREDIST" == "true" ]]; then
  noredist_flag="-Dnoredist"
fi

echo "==> Full build in $CLOUDSTACK_DIR"
cd "$CLOUDSTACK_DIR"

exec mvn -B -P developer,systemvm -Dsimulator \
  ${noredist_flag} \
  clean install \
  ${skip_flag} \
  -T"${THREADS}"
