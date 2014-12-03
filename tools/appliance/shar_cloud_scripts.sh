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

# since veewee wants .sh files to execute, we'll give it a shar

set -e
set -x

# where we are running this script from
CURR_DIR=${PWD}
# where this script is
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
# where cloudstack is checked out
cd ${SCRIPT_DIR}/../..
CLOUDSTACK_DIR=${PWD}
cd ${CURR_DIR}
# ensure we are running in isolation
TMPDIR=${TMPDIR:-/tmp}
TMPDIR=${TMPDIR%/}
TEMP_DIR=`mktemp -d ${TMPDIR}/shar_cloud.XXXXXXXX`

cd ${TEMP_DIR}
mkdir cloud_scripts
mkdir -p cloud_scripts/opt/cloudstack
cp -r ${CLOUDSTACK_DIR}/systemvm/patches/debian/config/* cloud_scripts/
cp -r ${CLOUDSTACK_DIR}/systemvm/patches/debian/vpn/* cloud_scripts/

mkdir -p cloud_scripts/usr/share/cloud
cd ${CLOUDSTACK_DIR}/systemvm/patches/debian/config
tar -cf ${TEMP_DIR}/cloud_scripts/usr/share/cloud/cloud-scripts.tar *
cd ${CLOUDSTACK_DIR}/systemvm/patches/debian/vpn
tar -rf ${TEMP_DIR}/cloud_scripts/usr/share/cloud/cloud-scripts.tar *

cd ${TEMP_DIR}
shar `find . -print` > ${CURR_DIR}/cloud_scripts_shar_archive.sh

cd ${CURR_DIR}
rm -rf ${TEMP_DIR}
chmod +x cloud_scripts_shar_archive.sh
echo cloud_scripts are in cloud_scripts_shar_archive.sh
