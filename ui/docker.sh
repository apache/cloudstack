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

set -e
set -x

cd $(dirname $0)

GIT_TAG="$(git tag --points-at | head -n 1)"
if [ -n "${GIT_REV}" ]; then
	LABEL_GIT_TAG="--label \"org.opencontainers.image.version=${GIT_TAG}\""
fi
DATE="$(date --iso-8601=seconds)"
LABEL_DATE="--label \"org.opencontainers.image.created=${DATE}\""
GIT_REV="$(git rev-parse HEAD)"
LABEL_GIT_REV="--label \"org.opencontainers.image.revision=${GIT_REV}\""

docker build -t cloudstack-ui ${LABEL_DATE} ${LABEL_GIT_REV} ${LABEL_GIT_TAG} .
