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
    echo "$json_string" | jq '.' || { echo '{"error":"Invalid JSON input"}'; exit 1; }
}

generate_random_mac() {
    hexchars="0123456789ABCDEF"
    echo "52:54:00:$(for i in {1..3}; do echo -n ${hexchars:$(( RANDOM % 16 )):1}${hexchars:$(( RANDOM % 16 )):1}; [[ $i -lt 3 ]] && echo -n ':'; done)"
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

    local response
    response=$(jq -n \
        '{status: "success", message: "Resource created"}')

    echo "$response"
}

delete() {
    parse_json "$1" || exit 1

    local response
    response=$(jq -n \
        '{status: "success", message: "Resource deleted"}')

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
    echo '{"status": "success", "power_state": "running"}'
}

action=$1
parameters=$2
wait_time=$3

if [[ -z $action || -z $parameters ]]; then
    echo '{"error":"Missing required arguments"}'
    exit 1
fi

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
        prepare "$parameters" "$wait_time"
        ;;
    stop)
        create "$parameters" "$wait_time"
        ;;
    reboot)
        delete "$parameters" "$wait_time"
        ;;
    status)
        status "$parameters" "$wait_time"
        ;;
    *)
        echo '{"error":"Invalid action"}'
        exit 1
        ;;
esac

exit 0
