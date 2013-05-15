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

usage() {
  printf "Usage: %s: (-A|-D) -b <bridge/switch> -p <primary vlan> -i <secondary isolated vlan> -n <DHCP server name> -d <DHCP server IP> -m <DHCP server MAC> -I <interface> -v <VM MAC> -h \n" $(basename $0) >&2
  exit 2
}

br=
pri_vlan=
sec_iso_vlan=
dhcp_name=
dhcp_ip=
dhcp_mac=
vm_mac=
iface=
op=

while getopts 'ADb:p:i:d:m:v:n:I:h' OPTION
do
  case $OPTION in
  A)  op="add"
      ;;
  D)  op="del"
      ;;
  b)  br="$OPTARG"
      ;;
  p)  pri_vlan="$OPTARG"
      ;;
  i)  sec_iso_vlan="$OPTARG"
      ;;
  n)  dhcp_name="$OPTARG"
      ;;
  d)  dhcp_ip="$OPTARG"
      ;;
  m)  dhcp_mac="$OPTARG"
      ;;
  I)  iface="$OPTARG"
      ;;
  v)  vm_mac="$OPTARG"
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

if [ -z "$br" ]
then
    echo Missing parameter bridge!
    exit 1
fi

if [ -z "$pri_vlan" ]
then
    echo Missing parameter primary vlan!
    exit 1
fi

if [ -z "$sec_iso_vlan" ]
then
    echo Missing parameter secondary isolate vlan!
    exit 1
fi

if [ -z "$dhcp_name" ]
then
    echo Missing parameter DHCP NAME!
    exit 1
fi

if [ -z "$dhcp_ip" ]
then
    echo Missing parameter DHCP IP!
    exit 1
fi

if [ -z "$dhcp_mac" ]
then
    echo Missing parameter DHCP MAC!
    exit 1
fi

if [ "$op" == "add" -a -z "$iface" ]
then
    echo Missing parameter DHCP VM interface!
    exit 1
fi

if [ "$op" == "add" ]
then
    dhcp_port=`ovs-ofctl show $br | grep $iface | cut -d '(' -f 1|tr -d ' '`
    ovs-ofctl add-flow $br priority=200,arp,dl_vlan=$sec_iso_vlan,nw_dst=$dhcp_ip,actions=strip_vlan,output:$dhcp_port
    ovs-ofctl add-flow $br priority=150,dl_vlan=$sec_iso_vlan,dl_dst=$dhcp_mac,actions=strip_vlan,output:$dhcp_port
    ovs-ofctl add-flow $br priority=100,udp,dl_vlan=$sec_iso_vlan,nw_dst=255.255.255.255,tp_dst=67,actions=strip_vlan,output:$dhcp_port
else
    ovs-ofctl del-flows --strict $br priority=200,arp,dl_vlan=$sec_iso_vlan,nw_dst=$dhcp_ip
    ovs-ofctl del-flows --strict $br priority=150,dl_vlan=$sec_iso_vlan,dl_dst=$dhcp_mac
    ovs-ofctl del-flows --strict $br priority=100,udp,dl_vlan=$sec_iso_vlan,nw_dst=255.255.255.255,tp_dst=67
fi
