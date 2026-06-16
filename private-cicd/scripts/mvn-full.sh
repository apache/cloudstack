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
