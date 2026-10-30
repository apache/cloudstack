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

# modifybrdr.sh -- Manage the per-network bridges of Direct Routed Networks
#
# One bridge per network, named brdr-<routed id>, without an uplink: the
# hypervisor routes all Instance traffic. The bridge carries the shared
# gateway addresses and a MAC derived from the routed id, so it is identical
# on every hypervisor and a migrated Instance keeps a valid neighbour cache.
#
# Every operation prints exactly one token on stdout, diagnostics go to stderr:
#   add:    the bridge name
#   delete: notmine | kept | deleted
#   query:  mine | notmine
#
# Usage:
#   modifybrdr.sh -o add    -n <routed id> [-4 <ipv4 gateway>] [-6 <ipv6 gateway>]
#   modifybrdr.sh -o delete -b <bridge name>
#   modifybrdr.sh -o query  -b <bridge name>

set -e

usage() {
    echo "Usage: $0 -o add -n <routed id> [-4 <ipv4 gateway>] [-6 <ipv6 gateway>] | -o delete -b <bridge name> | -o query -b <bridge name>" >&2
    exit 2
}

BRIDGE_NAME_RE='^brdr-[1-9][0-9]{0,9}$'

OP=
ROUTED_ID=
BRNAME=
IPV4_GATEWAY=
IPV6_GATEWAY=

while getopts 'o:n:b:4:6:' OPTION; do
    case "$OPTION" in
    o)    OP="$OPTARG" ;;
    n)    ROUTED_ID="$OPTARG" ;;
    b)    BRNAME="$OPTARG" ;;
    4)    IPV4_GATEWAY="$OPTARG" ;;
    6)    IPV6_GATEWAY="$OPTARG" ;;
    ?)    usage ;;
    esac
done

case "$OP" in
add)
    [[ "$ROUTED_ID" =~ ^[1-9][0-9]{0,9}$ ]] || usage
    [[ -n "$IPV4_GATEWAY" || -n "$IPV6_GATEWAY" ]] || usage
    BRNAME="brdr-${ROUTED_ID}"
    ;;
delete|query)
    [[ -n "$BRNAME" ]] || usage
    if [[ ! "$BRNAME" =~ $BRIDGE_NAME_RE ]]; then
        echo "notmine"
        exit 0
    fi
    if [[ "$OP" == "query" ]]; then
        echo "mine"
        exit 0
    fi
    ;;
*)
    usage
    ;;
esac

# Instances may only reach the host itself with ICMP; an IPv6 source must route back out of the bridge
removeFirewall() {
    iptables -w -D INPUT -i "$BRNAME" -p icmp -j ACCEPT 2>/dev/null || true
    iptables -w -D INPUT -i "$BRNAME" -j DROP 2>/dev/null || true
    ip6tables -w -D INPUT -i "$BRNAME" -p icmpv6 -j ACCEPT 2>/dev/null || true
    ip6tables -w -D INPUT -i "$BRNAME" -j DROP 2>/dev/null || true
    ip6tables -w -t raw -D PREROUTING -i "$BRNAME" -m rpfilter --invert -j DROP 2>/dev/null || true
}

addFirewall() {
    removeFirewall
    iptables -w -I INPUT -i "$BRNAME" -j DROP
    iptables -w -I INPUT -i "$BRNAME" -p icmp -j ACCEPT
    ip6tables -w -I INPUT -i "$BRNAME" -j DROP
    ip6tables -w -I INPUT -i "$BRNAME" -p icmpv6 -j ACCEPT
    ip6tables -w -t raw -I PREROUTING -i "$BRNAME" -m rpfilter --invert -j DROP
}

addBr() {
    addFirewall

    if [[ ! -d "/sys/class/net/${BRNAME}" ]]; then
        ip link add name "${BRNAME}" type bridge stp_state 0 forward_delay 0
    fi
    ip link set "${BRNAME}" up

    BRMAC=$(printf '0e:%010x' "${ROUTED_ID}" | sed -r 's/(..)(..)(..)(..)(..)$/\1:\2:\3:\4:\5/')
    ip link set dev "${BRNAME}" address "${BRMAC}"

    sysctl -qw "net.ipv4.conf.${BRNAME}.forwarding=1"
    sysctl -qw "net.ipv4.conf.${BRNAME}.arp_ignore=1"
    sysctl -qw "net.ipv4.conf.${BRNAME}.arp_announce=2"
    sysctl -qw "net.ipv4.conf.${BRNAME}.rp_filter=1"
    sysctl -qw "net.ipv6.conf.${BRNAME}.disable_ipv6=0"
    sysctl -qw "net.ipv6.conf.${BRNAME}.forwarding=1"
    sysctl -qw "net.ipv6.conf.${BRNAME}.accept_ra=0"

    if [[ -n "${IPV4_GATEWAY}" ]]; then
        ip address replace "${IPV4_GATEWAY}/32" dev "${BRNAME}"
    fi
    if [[ -n "${IPV6_GATEWAY}" ]]; then
        ip -6 address replace "${IPV6_GATEWAY}/64" dev "${BRNAME}"
    fi
}

deleteBr() {
    if [[ -d "/sys/class/net/${BRNAME}" ]]; then
        # Leave the bridge alone while an Instance is still attached to it
        if [[ -n "$(ls -A "/sys/class/net/${BRNAME}/brif" 2>/dev/null)" ]]; then
            echo "kept" >&3
            return 0
        fi
        ip link set "${BRNAME}" down
        ip link delete "${BRNAME}" type bridge
    fi

    removeFirewall
    echo "deleted" >&3
}

LOCKFILE=/var/run/cloud/brdr.lock
mkdir -p "${LOCKFILE%/*}"

(
    flock -x -w 10 200 || { echo "could not acquire ${LOCKFILE} within 10 seconds" >&2; exit 1; }

    # Only the result token goes to stdout (fd 3); command output goes to stderr
    exec 3>&1 1>&2

    case "$OP" in
    add)
        addBr
        echo "${BRNAME}" >&3
        ;;
    delete)
        deleteBr
        ;;
    esac
) 200>"${LOCKFILE}"
