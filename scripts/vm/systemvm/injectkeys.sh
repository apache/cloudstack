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


# Copies keys that enable SSH communication with system vms
# $1 = new private key

#set -x
set -e

TMP=/tmp
umask 022

copy_priv_key() {
  local newprivkey=$1
  diff -q $newprivkey $(dirname $0)/id_rsa.cloud && return 0
  $SUDO cp -fb $newprivkey $(dirname $0)/id_rsa.cloud
  $SUDO chmod 644 $(dirname $0)/id_rsa.cloud
  return $?
}

if [[ "$EUID" -ne 0  ]]
then
   SUDO="sudo -n "
fi

[ $# -ne 1 ] && echo "Usage: $(basename $0) <new private key file>" && exit 3
newprivkey=$1
[ ! -f $newprivkey ] && echo "$(basename $0): Could not open $newprivkey" && exit 3

# if running into Docker as unprivileges, skip ssh verification as iso cannot be mounted due to missing loop device.
if [ -f /.dockerenv ]; then
  if [ -e /dev/loop0 ]; then
    # it's a docker instance with privileges.
    copy_priv_key $newprivkey
  else
    # this mean it's a docker instance, ssh key cannot be verified.
    echo "We run inside Docker, skipping copying private key"
  fi
else
  copy_priv_key $newprivkey
fi
