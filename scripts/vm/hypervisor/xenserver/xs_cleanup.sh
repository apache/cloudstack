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

#set -x
 
usage() {
  printf "Usage: %s \n" $(basename $0) >&2

}

remove_device() {
  be=$1
  xenstore-write /local/domain/0/backend/tap/0/$be/online 0 &>/dev/null
  xenstore-write /local/domain/0/backend/tap/0/$be/shutdown-request normal &>/dev/null
  for i in $(seq 20)
  do
    sleep 1
    xenstore-exists /local/domain/0/backend/tap/0/$be/shutdown-done &>/dev/null
    if [ $? -eq 0 ] ; then
      xenstore-rm /local/domain/0/device/vbd/$be &>/dev/null
      xenstore-rm /local/domain/0/backend/tap/0/$be &>/dev/null
      xenstore-rm /local/domain/0/error/backend/tap/0/$be &>/dev/null
      xenstore-rm /local/domain/0/error/device/vbd/$be &>/dev/null
      return
    fi
    xenstore-exists /local/domain/0/backend/tap/0/$be &>/dev/null
    if [ $? -ne 0 ] ; then
      return
    fi
  done

  echo "unplug device $be failed"
  exit 2
}

bes=`xenstore-list /local/domain/0/backend/tap/0`

if [ -z "$bes" ]; then
  exit 0
fi

for be in $bes
do
  device=`xenstore-read /local/domain/0/backend/tap/0/$be/dev`
  ls $device >/dev/null 2>&1 
  if [ $? -ne 0 ]; then
    remove_device $be
  fi
done


echo "======> DONE <======"
exit 0

