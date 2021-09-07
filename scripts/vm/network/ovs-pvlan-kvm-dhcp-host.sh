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

#!/bin/bash

# We're trying to do the impossible here by allowing pvlan on kvm / xen. As only God can do the impossible, and we've got too much ego to
# admit that we can't, we're just hacking our way around it.
# We're pretty much crafting two vlan headers, one with the primary vlan and the other with the secondary and with a few fancy rules
# it managed to work. But take note that the'res no checking over here for secondary vlan overlap. That has to be handled while
# creating the pvlan!!

exec 2>&1

usage() {
  printf "Usage: %s: (-A|-D) (-P/I/C) -b <bridge/switch> -p <primary vlan> -s <secondary vlan> -m <VM MAC> -d <DHCP IP> -h \n" $(basename $0) >&2
  exit 2
}

br=
pri_vlan=
sec_vlan=
vm_mac=
dhcp_ip=
op=
type=

while getopts 'ADPICb:p:s:m:d:h' OPTION
do
  case $OPTION in
  A)  op="add"
      ;;
  D)  op="del"
      ;;
  P)  type="P"
      ;;
  I)  type="I"
      ;;
  C)  type="C"
      ;;
  b)  br="$OPTARG"
      ;;
  p)  pri_vlan="$OPTARG"
      ;;
  s)  sec_vlan="$OPTARG"
      ;;
  m)  vm_mac="$OPTARG"
      ;;
  d)  dhcp_ip="$OPTARG"
      ;;
  h)  usage
      exit 1
      ;;
  esac
done

if [ -z "$op" ]
then
    echo Missing operation pararmeter!
    exit 1
fi

if [ -z "$type" ]
then
    echo Missing pvlan type pararmeter!
    exit 1
fi

if [ -z "$br" ]
then
    echo Missing parameter bridge!
    exit 1
fi

if [ -z "$vm_mac" ]
then
    echo Missing parameter VM MAC!
    exit 1
fi

if [ -z "$pri_vlan" ]
then
    echo Missing parameter primary vlan!
    exit 1
fi

if [ -z "$sec_vlan" ]
then
    echo Missing parameter secondary vlan!
    exit 1
fi

if [ -z "$dhcp_ip" ]
then
    echo Missing parameter DHCP IP!
    exit 1
fi

find_port() {
  mac=`echo "$1" | sed -e 's/:/\\\:/g'`
  port=`ovs-vsctl --column ofport find interface external_ids:attached-mac="$mac" | tr -d ' ' | cut -d ':' -f 2`
  echo $port
}

ovs-vsctl set bridge $br protocols=OpenFlow10,OpenFlow11,OpenFlow12,OpenFlow13
ovs-vsctl set Open_vSwitch . other_config:vlan-limit=2

if [ "$op" == "add" ]
then
    dhcp_port=$(find_port $vm_mac)

    ovs-ofctl add-flow $br table=0,priority=200,arp,dl_vlan=$pri_vlan,nw_dst=$dhcp_ip,actions=strip_vlan,resubmit\(,1\)
    ovs-ofctl add-flow $br table=1,priority=200,arp,dl_vlan=$sec_vlan,nw_dst=$dhcp_ip,actions=strip_vlan,output:$dhcp_port

    ovs-ofctl add-flow $br table=0,priority=100,udp,dl_vlan=$pri_vlan,nw_dst=255.255.255.255,tp_dst=67,actions=strip_vlan,resubmit\(,1\)
    ovs-ofctl add-flow $br table=1,priority=100,udp,dl_vlan=$sec_vlan,nw_dst=255.255.255.255,tp_dst=67,actions=strip_vlan,output:$dhcp_port
else
    ovs-ofctl del-flows --strict $br table=0,priority=200,arp,dl_vlan=$pri_vlan,nw_dst=$dhcp_ip
    ovs-ofctl del-flows --strict $br table=1,priority=200,arp,dl_vlan=$sec_vlan,nw_dst=$dhcp_ip

    ovs-ofctl del-flows --strict $br table=0,priority=100,udp,dl_vlan=$pri_vlan,nw_dst=255.255.255.255,tp_dst=67
    ovs-ofctl del-flows --strict $br table=1,priority=100,udp,dl_vlan=$sec_vlan,nw_dst=255.255.255.255,tp_dst=67
fi
