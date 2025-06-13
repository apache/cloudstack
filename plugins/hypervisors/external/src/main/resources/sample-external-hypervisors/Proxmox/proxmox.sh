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

log_file_name="proxmox.log"
conf_file_name="detailsmap.conf"
declare -A abhisar

generate_random_mac() {
    hexchars="0123456789ABCDEF"
    echo "52:54:00:$(for i in {1..3}; do echo -n ${hexchars:$(( RANDOM % 16 )):1}${hexchars:$(( RANDOM % 16 )):1}; [[ $i -lt 3 ]] && echo -n ':'; done)"
}

get_filepath() {
    local file_name="$1"
    echo "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/$file_name"
}

get_log_file() {
    get_filepath "$log_file_name"
}

get_conf_file() {
    get_filepath "$conf_file_name"
}

logit() {
    local message=$1
    echo "$message" >> "$(get_log_file)"
}

urlencode() {
    encoded_data=$(python3 -c "import urllib.parse; print(urllib.parse.quote('''$1'''))")
    echo "$encoded_data"
}

load_external_details() {
    local full_json="$1"
    logit "load_external_details $full_json"

    declare -A details_map
    details_map["mac_address"]="mac_address"
    details_map["vlan"]="cloudstack.vlan"

    # Read config and build key map
    while IFS='=' read -r key value; do
        key=$(echo "$key" | xargs)
        value=$(echo "$value" | xargs)
        [[ -z $key || $key == \#* ]] && continue  # skip blanks/comments
        details_map["$key"]="$value"
    done < "$(get_conf_file)"

    # Compose jq query using only top-level keys
    jq_query=$(for key in "${!details_map[@]}"; do
        jq_key="${details_map[$key]}"
        echo -n ".\"$jq_key\" // \"null\", "
    done)
    jq_query="[ ${jq_query%, } ] | @tsv"

    #logit "jq_query $jq_query"

    # Extract and assign values to variables
    IFS=$'\t' read -r -a extracted_values < <(jq -r "$jq_query" <<< "$full_json")
    i=0
    for key in "${!details_map[@]}"; do
        value="${extracted_values[$i]}"
        [[ -z "$value" || "$value" == "null" ]] && value="null"
        declare -g "$key=$value"
        logit "$key = $value"
        ((i++))
    done
    logit "external details : $name, $url, $user, $token, $secret, $node, $storage, $templateid, $vlan, $mac_address"
}

wait_for_proxmox_task() {
    local upid="$1"
    local timeout="${2:-60}"  # optional timeout in seconds (default: 60)
    local interval="${3:-1}"  # optional polling interval (default: 1s)

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

        # Check for invalid or error responses
        if [[ -z "$status_response" || "$status_response" == *'"errors":'* ]]; then
            local msg
            msg=$(echo "$status_response" | jq -r '.message // "Unknown error"')
            echo "{\"error\":\"$msg\"}"
            return 1
        fi

        local task_status
        task_status=$(echo "$status_response" | jq -r '.data.status')
        logit "task_reponse $task_response"

        if [[ "$task_status" == "stopped" ]]; then
            local exit_status
            exit_status=$(echo "$status_response" | jq -r '.data.exitstatus')
            logit "exit_status $exit_status"
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

    load_external_details "$json_string"

    vmname=$(echo "$json_string" | jq -r '."cloudstack.vm.details" | fromjson | .name')
    vmid=$(cloudstack_vmname_to_proxmox_vmid "$vmname")
}

call_proxmox_api() {
    local method=$1
    local path=$2
    local data=$3

    logit "call api: $1 $2 $3 $user $token $secret"
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
    logit "execute and wait: $method $path $data response: $response"

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
        data+="&cores=1"
        data+="&numa=0"
        data+="&cpu=x86-64-v2-AES"
        data+="&memory=2048"
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
    logit "vmstatus $vm_status"
    case "$vm_status" in
        running)  powerstate="poweron"  ;;
        stopped)  powerstate="poweroff" ;;
        *)        powerstate="unknown"  ;;
    esac

    echo '{"status": "success", "power_state": "$powerstate"}'
}

check_console_url() {
    local url="$1"

    http_code=$(curl -sk -o /dev/null -w "%{http_code}" "$url")

    if [[ "$http_code" == "200" ]]; then
        logit "Console URL is accessible"
    else
        logit "Console URL check failed with HTTP code: $http_code"
    fi
}

getWebSocketUrl() {
    parse_json "$1" || exit 1
    response=$(call_proxmox_api POST "/nodes/${node}/qemu/${vmid}/vncproxy")
    ticket=$(echo "$response" | jq -r '.data.ticket')
    port=$(echo "$response" | jq -r '.data.port')
    encoded_ticket=$(urlencode "$ticket")
    websocket_url="wss://${url}:8006/api2/json/nodes/${node}/qemu/${vmid}/vncwebsocket?port=${port}&vncticket=${encoded_ticket}"
    browser_url="https://${url}:8006/?console=kvm&novnc=1&vmid=${vmid}&vmname=${vmname}&node=${node}&resize=off&vncticket=${encoded_ticket}"
    logit "browser url: $browser_url"
    check_console_url "$browser_url"

    logit "websocket url : $console_url"
    echo '{"status": "success", "message": "$url"}'
}

createVMSnapshot() {
    parse_json "$1" || exit 1
    execute_and_wait POST "/nodes/${node}/qemu/${vmid}/snapshot"
    echo '{"status": "success", "message": "VM snapshot is created"}'
}

deleteVMSnapshot() {
    parse_json "$1" || exit 1
    execute_and_wait DELETE "/nodes/${node}/qemu/${vmid}/snapshot/${snapname}"
    echo '{"status": "success", "message": "VM snapshot is deleted"}'
}

restoreVMSnapshot() {
    parse_json "$1" || exit 1
    logit "in restpreVMSnapshot $1"
    execute_and_wait POST "/nodes/${node}/qemu/${vmid}/snapshot/${snapname}/rollback"
    echo '{"status": "success", "message": "VM snapshot is restored"}'
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

logit ""
logit "===================================="
logit "$action"
logit "===================================="

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
    getWebSocketUrl)
        getWebSocketUrl "$parameters"
        ;;
    createVMSnapshot)
        createVMSnapshot "$parameters"
        ;;
    deleteVMSnapshot)
        deleteVMSnapshot "$parameters"
        ;;
    restoreVMSnapshot)
        restoreVMSnapshot "$parameters"
        ;;
    testAction)
        restoreVMSnapshot "$parameters"
        ;;
    *)
        echo '{"error":"Invalid action"}'
        exit 1
        ;;
esac

exit 0
