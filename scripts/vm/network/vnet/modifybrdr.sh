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

# modifybrdr.sh -- Manage per-network bridges for Direct Routed Networks
#
# One bridge is created per network, named brdr-<network id>
# (Bridge-DirectRouted), using the network's numeric database id. The bridge
# carries the shared, host-independent gateway addresses and is identical on
# every hypervisor, so an Instance needs no reconfiguration when it migrates.
#
# The bridge never has a physical uplink: all Instance traffic is routed by
# the hypervisor, not bridged. Separate bridges are what keep networks
# isolated from one another at layer 2, and they give the operator a named
# interface per network to hang local routing policy off.
#
# The gateway addresses are always passed in by the CloudStack agent, taken
# from the Instance's NIC; this script carries no defaults of its own, so the
# addresses are defined in exactly one place (NetUtils on the management
# server).
#
# This script is the only place that knows how these bridges are named. 'add'
# prints the bridge name on stdout for the agent to use, and 'delete' takes a
# bridge name and answers on stdout:
#   notmine  - not a bridge this script manages; the agent falls back to its
#              regular unplug handling
#   kept     - ours, but other Instances still use it
#   deleted  - ours, removed
#
# Usage:
#   add:    modifybrdr.sh -o add    -n <network id> -4 <ipv4 gateway> and/or -6 <ipv6 gateway>
#   delete: modifybrdr.sh -o delete -b <bridge name>

usage() {
    echo "Usage: $0 -o add -n <network id> [-4 <ipv4 gateway>] [-6 <ipv6 gateway>] | -o delete -b <bridge name>"
}

OP=
NETWORK_ID=
BRNAME=
IPV4_GATEWAY=
IPV6_GATEWAY=

while getopts 'o:n:b:4:6:' OPTION; do
    case $OPTION in
    o)    oflag=1
          OP="$OPTARG"
          ;;
    n)    NETWORK_ID="$OPTARG"
          ;;
    b)    BRNAME="$OPTARG"
          ;;
    4)    IPV4_GATEWAY="$OPTARG"
          ;;
    6)    IPV6_GATEWAY="$OPTARG"
          ;;
    ?)    usage
          exit 2
          ;;
    esac
done

if [[ "$oflag" != "1" ]]; then
    usage
    exit 2
fi

if [[ "$OP" == "add" ]]; then
    if [[ ! "$NETWORK_ID" =~ ^[0-9]+$ ]]; then
        echo "Network id must be numeric: ${NETWORK_ID}"
        exit 2
    fi

    if [[ -z "$IPV4_GATEWAY" && -z "$IPV6_GATEWAY" ]]; then
        echo "At least one of -4 or -6 must be given for add"
        usage
        exit 2
    fi

    BRNAME="brdr-${NETWORK_ID}"

    # Linux caps interface names at 15 characters
    if [[ ${#BRNAME} -gt 15 ]]; then
        echo "Bridge name ${BRNAME} exceeds the 15 character interface name limit"
        exit 2
    fi
elif [[ "$OP" == "delete" ]]; then
    if [[ -z "$BRNAME" ]]; then
        usage
        exit 2
    fi

    # Not one of ours: tell the agent so it can fall back to its regular
    # unplug handling. This is what keeps the bridge naming knowledge in
    # this script and nowhere else.
    if [[ "$BRNAME" != brdr-* ]]; then
        echo "notmine"
        exit 0
    fi
else
    usage
    exit 2
fi

addBr() {
    if [[ ! -d /sys/class/net/${BRNAME} ]]; then
        # No STP and no forwarding delay: the bridge has no uplink, so there is
        # no loop to detect and no reason to hold ports down when an Instance starts
        ip link add name ${BRNAME} type bridge stp_state 0 forward_delay 0
        ip link set ${BRNAME} up
    fi

    # The bridge routes on behalf of every Instance attached to it
    sysctl -qw net.ipv4.conf.${BRNAME}.forwarding=1
    sysctl -qw net.ipv6.conf.${BRNAME}.disable_ipv6=0
    sysctl -qw net.ipv6.conf.${BRNAME}.forwarding=1

    # Never act on a router advertisement sent by an Instance
    sysctl -qw net.ipv6.conf.${BRNAME}.accept_ra=0

    # The same gateway address is configured on every brdr bridge on this host.
    # Only answer ARP for the address on the interface the request arrived on,
    # and always source ARP from that interface's own address.
    sysctl -qw net.ipv4.conf.${BRNAME}.arp_ignore=1
    sysctl -qw net.ipv4.conf.${BRNAME}.arp_announce=2

    if [[ -n "${IPV4_GATEWAY}" ]]; then
        ip address replace ${IPV4_GATEWAY}/32 dev ${BRNAME}
    fi
    if [[ -n "${IPV6_GATEWAY}" ]]; then
        ip -6 address replace ${IPV6_GATEWAY}/64 dev ${BRNAME}
    fi

    # Strict reverse path filtering: an Instance may only send from an address
    # that is routed back out of this bridge, which is its own /32. Set on the
    # bridge only, never on 'all', so no other interface changes behaviour.
    # Note this is IPv4 only; the kernel has no IPv6 equivalent.
    sysctl -qw net.ipv4.conf.${BRNAME}.rp_filter=1

}

deleteBr() {
    if [[ ! -d /sys/class/net/${BRNAME} ]]; then
        echo "deleted"
        return 0
    fi

    # An Instance may have been started on this network while the last one was
    # being stopped; leave the bridge alone if anything is still attached
    if [[ -n "$(ls -A /sys/class/net/${BRNAME}/brif 2>/dev/null)" ]]; then
        echo "kept"
        return 0
    fi

    ip link set ${BRNAME} down
    ip link delete ${BRNAME} type bridge
    echo "deleted"
}

#
# Add a lockfile to prevent this script from running twice on the same host
# this can cause a race condition
#

LOCKFILE=/var/run/cloud/brdr.lock

# ensures that parent directories exists and prepares the lock file
mkdir -p "${LOCKFILE%/*}"

(
    flock -x -w 10 200 || exit 1
    if [[ "$OP" == "add" ]]; then
        addBr

        if [[ $? -gt 0 ]]; then
            exit 1
        fi

        # The agent uses this as the bridge name; naming lives here only
        echo "${BRNAME}"
    elif [[ "$OP" == "delete" ]]; then
        deleteBr
    fi
) 200>${LOCKFILE}
