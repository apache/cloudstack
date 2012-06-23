#!/bin/sh
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

usage() {
  echo "$0 <VM uuid>"
  echo " -- fakes the presence of PV tools in <VM uuid>"
  echo " -- NB the VM must be either paused or running on localhost"
}
 
fake(){
  local uuid=$1
  domid=$(xe vm-list uuid=${uuid} params=dom-id --minimal)
  xenstore-write /local/domain/${domid}/attr/PVAddons/MajorVersion ${major} \
                 /local/domain/${domid}/attr/PVAddons/MinorVersion ${minor} \
                 /local/domain/${domid}/attr/PVAddons/MicroVersion ${micro} \
                 /local/domain/${domid}/data/updated 1
}

VERSION_RE="\([[:digit:]]\{1,3\}\.\)\{2\}[[:digit:]]\{1,3\}"

uuid=$(xe vm-list uuid=$1 params=uuid --minimal)
if [ $? -ne 0 ]; then
  echo "Argument should be a VM uuid"
  usage
  exit 1
fi

# use the INSTALLATION_UUID from here:
. /etc/xensource-inventory

product_version=$(xe host-list params=software-version uuid="$INSTALLATION_UUID" | \
        sed -n -e 's/product_version: \('${VERSION_RE}'\).*/\1/' \
               -e 's/^.*\('${VERSION_RE}'\)/\1/p')

if [ $? -ne 0 ]; then
  echo "Failed to get product version"
  exit 1
fi

major=$(echo $product_version | cut -f 1 -d .)
minor=$(echo $product_version | cut -f 2 -d .)
micro=$(echo $product_version | cut -f 3 -d .)

# Check the VM is running on this host
resident_on=$(xe vm-list uuid=${uuid} params=resident-on --minimal)
if [ "${resident_on}" != "${INSTALLATION_UUID}" ]; then
  echo "VM must be resident on this host"
  exit 2
fi

power_state=$(xe vm-list uuid=${uuid} params=power-state --minimal)
case "${power_state}" in
  running)
    fake $uuid
    ;;
  paused)
    fake $uuid
    ;;
  *)
    echo "VM must be either running or paused"
    exit 3
    ;;
esac


