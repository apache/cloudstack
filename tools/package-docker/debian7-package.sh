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
# Build Debian packages
#
# 1) Install docker
# 2) Call "debian7-prepare.sh" to build container (or pull it from external source)
# 3) Call this script to build packages
# 4) Upload or install the binaries stored in debian/out
#
set -e

PACKAGE_DIR=$(cd $(dirname "${BASH_SOURCE[0]}") && pwd)
CONTAINER_TAG="cloudstack/package-debian"
OUT_DIR="debian/out"

# Must run this from source root since we we are using it in the build.
cd "${PACKAGE_DIR}/../.."
SOURCE_ROOT=$(pwd)

# Debian package files are by default in the parent directory of the build folder.
# Files will be copied to this location after build.
mkdir -p ${OUT_DIR}
rm -f ${OUT_DIR}/*

DOCKER_VOLUMES="-v ${OUT_DIR}:/build -v ${SOURCE_ROOT}:/build/cloudstack"
DOCKER_RUN="tools/package-docker/build-packages.sh"

if [ -z "${DEBIAN7_DOCKER_PACKAGE_DEBUG}" ] ; then
    docker run --rm ${DOCKER_VOLUMES} "${CONTAINER_TAG}" "${DOCKER_RUN}" $*
else
    echo "Would run this in container:"
    echo "${DOCKER_RUN} $*"
    docker run -it ${DOCKER_VOLUMES} "${CONTAINER_TAG}" bash
fi
