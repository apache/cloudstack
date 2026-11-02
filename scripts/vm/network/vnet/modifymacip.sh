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

# modifymacip.sh -- Manage static ARP/NDP entries and host routes for VM NICs
#
# Usage:
#   add:    modifymacip.sh -o add    -b <bridge> -m <mac> [-4 <ipv4>] ... [-6 <ipv6>] ...
#   delete: modifymacip.sh -o delete -b <bridge> -m <mac>
#
# Both -4 and -6 may be specified multiple times to cover primary and secondary
# addresses (e.g. link-local + global unicast for IPv6).
# On delete the bridge neighbour table is queried for all entries matching the
# MAC address; no separate state file is required.

usage() {
    echo "Usage: $0 -o <add|delete> -b <bridge> -m <mac> [-4 <ipv4>] ... [-6 <ipv6>] ..."
}

OP=
BRIDGE=
MAC=
IPV4_LIST=()
IPV6_LIST=()

while getopts 'o:b:m:4:6:' OPTION; do
    case $OPTION in
    o) OP="$OPTARG" ;;
    b) BRIDGE="$OPTARG" ;;
    m) MAC="$OPTARG" ;;
    4) IPV4_LIST+=("$OPTARG") ;;
    6) IPV6_LIST+=("$OPTARG") ;;
    ?) usage; exit 2 ;;
    esac
done

if [[ -z "$OP" || -z "$BRIDGE" || -z "$MAC" ]]; then
    usage
    exit 2
fi

add_entries() {
    for addr in "${IPV4_LIST[@]}"; do
        ip neigh replace "${addr}" lladdr "${MAC}" dev "${BRIDGE}" nud permanent
        ip route replace "${addr}/32" dev "${BRIDGE}"
    done

    if [[ "${#IPV6_LIST[@]}" -gt 0 ]]; then
        # Ensure IPv6 is enabled on the bridge before installing NDP entries
        sysctl -qw "net.ipv6.conf.${BRIDGE}.disable_ipv6=0"
        for addr in "${IPV6_LIST[@]}"; do
            ip -6 neigh replace "${addr}" lladdr "${MAC}" dev "${BRIDGE}" nud permanent
            ip -6 route replace "${addr}/128" dev "${BRIDGE}"
        done
    fi
}

delete_entries() {
    # Find all IPv4 neighbour entries on the bridge matching this MAC and remove them
    while read -r addr; do
        ip neigh del "${addr}" dev "${BRIDGE}" 2>/dev/null || true
        ip route del "${addr}/32" dev "${BRIDGE}" 2>/dev/null || true
    done < <(ip neigh show dev "${BRIDGE}" | awk -v mac="${MAC}" 'tolower($3) == tolower(mac) {print $1}')

    # Find all IPv6 neighbour entries on the bridge matching this MAC and remove them
    while read -r addr; do
        ip -6 neigh del "${addr}" dev "${BRIDGE}" 2>/dev/null || true
        ip -6 route del "${addr}/128" dev "${BRIDGE}" 2>/dev/null || true
    done < <(ip -6 neigh show dev "${BRIDGE}" | awk -v mac="${MAC}" 'tolower($3) == tolower(mac) {print $1}')
}

case "$OP" in
    add)    add_entries ;;
    delete) delete_entries ;;
    *)      usage; exit 2 ;;
esac
