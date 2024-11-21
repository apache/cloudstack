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
set +u

prepare() {
    parsed_arguments="$1"
    # Add code to handle preparation logic
}

create() {
    local vm_id=$((1000 + id))

    sshpass_path=$(which sshpass)
    ssh_path=$(which ssh)
    local ssh_command="sudo $sshpass_path -p 'password' ssh -T root@$endpoint \
    qm create $vm_id \
    --ide2 local:iso/TinyCore-8.0.iso,media=cdrom \
    --ostype l26 \
    --scsihw virtio-scsi-single \
    --scsi0 local-lvm:32,iothread=on \
    --sockets 1 \
    --cores 1 \
    --numa 0 \
    --cpu x86-64-v2-AES \
    --memory 2048 \
    --net0 virtio=$mac,bridge=vmbr0,tag=$vlan,firewall=1"

    eval "$ssh_command"
    local ssh_command2="sudo $sshpass_path -p 'password' $ssh_path root@$endpoint \
    qm start $vm_id"

    eval "$ssh_command2"
}

delete() {
    parsed_arguments="$1"
    # Add code to handle delete logic
}

action=$1
parameters="$2"
wait_time=$3

if [[ -z $action || -z $parameters || -z $wait_time ]]; then
    exit 1
fi

case $action in
    prepare)
        prepare "$parameters" "$wait_time"
        ;;
    create)
        declare -A arguments
        while IFS= read -r line; do
            key=$(echo "$line" | awk '{print $1}')
            value=$(echo "$line" | awk '{print $2}')
            arguments["$key"]="$value"
        done < <(echo "$parameters" | jq -r 'to_entries | .[] | "\(.key) \(.value)"')

        endpoint=${arguments['endpoint.url']}
        vlan=${arguments['cloudstack.vlan']}
        id=${arguments['cloudstack.id']}
        mac=${arguments['cloudstack.mac']}
        create "$parameters" "$wait_time"
        ;;
    delete)
        delete "$parameters" "$wait_time"
        ;;
    *)
        exit 1
        ;;
esac
set -u
exit 0
