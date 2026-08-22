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
      bootargs=$OPTARG
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

# Wait for the guest agent to come online (max 120s to avoid indefinite hang)
# FIX: Added timeout + clear error message (GitHub Issue #13471)
GUEST_AGENT_WAIT_TICK=0
GUEST_AGENT_MAX_TICKS=1200  # 120s = 1200 x 0.1s
while ! virsh qemu-agent-command $name '{"execute":"guest-ping"}' >/dev/null 2>&1
do
    sleep 0.1
    GUEST_AGENT_WAIT_TICK=$((GUEST_AGENT_WAIT_TICK + 1))
    if [ $((GUEST_AGENT_WAIT_TICK % 100)) -eq 0 ]; then
        echo "Waiting for qemu-guest-agent to respond... (${GUEST_AGENT_WAIT_TICK}/1200 ticks, ~$((GUEST_AGENT_WAIT_TICK / 10))s elapsed)"
    fi
    if [ $GUEST_AGENT_WAIT_TICK -ge $GUEST_AGENT_MAX_TICKS ]; then
        echo "ERROR: qemu-guest-agent not responding after 120 seconds."
        echo "The VM template is missing 'qemu-guest-agent' or the service is not running."
        echo "Required packages: cloud-init, qemu-guest-agent, cloud-guest-utils, conntrack, containerd.io"
        echo "See: https://docs.cloudstack.apache.org/en/latest/kubernetes/kubernetes-cluster-requirements.html"
        exit 1
    fi
done
echo "qemu-guest-agent is responsive."

# Test guest agent sanity (bounded to 30s)
# FIX: Added timeout (GitHub Issue #13471)
GUEST_SYNC_TICK=0
GUEST_SYNC_MAX_TICKS=300  # 30s
while [ "$(virsh qemu-agent-command $name '{"execute":"guest-sync","arguments":{"id":1234567890}}' 2>/dev/null)" != '{"return":1234567890}' ]; do
    sleep 0.1
    GUEST_SYNC_TICK=$((GUEST_SYNC_TICK + 1))
    if [ $GUEST_SYNC_TICK -ge $GUEST_SYNC_MAX_TICKS ]; then
        echo "ERROR: qemu-guest-agent sanity check (guest-sync) failed after 30 seconds."
        exit 1
    fi
done

# Write cmdline payload
send_file $name "/var/cache/cloud/cmdline" $cmdline
