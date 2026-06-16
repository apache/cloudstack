#!/usr/bin/env bash
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.

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
