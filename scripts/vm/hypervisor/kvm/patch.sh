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

# Get the VM name and cmdline
while getopts "n:c:h" opt; do
  case ${opt} in
    n )
      name=$OPTARG
      ;;
    c )
      cmdline=$(echo $OPTARG | base64 -w 0)
      ;;
    h )
      echo "Usage: $0 -n [VM name] -c [command line]"
      exit 0
      ;;
  esac
done

SSHKEY_FILE="/root/.ssh/id_rsa.pub.cloud"
if [ ! -e $SSHKEY_FILE ]; then
    echo "SSH public key file $SSHKEY_FILE not found!"
    exit 1
fi

if ! which virsh > /dev/null; then
    echo "Libvirt CLI 'virsh' not found"
    exit 1
fi

# Read the SSH public key
sshkey=$(cat $SSHKEY_FILE | base64 -w 0)

# Method to send and write payload inside the VM
send_file() {
    local name=${1}
    local path=${2}
    local content=${@:3}
    local fd=$(virsh qemu-agent-command $name "{\"execute\":\"guest-file-open\", \"arguments\":{\"path\":\"$path\",\"mode\":\"w+\"}}" | sed 's/[^:]*:\([^}]*\).*/\1/')
    virsh qemu-agent-command $name "{\"execute\":\"guest-file-write\", \"arguments\":{\"handle\":$fd,\"buf-b64\":\"$content\"}}" > /dev/null
    virsh qemu-agent-command $name "{\"execute\":\"guest-file-close\", \"arguments\":{\"handle\":$fd}}" > /dev/null
}

# Wait for the guest agent to come online
while ! virsh qemu-agent-command $name '{"execute":"guest-ping"}' >/dev/null 2>&1
do
    sleep 0.1
done

# Test guest agent sanity
while [ "$(virsh qemu-agent-command $name '{"execute":"guest-sync","arguments":{"id":1234567890}}' 2>/dev/null)" != '{"return":1234567890}' ]
do
    sleep 0.1
done

# Write ssh public key
send_file $name "/root/.ssh/authorized_keys" $sshkey

# Fix ssh public key permission
virsh qemu-agent-command $name '{"execute":"guest-exec","arguments":{"path":"chmod","arg":["go-rwx","/root/.ssh/authorized_keys"]}}' > /dev/null

# Write cmdline payload
send_file $name "/var/cache/cloud/cmdline" $cmdline
