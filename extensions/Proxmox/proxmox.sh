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

parse_json() {
    local json_string="$1"
    echo "$json_string" | jq '.' > /dev/null || { echo '{"error":"Invalid JSON input"}'; exit 1; }

    local -A details
    while IFS="=" read -r key value; do
        details[$key]="$value"
    done < <(echo "$json_string" | jq -r '{
        "extension_url":    (.externaldetails.extension.url // ""),
        "extension_user":   (.externaldetails.extension.user // ""),
        "extension_token":  (.externaldetails.extension.token // ""),
        "extension_secret": (.externaldetails.extension.secret // ""),
        "host_url":         (.externaldetails.host.url // ""),
        "host_user":        (.externaldetails.host.user // ""),
        "host_token":       (.externaldetails.host.token // ""),
        "host_secret":      (.externaldetails.host.secret // ""),
        "node":             (.externaldetails.host.node // ""),
        "network_bridge":   (.externaldetails.host.network_bridge // ""),
        "verify_tls_certificate": (.externaldetails.host.verify_tls_certificate // "true"),
        "vm_name":          (.externaldetails.virtualmachine.vm_name // ""),
        "template_id":      (.externaldetails.virtualmachine.template_id // ""),
        "template_type":    (.externaldetails.virtualmachine.template_type // ""),
        "iso_path":         (.externaldetails.virtualmachine.iso_path // ""),
        "snap_name":        (.parameters.snap_name // ""),
        "snap_description": (.parameters.snap_description // ""),
        "snap_save_memory": (.parameters.snap_save_memory // ""),
        "vmid":             (."cloudstack.vm.details".details.proxmox_vmid // ""),
        "vm_internal_name": (."cloudstack.vm.details".name // ""),
        "vmmemory":         (."cloudstack.vm.details".minRam // ""),
        "vmcpus":           (."cloudstack.vm.details".cpus // ""),
        "vlans":            ([."cloudstack.vm.details".nics[]?.broadcastUri // "" | sub("vlan://"; "")] | join(",")),
        "mac_addresses":    ([."cloudstack.vm.details".nics[]?.mac // ""] | join(","))
    } | to_entries | .[] | "\(.key)=\(.value)"')

    for key in "${!details[@]}"; do
        declare -g "$key=${details[$key]}"
    done

    # set url, user, token, secret to host values if present, otherwise use extension values
    url="${host_url:-$extension_url}"
    user="${host_user:-$extension_user}"
    token="${host_token:-$extension_token}"
    secret="${host_secret:-$extension_secret}"

    check_required_fields vm_internal_name url user token secret node
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

validate_name() {
    local entity="$1"
    local name="$2"
    if [[ ! "$name" =~ ^[a-zA-Z0-9-]+$ ]]; then
        echo "{\"error\":\"Invalid $entity name '$name'. Only alphanumeric characters and dashes (-) are allowed.\"}"
        exit 1
    fi
}

call_proxmox_api() {
    local method=$1
    local path=$2
    local data=$3

    curl_opts=(
      -s
      --fail
      -X "$method"
      -H "Authorization: PVEAPIToken=${user}!${token}=${secret}"
    )

    if [[ "$verify_tls_certificate" == "false" ]]; then
      curl_opts+=(-k)
    fi

    if [[ -n "$data" ]]; then
      curl_opts+=(-d "$data")
    fi

    #echo curl "${curl_opts[@]}" "https://${url}:8006/api2/json${path}" >&2
    response=$(curl "${curl_opts[@]}" "https://${url}:8006/api2/json${path}")
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

vm_not_present() {
    response=$(call_proxmox_api GET "/cluster/nextid?vmid=$vmid")
    vmid_result=$(echo "$response" | jq -r '.data // empty')
    if [[ "$vmid_result" == "$vmid" ]]; then
        return 0
    else
        return 1
    fi
}

prepare() {
    response=$(call_proxmox_api GET "/cluster/nextid")
    vmid=$(echo "$response" | jq -r '.data // ""')

    echo "{\"details\":{\"proxmox_vmid\": \"$vmid\"}}"
}

create() {
    if [[ -z "$vm_name" ]]; then
        vm_name="$vm_internal_name"
    fi
    validate_name "VM" "$vm_name"
    check_required_fields vmid network_bridge vmcpus vmmemory

    if [[ "${template_type^^}" == "ISO" ]]; then
        check_required_fields iso_path
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
        cleanup_vm=1

    else
        check_required_fields template_id
        local data="newid=$vmid"
        data+="&name=$vm_name"
        execute_and_wait POST "/nodes/${node}/qemu/${template_id}/clone" "$data"
        cleanup_vm=1

        data="cores=$vmcpus"
        data+="&memory=$((vmmemory / 1024 / 1024))"
        execute_and_wait POST "/nodes/${node}/qemu/${vmid}/config" "$data"
    fi

    IFS=',' read -ra vlan_array <<< "$vlans"
    IFS=',' read -ra mac_array <<< "$mac_addresses"
    for i in "${!vlan_array[@]}"; do
        network="net${i}=$(urlencode "virtio=${mac_array[i]},bridge=${network_bridge},tag=${vlan_array[i]},firewall=0")"
        call_proxmox_api PUT "/nodes/${node}/qemu/${vmid}/config/" "$network" > /dev/null
    done

    execute_and_wait POST "/nodes/${node}/qemu/${vmid}/status/start"

    cleanup_vm=0
    echo '{"status": "success", "message": "Instance created"}'
}

start() {
    execute_and_wait POST "/nodes/${node}/qemu/${vmid}/status/start"
    echo '{"status": "success", "message": "Instance started"}'
}

delete() {
    if vm_not_present; then
        echo '{"status": "success", "message": "Instance deleted"}'
        return 0
    fi
    execute_and_wait DELETE "/nodes/${node}/qemu/${vmid}"
    echo '{"status": "success", "message": "Instance deleted"}'
}

stop() {
    if vm_not_present; then
        echo '{"status": "success", "message": "Instance stopped"}'
        return 0
    fi
    execute_and_wait POST "/nodes/${node}/qemu/${vmid}/status/stop"
    echo '{"status": "success", "message": "Instance stopped"}'
}

reboot() {
    execute_and_wait POST "/nodes/${node}/qemu/${vmid}/status/reboot"
    echo '{"status": "success", "message": "Instance rebooted"}'
}

status() {
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

list_snapshots() {
    snapshot_response=$(call_proxmox_api GET "/nodes/${node}/qemu/${vmid}/snapshot")
    echo "$snapshot_response" | jq '
      def to_date:
        if . == "-" then "-"
        elif . == null then "-"
        else (. | tonumber | strftime("%Y-%m-%d %H:%M:%S"))
        end;

      {
        status: "success",
        printmessage: "true",
        message: [.data[] | {
          name: .name,
          snaptime: ((.snaptime // "-") | to_date),
          description: .description,
          parent: (.parent // "-"),
          vmstate: (.vmstate // "-")
        }]
      }
    '
}

create_snapshot() {
    check_required_fields snap_name
    validate_name "Snapshot" "$snap_name"

    local data vmstate
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
    check_required_fields snap_name
    validate_name "Snapshot" "$snap_name"

    execute_and_wait POST "/nodes/${node}/qemu/${vmid}/snapshot/${snap_name}/rollback"

    status_response=$(call_proxmox_api GET "/nodes/${node}/qemu/${vmid}/status/current")
    vm_status=$(echo "$status_response" | jq -r '.data.status')
    if [ "$vm_status" = "stopped" ];then
        execute_and_wait POST "/nodes/${node}/qemu/${vmid}/status/start"
    fi

    echo '{"status": "success", "message": "Instance Snapshot restored"}'
}

delete_snapshot() {
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

parse_json "$parameters" || exit 1

cleanup_vm=0
cleanup() {
  if (( cleanup_vm == 1 )); then
    execute_and_wait DELETE "/nodes/${node}/qemu/${vmid}"
  fi
}

trap cleanup EXIT

case $action in
    prepare)
        prepare
        ;;
    create)
        create
        ;;
    delete)
        delete
        ;;
    start)
        start
        ;;
    stop)
        stop
        ;;
    reboot)
        reboot
        ;;
    status)
        status
        ;;
    ListSnapshots)
        list_snapshots
        ;;
    CreateSnapshot)
        create_snapshot
        ;;
    RestoreSnapshot)
        restore_snapshot
        ;;
    DeleteSnapshot)
        delete_snapshot
        ;;
    *)
        echo '{"error":"Invalid action"}'
        exit 1
        ;;
esac

exit 0
