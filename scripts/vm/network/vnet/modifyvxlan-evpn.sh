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

#
# Use BGP+EVPN for VXLAN with CloudStack instead of Multicast
#
# The default 'modifyvxlan.sh' script from CloudStack uses Multicast instead of EVPN for VXLAN
# In order to use this script and thus utilize BGP+EVPN, symlink this file:
#
# cd /usr/share
# ln -s cloudstack-common/scripts/vm/network/vnet/modifyvxlan-evpn.sh modifyvxlan.sh
#
#
# CloudStack will not handle the BGP configuration nor communication, the operator of the hypervisor will
# need to configure the properly.
#
# Frrouting is recommend to be used on the hypervisor to establish BGP sessions with upstream routers and
# exchange BGP+EVPN information.
#
# More information about BGP and EVPN with FRR: https://vincent.bernat.ch/en/blog/2017-vxlan-bgp-evpn
#

DSTPORT=4789

# We bind our VXLAN tunnel IP(v4) on Loopback device 'lo'
DEV="lo"

usage() {
    echo "Usage: $0: -o <op>(add | delete) -v <vxlan id> -p <pif> -b <bridge name> (-6)"
}

localAddr() {
    local FAMILY=$1

    if [[ -z "$FAMILY" || $FAMILY == "inet" ]]; then
       ip -4 -o addr show scope global dev ${DEV} | awk 'NR==1 {gsub("/[0-9]+", "") ; print $4}'
    fi

    if [[ "$FAMILY" == "inet6" ]]; then
       ip -6 -o addr show scope global dev ${DEV} | awk 'NR==1 {gsub("/[0-9]+", "") ; print $4}'
    fi
}

addVxlan() {
    local VNI=$1
    local PIF=$2
    local VXLAN_BR=$3
    local FAMILY=$4
    local VXLAN_DEV=vxlan${VNI}
    local ADDR=$(localAddr ${FAMILY})

    echo "local addr for VNI ${VNI} is ${ADDR}"

    if [[ ! -d /sys/class/net/${VXLAN_DEV} ]]; then
        ip -f ${FAMILY} link add ${VXLAN_DEV} type vxlan id ${VNI} local ${ADDR} dstport ${DSTPORT} nolearning
        ip link set ${VXLAN_DEV} up
        sysctl -qw net.ipv6.conf.${VXLAN_DEV}.disable_ipv6=1
    fi

    if [[ ! -d /sys/class/net/$VXLAN_BR ]]; then
        ip link add name ${VXLAN_BR} type bridge
        ip link set ${VXLAN_BR} up
        sysctl -qw net.ipv6.conf.${VXLAN_BR}.disable_ipv6=1
    fi

    bridge link show|grep ${VXLAN_BR}|awk '{print $2}'|grep "^${VXLAN_DEV}\$" > /dev/null
    if [[ $? -gt 0 ]]; then
        ip link set ${VXLAN_DEV} master ${VXLAN_BR}
    fi
}

deleteVxlan() {
    local VNI=$1
    local PIF=$2
    local VXLAN_BR=$3
    local FAMILY=$4
    local VXLAN_DEV=vxlan${VNI}

    ip link set ${VXLAN_DEV} nomaster
    ip link delete ${VXLAN_DEV}

    ip link set ${VXLAN_BR} down
    ip link delete ${VXLAN_BR} type bridge
}

OP=
VNI=
FAMILY=inet
option=$@

while getopts 'o:v:p:b:6' OPTION
do
  case $OPTION in
  o)    oflag=1
        OP="$OPTARG"
        ;;
  v)    vflag=1
        VNI="$OPTARG"
        ;;
  p)    pflag=1
        PIF="$OPTARG"
        ;;
  b)    bflag=1
        BRNAME="$OPTARG"
        ;;
  6)
        FAMILY=inet6
        ;;
  ?)    usage
        exit 2
        ;;
  esac
done

if [[ "$oflag$vflag$pflag$bflag" != "1111" ]]; then
    usage
    exit 2
fi

lsmod|grep ^vxlan >& /dev/null
if [[ $? -gt 0 ]]; then
    modprobe=`modprobe vxlan 2>&1`
    if [[ $? -gt 0 ]]; then
        echo "Failed to load vxlan kernel module: $modprobe"
        exit 1
    fi
fi


#
# Add a lockfile to prevent this script from running twice on the same host
# this can cause a race condition
#

LOCKFILE=/var/run/cloud/vxlan.lock

(
    flock -x -w 10 200 || exit 1
    if [[ "$OP" == "add" ]]; then
        addVxlan ${VNI} ${PIF} ${BRNAME} ${FAMILY}

        if [[ $? -gt 0 ]]; then
            exit 1
        fi
    elif [[ "$OP" == "delete" ]]; then
        deleteVxlan ${VNI} ${PIF} ${BRNAME} ${FAMILY}
    fi
) 200>${LOCKFILE}
