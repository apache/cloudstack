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

parse_json() {
    local json_string=$1
    echo "$json_string" | jq '.' > /dev/null || { echo '{"status": "error", "error": "Invalid JSON input"}'; exit 1; }
}

generate_random_mac() {
    hexchars="0123456789ABCDEF"
    echo "52:54:00:$(for i in {1..3}; do echo -n ${hexchars:$(( RANDOM % 16 )):1}${hexchars:$(( RANDOM % 16 )):1}; [[ $i -lt 3 ]] && echo -n ':'; done)"
}

prepare() {
    parse_json "$1" || exit 1

    local input_json="$1"
    local nics_json
    nics_json=$(echo "$input_json" | jq '.["cloudstack.vm.details"].nics')

    # If NICs array is empty, return empty string
    if [ "$(echo "$nics_json" | jq 'length // 0')" -eq 0 ]; then
        echo ""
        return
    fi

    local result='{"nics":['
    local first=1

    while IFS= read -r uuid; do
        local mac
        mac=$(generate_random_mac)

        if [ $first -eq 1 ]; then
            first=0
        else
            result+=','
        fi

        result+='{"uuid":"'"$uuid"'","mac":"'"$mac"'"}'
    done < <(echo "$nics_json" | jq -r '.[].uuid')

    result+=']}'
    echo "$result"
}

create() {
    parse_json "$1" || exit 1

    local response
    response=$(jq -n \
        '{status: "success", message: "Instance created"}')

    echo "$response"
}

delete() {
    parse_json "$1" || exit 1

    local response
    response=$(jq -n \
        '{status: "success", message: "Instance deleted"}')

    echo "$response"
}

start() {
    parse_json "$1" || exit 1
    echo '{"status": "success", "message": "Instance started"}'
}

stop() {
    parse_json "$1" || exit 1
    echo '{"status": "success", "message": "Instance stopped"}'
}

reboot() {
    parse_json "$1" || exit 1
    echo '{"status": "success", "message": "Instance rebooted"}'
}

status() {
    parse_json "$1" || exit 1
    echo '{"status": "success", "power_state": "poweron"}'
}

statuses() {
    parse_json "$1" || exit 1
    # This external system can not return an output like the following:
    # {"status":"success","power_state":{"i-3-23-VM":"poweroff","i-2-25-VM":"poweron"}}
    # CloudStack can fallback to retrieving the power state of the single VM using the "status" action
    echo '{"status": "error", "message": "Not supported"}'
}

get_console() {
    parse_json "$1" || exit 1
    local response
    jq -n '{status:"error", error: "Operation not supported"}'
    exit 1
}

action=$1
parameters_file="$2"
wait_time="$3"

if [[ -z "$action" || -z "$parameters_file" ]]; then
    echo '{"status": "error", "error": "Missing required arguments"}'
    exit 1
fi

if [[ ! -r "$parameters_file" ]]; then
    echo '{"status": "error", "error": "File not found or unreadable"}'
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
    statuses)
        statuses "$parameters"
        ;;
    getconsole)
        get_console "$parameters"
        ;;
    *)
        echo '{"status": "error", "error": "Invalid action"}'
        exit 1
        ;;
esac

exit 0
