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

OUTPUT_FORMAT=${1:?"Output format is required"}
INPUT_FILE=${2:?"Input file/path is required"}
OUTPUT_FILE=${3:?"Output file/path is required"}

echo "$(date): qemu-img convert -n -p -W -t none -O ${OUTPUT_FORMAT} ${INPUT_FILE} ${OUTPUT_FILE}"

qemu-img convert -n -p -W -t writeback -O ${OUTPUT_FORMAT} ${INPUT_FILE} ${OUTPUT_FILE} && {
   # if its a block device make sure we flush caches before exiting
   lsblk ${OUTPUT_FILE} >/dev/null 2>&1 && {
      blockdev --flushbufs ${OUTPUT_FILE}
      hdparm -F ${OUTPUT_FILE}
   }
   exit 0
}
