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

check_required_fields() {
    local missing=()
    for varname in "$@"; do
        local value="${!varname}"
        if [[ -z "$value" ]]; then
            missing+=("$varname")
        fi
    done

    if [[ ${#missing[@]} -gt 0 ]]; then
        echo "{\"error\":\"Missing required fields: ${missing[*]}\"}"
        exit 1
    fi
}

cloudstack_vm_internal_name_to_proxmox_vmid() {
    local vm_internal_name="$1"
    if [[ -z "$vm_internal_name" || ! "$vm_internal_name" =~ ^i-[0-9]+-[0-9]+ ]]; then
            echo "{\"error\":\"Invalid VM Internal Name: '$vm_internal_name'\"}"
            exit 1
        fi

    local account id vmid
    account=$(echo "$vm_internal_name" | cut -d '-' -f2)
    id=$(echo "$vm_internal_name" | cut -d '-' -f3)
    id=$(printf "%04d" "$id")
    vmid="${account}${id}"
    echo "$vmid"
}

validate_name() {
    local entity="$1"
    local name="$2"
    if [[ ! "$name" =~ ^[a-zA-Z0-9-]+$ ]]; then
        echo "{\"error\":\"Invalid $entity name '$name'. Only alphanumeric characters and dashes (-) are allowed.\"}"
        exit 1
    fi
}

parse_json() {
    local json_string="$1"
    echo "$json_string" | jq '.' > /dev/null || { echo '{"error":"Invalid JSON input"}'; exit 1; }

    local -A details
    while IFS="=" read -r key value; do
        details[$key]="$value"
    done < <(echo "$json_string" | jq -r '{
        "url": (.externaldetails.extension.url // ""),
        "user": (.externaldetails.extension.user // ""),
        "token": (.externaldetails.extension.token // ""),
        "secret": (.externaldetails.extension.secret // ""),
        "node": (.externaldetails.host.node // ""),
        "vm_name": (.externaldetails.virtualmachine.vm_name // ""),
        "template_id": (.externaldetails.virtualmachine.template_id // ""),
        "template_type": (.externaldetails.virtualmachine.template_type // ""),
        "iso_path": (.externaldetails.virtualmachine.iso_path // ""),
        "vm_internal_name": (."cloudstack.vm.details".name // ""),
        "vmmemory": (."cloudstack.vm.details".minRam // ""),
        "vmcpus": (."cloudstack.vm.details".cpus // ""),
        "vlan": (."cloudstack.vm.details".nics[0].broadcastUri // "" | sub("vlan://"; "")),
        "mac_address": (."cloudstack.vm.details".nics[0].mac // ""),
        "snap_name": (.parameters.snap_name // ""),
        "snap_description": (.parameters.snap_description // ""),
        "snap_save_memory": (.parameters.snap_save_memory // "")
    } | to_entries | .[] | "\(.key)=\(.value)"')

    for key in "${!details[@]}"; do
        declare -g "$key=${details[$key]}"
    done

    check_required_fields vm_internal_name url user token secret node

    if [[ -z "$vm_name" ]]; then
        vm_name="$vm_internal_name"
    fi
    validate_name "VM" "$vm_name"
    vmid=$(cloudstack_vm_internal_name_to_proxmox_vmid "$vm_internal_name")
}

call_proxmox_api() {
    local method=$1
    local path=$2
    local data=$3

    echo "curl -sk --fail -X $method -H \"Authorization: PVEAPIToken=${user}!${token}=${secret}\" ${data:+-d \"$data\"} https://${url}:8006/api2/json${path}" >&2
    response=$(curl -sk --fail -X "$method" \
        -H "Authorization: PVEAPIToken=${user}!${token}=${secret}" \
        ${data:+-d "$data"} \
        "https://${url}:8006/api2/json${path}")
    echo "$response"
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
            exit 1
        fi

        local status_response
        status_response=$(call_proxmox_api GET "/nodes/${node}/tasks/$(urlencode "$upid")/status")

        if [[ -z "$status_response" || "$status_response" == *'"errors":'* ]]; then
            local msg
            msg=$(echo "$status_response" | jq -r '.message // "Unknown error"')
            echo "{\"error\":\"$msg\"}"
            exit 1
        fi

        local task_status
        task_status=$(echo "$status_response" | jq -r '.data.status')

        if [[ "$task_status" == "stopped" ]]; then
            local exit_status
            exit_status=$(echo "$status_response" | jq -r '.data.exitstatus')
            if [[ "$exit_status" != "OK" ]]; then
                echo "{\"error\":\"Task failed with exit status: $exit_status\"}"
                exit 1
            fi
            return 0
        fi

        sleep "$interval"
    done
}

execute_and_wait() {
    local method="$1"
    local path="$2"
    local data="$3"
    local response upid msg

    response=$(call_proxmox_api "$method" "$path" "$data")
    upid=$(echo "$response" | jq -r '.data // ""')

    if [[ -z "$upid" ]]; then
        msg=$(echo "$response" | jq -r '.message // "Unknown error"')
        echo "{\"error\":\"Failed to execute API or retrieve UPID. Message: $msg\"}"
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

    check_required_fields vm_name vlan mac_address
    validate_name "VM" "$vm_name"

    if [[ "${template_type^^}" == "ISO" ]]; then
        check_required_fields iso_path vmcpus vmmemory
        local data="vmid=$vmid"
        data+="&name=$vm_name"
        data+="&ide2=$(urlencode "$iso_path,media=cdrom")"
        data+="&ostype=l26"
        data+="&scsihw=virtio-scsi-single"
        data+="&scsi0=$(urlencode "local-lvm:64,iothread=on")"
        data+="&sockets=1"
        data+="&cores=$vmcpus"
        data+="&numa=0"
        data+="&cpu=x86-64-v2-AES"
        data+="&memory=$((vmmemory / 1024 / 1024))"
        execute_and_wait POST "/nodes/${node}/qemu/" "$data"
    else
        check_required_fields template_id
        local data="newid=$vmid"
        data+="&name=$vm_name"
        execute_and_wait POST "/nodes/${node}/qemu/${template_id}/clone" "$data"
    fi

    network="net0=$(urlencode "virtio=${mac_address},bridge=vmbr0,tag=${vlan},firewall=1")"
    call_proxmox_api PUT "/nodes/${node}/qemu/${vmid}/config/" "$network" > /dev/null

    execute_and_wait POST "/nodes/${node}/qemu/${vmid}/status/start"

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

create_snapshot() {
    parse_json "$1" || exit 1

    check_required_fields snap_name
    validate_name "Snapshot" "$snap_name"

    local data, vmstate
    data="snapname=$snap_name"
    if [[ -n "$snap_description" ]]; then
        data+="&description=$snap_description"
    fi
    if [[ -n "$snap_save_memory" && "$snap_save_memory" == "true" ]]; then
        vmstate="1"
    else
        vmstate="0"
    fi
    data+="&vmstate=$vmstate"

    execute_and_wait POST "/nodes/${node}/qemu/${vmid}/snapshot" "$data"
    echo '{"status": "success", "message": "Instance Snapshot created"}'
}

restore_snapshot() {
    parse_json "$1" || exit 1

    check_required_fields snap_name
    validate_name "Snapshot" "$snap_name"

    execute_and_wait POST "/nodes/${node}/qemu/${vmid}/snapshot/${snap_name}/rollback"

    execute_and_wait POST "/nodes/${node}/qemu/${vmid}/status/start"

    echo '{"status": "success", "message": "Instance Snapshot restored"}'
}

delete_snapshot() {
    parse_json "$1" || exit 1

    check_required_fields snap_name
    validate_name "Snapshot" "$snap_name"

    execute_and_wait DELETE "/nodes/${node}/qemu/${vmid}/snapshot/${snap_name}"
    echo '{"status": "success", "message": "Instance Snapshot deleted"}'
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
    CreateSnapshot)
        create_snapshot "$parameters"
        ;;
    RestoreSnapshot)
        restore_snapshot "$parameters"
        ;;
    DeleteSnapshot)
        delete_snapshot "$parameters"
        ;;
    *)
        echo '{"error":"Invalid action"}'
        exit 1
        ;;
esac

exit 0
