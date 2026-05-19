#!/usr/bin/env bash
# Compile and run JUnit for the ONTAP volume plugin only (-pl -am). Downstream CI only.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CICD_ROOT="${CICD_ROOT:-$(cd "$SCRIPT_DIR/.." && pwd)}"
CLOUDSTACK_DIR="${CLOUDSTACK_DIR:-$(cd "$CICD_ROOT/.." && pwd)}"

if [[ ! -f "$CLOUDSTACK_DIR/pom.xml" ]]; then
  echo "CLOUDSTACK_DIR must contain pom.xml (got: $CLOUDSTACK_DIR)" >&2
  exit 1
fi

SKIP_TESTS="${SKIP_TESTS:-false}"
EXTRA="${MAVEN_EXTRA_ARGS:-}"

skip_flag=""
if [[ "$SKIP_TESTS" == "true" ]]; then
  skip_flag="-DskipTests=true"
fi

echo "==> ONTAP fast build in $CLOUDSTACK_DIR"
cd "$CLOUDSTACK_DIR"

# Step 1: compile/install plugin dependencies (-am) without running their tests.
# Using "mvn … -am test" would run the entire upstream reactor (e.g. engine/schema
# SystemVmTemplateRegistrationTest), which can invoke "sudo mount" and hang on Password:
echo "==> Step 1/2: install dependencies (skipTests)"
mvn -B -P developer \
  -pl :cloud-plugin-storage-volume-ontap -am \
  -DskipTests=true \
  $EXTRA \
  install

if [[ "$SKIP_TESTS" == "true" ]]; then
  echo "==> Step 2/2: skipped (SKIP_TESTS=true)"
  exit 0
fi

# Step 2: run tests only on the ONTAP plugin module (no -am).
echo "==> Step 2/2: ONTAP plugin tests only"
exec mvn -B -P developer \
  -pl :cloud-plugin-storage-volume-ontap \
  $EXTRA \
  test
