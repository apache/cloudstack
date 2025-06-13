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

generate_random_mac() {
    hexchars="0123456789ABCDEF"
    echo "52:54:00:$(for i in {1..3}; do echo -n ${hexchars:$(( RANDOM % 16 )):1}${hexchars:$(( RANDOM % 16 )):1}; [[ $i -lt 3 ]] && echo -n ':'; done)"
}

urlencode() {
    encoded_data=$(python3 -c "import urllib.parse; print(urllib.parse.quote('''$1'''))")
    echo "$encoded_data"
}

wait_for_proxmox_task() {
    local upid="$1"
    local timeout="${2:-$wait_time}"
    local interval="${3:-1}"

    local start_time
    start_time=$(date +%s)

    while true; do
        local now
        now=$(date +%s)
        if (( now - start_time > timeout )); then
            echo '{"error":"Timeout while waiting for async task"}'
            return 1
        fi

        local status_response
        status_response=$(call_proxmox_api GET "/nodes/${node}/tasks/$(urlencode "$upid")/status")

        if [[ -z "$status_response" || "$status_response" == *'"errors":'* ]]; then
            local msg
            msg=$(echo "$status_response" | jq -r '.message // "Unknown error"')
            echo "{\"error\":\"$msg\"}"
            return 1
        fi

        local task_status
        task_status=$(echo "$status_response" | jq -r '.data.status')

        if [[ "$task_status" == "stopped" ]]; then
            local exit_status
            exit_status=$(echo "$status_response" | jq -r '.data.exitstatus')
            echo "{\"exitstatus\":\"$exit_status\"}"
            return 0
        fi

        sleep "$interval"
    done
}

cloudstack_vmname_to_proxmox_vmid() {
    local vmname="$1"
    account=$(echo "$vmname" | cut -d '-' -f2)
    id=$(echo "$vmname" | cut -d '-' -f3)
    id=$(printf "%04d" "$id")
    vmid="${account}${id}"
    echo "$vmid"
}

parse_json() {
    local json_string="$1"
    echo "$json_string" | jq '.' > /dev/null || { echo '{"error":"Invalid JSON input"}'; exit 1; }

    local -A details
    while IFS="=" read -r key value; do
        details[$key]="$value"
    done < <(echo "$json_string" | jq -r '{
        "url": .external.extensionid.url,
        "user": .external.extensionid.user,
        "token": .external.extensionid.token,
        "secret": .external.extensionid.secret,
        "node": .external.hostid.node,
        "templateid": .external.virtualmachineid.templateid,
        "templatetype": .external.virtualmachineid.isiso,
        "vmname": ."cloudstack.vm.details".name,
        "vmmemory": ."cloudstack.vm.details".minRam,
        "vmcpus": ."cloudstack.vm.details".cpus,
        "vlan": (."cloudstack.vm.details".nics[0].broadcastUri | sub("vlan://"; "")),
        "mac_address": ."cloudstack.vm.details".nics[0].mac
    } | to_entries | .[] | "\(.key)=\(.value)"')

    for key in "${!details[@]}"; do
        declare -g "$key=${details[$key]}"
    done

    if [[ "$vmname" == "null" || "$url" == "null" || "$user" == "null" || "$token" == "null" || "$secret" == "null" || "$node" == "null" ]]; then
        echo '{"error":"Missing required fields in JSON input"}'
        exit 1
    fi

    vmid=$(cloudstack_vmname_to_proxmox_vmid "$vmname")
}

call_proxmox_api() {
    local method=$1
    local path=$2
    local data=$3

    curl -sk -X "$method" \
        -H "Authorization: PVEAPIToken=${user}!${token}=${secret}" \
        ${data:+-d "$data"} \
        "https://${url}:8006/api2/json${path}"
}

execute_and_wait() {
    local method="$1"
    local path="$2"
    local data="$3"

    local response
    response=$(call_proxmox_api "$method" "$path" "$data")

    local upid
    upid=$(echo "$response" | jq -r '.data')

    if [[ -z "$upid" || "$upid" == "null" ]]; then
        echo '{"error":"Failed to execute API or retrieve UPID"}'
        exit 1
    fi

    wait_for_proxmox_task "$upid"
}

prepare() {
    parse_json "$1" || exit 1

    local mac_address
    mac_address=$(generate_random_mac)

    local response
    response=$(jq -n --arg mac "$mac_address" \
        '{status: "success", mac_address: $mac}')

    echo "$response"
}

create() {
    parse_json "$1" || exit 1

    if [[ "$templatetype" == "ISO" ]]; then
        local data="vmid=$vmid"
        data+="&name=$name"
        data+="&ide2=$(urlencode "$iso_path,media=cdrom")"
        data+="&ostype=l26"
        data+="&scsihw=virtio-scsi-single"
        data+="&scsi0=$(urlencode "local-lvm:32,iothread=on")"
        data+="&sockets=1"
        data+="&cores=$vmcpus"
        data+="&numa=0"
        data+="&cpu=x86-64-v2-AES"
        data+="&memory=$vmmemory"
        execute_and_wait POST "/nodes/${node}/qemu/" "$data"
    else
        local data="newid=$vmid"
        data+="&name=$name"
        execute_and_wait POST "/nodes/${node}/qemu/${templateid}/clone" "$data"
    fi

    network="net0=$(urlencode "virtio=${mac_address},bridge=vmbr0,tag=${vlan},firewall=1")"
    call_proxmox_api PUT "/nodes/${node}/qemu/${vmid}/config/" "$network"

    start "$1"

    echo '{"status": "success", "message": "Instance created"}'
}

start() {
    parse_json "$1" || exit 1
    execute_and_wait POST "/nodes/${node}/qemu/${vmid}/status/start"
    echo '{"status": "success", "message": "Instance started"}'
}

delete() {
    parse_json "$1" || exit 1
    execute_and_wait DELETE "/nodes/${node}/qemu/${vmid}"
    echo '{"status": "success", "message": "Instance deleted"}'
}

stop() {
    parse_json "$1" || exit 1
    execute_and_wait POST "/nodes/${node}/qemu/${vmid}/status/stop"
    echo '{"status": "success", "message": "Instance stopped"}'
}

reboot() {
    parse_json "$1" || exit 1
    execute_and_wait POST "/nodes/${node}/qemu/${vmid}/status/reboot"
    echo '{"status": "success", "message": "Instance rebooted"}'
}

status() {
    parse_json "$1" || exit 1

    local status_response vm_status powerstate
    status_response=$(call_proxmox_api GET "/nodes/${node}/qemu/${vmid}/status/current")
    vm_status=$(echo "$status_response" | jq -r '.data.status')
    case "$vm_status" in
        running)  powerstate="poweron"  ;;
        stopped)  powerstate="poweroff" ;;
        *)        powerstate="unknown"  ;;
    esac

    echo "{\"status\": \"success\", \"power_state\": \"$powerstate\"}"
}

action=$1
parameters_file="$2"
wait_time=$3

if [[ -z "$action" || -z "$parameters_file" ]]; then
    echo '{"error":"Missing required arguments"}'
    exit 1
fi

# Read file content as parameters (assumes space-separated arguments)
parameters=$(<"$parameters_file")

case $action in
    prepare)
        prepare "$parameters"
        ;;
    create)
        create "$parameters"
        ;;
    delete)
        delete "$parameters"
        ;;
    start)
        start "$parameters"
        ;;
    stop)
        stop "$parameters"
        ;;
    reboot)
        reboot "$parameters"
        ;;
    status)
        status "$parameters"
        ;;
    *)
        echo '{"error":"Invalid action"}'
        exit 1
        ;;
esac

exit 0
