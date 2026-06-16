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

# Private downstream build dependencies (Ubuntu/Debian).
# Aligns loosely with Apache CloudStack GitHub Actions build workflow.

set -euo pipefail

if [[ "${EUID:-$(id -u)}" -ne 0 ]]; then
  SUDO="sudo"
else
  SUDO=""
fi

$SUDO apt-get update
$SUDO apt-get install -y \
  git \
  uuid-runtime \
  genisoimage \
  netcat-openbsd \
  ipmitool \
  build-essential \
  libgcrypt20 \
  libgpg-error-dev \
  libgpg-error0 \
  libopenipmi0 \
  libpython3-dev \
  libssl-dev \
  libffi-dev \
  python3-openssl \
  python3-dev \
  python3-setuptools \
  wget \
  unzip
