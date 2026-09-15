#!/bin/bash

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
#
# Standalone smoke test for templateConfig.sh's version handling across the
# 4.x (legacy) to 24.x (cutover) versioning schemes. Run directly:
#   bash engine/schema/test_templateConfig.sh

set -u

SCRIPT_DIR="$( cd -- "$(dirname "${BASH_SOURCE[0]}")" >/dev/null 2>&1 ; pwd -P )"
# shellcheck source=templateConfig.sh
source "${SCRIPT_DIR}/templateConfig.sh"

pass=0
fail=0

assert_eq() {
  local description="$1"
  local expected="$2"
  local actual="$3"
  if [[ "$expected" == "$actual" ]]; then
    pass=$((pass + 1))
    echo "PASS: $description"
  else
    fail=$((fail + 1))
    echo "FAIL: $description (expected '$expected', got '$actual')"
  fi
}

# --- getTemplateVersion: legacy versioning (major < 24) ---

getTemplateVersion "4.22.0.0"
assert_eq "legacy: CS_VERSION" "4.22" "$CS_VERSION"
assert_eq "legacy: VERSION" "4.22.0" "$VERSION"
assert_eq "legacy: FULL_VERSION keeps the 4th (security) component" "4.22.0.0" "$FULL_VERSION"

getTemplateVersion "4.23.0.1-SNAPSHOT"
assert_eq "legacy with -SNAPSHOT suffix: VERSION" "4.23.0" "$VERSION"
assert_eq "legacy with -SNAPSHOT suffix: FULL_VERSION" "4.23.0.1" "$FULL_VERSION"

# --- getTemplateVersion: new versioning (major >= 24, post-cutover) ---

getTemplateVersion "24.0.0"
assert_eq "cutover: CS_VERSION" "24.0" "$CS_VERSION"
assert_eq "cutover: VERSION" "24.0.0" "$VERSION"
assert_eq "cutover: FULL_VERSION has no trailing dot (patch dropped)" "24.0.0" "$FULL_VERSION"

getTemplateVersion "24.1.2-SNAPSHOT"
assert_eq "cutover with -SNAPSHOT suffix: VERSION" "24.1.2" "$VERSION"
assert_eq "cutover with -SNAPSHOT suffix: FULL_VERSION" "24.1.2" "$FULL_VERSION"

getTemplateVersion "99.9.9"
assert_eq "future major: VERSION" "99.9.9" "$VERSION"
assert_eq "future major: FULL_VERSION" "99.9.9" "$FULL_VERSION"

# --- createMetadataFile: end-to-end metadata.ini "version" line ---

run_create_metadata_file() {
  local projVersion="$1"
  local workdir
  workdir="$(mktemp -d)"

  getTemplateVersion "$projVersion"
  METADATAFILE="${workdir}/metadata.ini"
  SOURCEFILE="${workdir}/sha512sum.txt"
  printf "abc123  systemvmtemplate-%s-x86_64-kvm.qcow2.bz2\n" "$VERSION" > "$SOURCEFILE"
  templates=("kvm-x86_64:${CS_SYSTEMTEMPLATE_REPO}/${CS_VERSION}/systemvmtemplate-${VERSION}-x86_64-kvm.qcow2.bz2")
  : > "$METADATAFILE"

  createMetadataFile
  grep '^version = ' "$METADATAFILE" | head -1 | cut -d' ' -f3

  rm -rf "$workdir"
}

actual="$(run_create_metadata_file "4.22.0.0")"
assert_eq "metadata.ini legacy 'version' line" "4.22.0.0" "$actual"

actual="$(run_create_metadata_file "24.0.0")"
assert_eq "metadata.ini cutover 'version' line has no trailing dot" "24.0.0" "$actual"

echo ""
echo "${pass} passed, ${fail} failed"
[[ $fail -eq 0 ]]
