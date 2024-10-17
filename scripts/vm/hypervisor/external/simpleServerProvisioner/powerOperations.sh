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
    declare -A arguments
    while IFS= read -r line; do
        key=$(echo "$line" | awk '{print $1}')
        value=$(echo "$line" | awk '{print $2}')
        arguments["$key"]="$value"
    done < <(echo "$json_string" | jq -r 'to_entries | .[] | "\(.key) \(.value)"')

    echo "${arguments[@]}"
}

start() {
    parsed_arguments=$(parse_json "$1")
    # Add code to handle start instance logic
}

stop() {
    parsed_arguments=$(parse_json "$1")
    # Add code to handle stop instance logic
}

reboot() {
    parsed_arguments=$(parse_json "$1")
    # Add code to handle reboot instance logic
}

status() {
    parsed_arguments=$(parse_json "$1")
    # Add code to handle get power status of instance logic
}

action=$1
parameters=$2
wait_time=$3

if [[ -z $action || -z $parameters || -z $wait_time ]]; then
    exit 1
fi

case $action in
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
        exit 1
        ;;
esac

exit 0
