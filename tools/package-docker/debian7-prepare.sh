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
# Build Docker container that can be used to build Debian packages
# You need only to do build it once.
#
# 1) Install docker
# 2) Call this script to build container (or pull it from external source)
# 3) Call "debian7-package.sh" to build packages
#
set -e

PACKAGE_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
CONTAINER_TAG="cloudstack/package-debian"

cd "${PACKAGE_DIR}"

docker build -t "${CONTAINER_TAG}" -f Dockerfile.debian7 .
