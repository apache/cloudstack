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

usage() {
  printf "Usage: %s: (-A|-D) -b <bridge/switch> -p <primary vlan> -i <secondary isolated vlan> -d <DHCP server IP> -m <DHCP server MAC> -v <VM MAC> -h \n" $(basename $0) >&2
  exit 2
}

br=
pri_vlan=
sec_iso_vlan=
dhcp_ip=
dhcp_mac=
vm_mac=
op=

while getopts 'ADb:p:i:d:m:v:h' OPTION
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
  d)  dhcp_ip="$OPTARG"
      ;;
  m)  dhcp_mac="$OPTARG"
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

if [ -z "$vm_mac" ]
then
    echo Missing parameter VM MAC!
    exit 1
fi

if [ -z "$dhcp_mac" ]
then
    echo Missing parameter DHCP MAC!
    exit 1
fi

if [ "$op" == "add" ]
then
    ovs-ofctl add-flow $br priority=120,dl_src=$vm_mac,dl_dst=$dhcp_mac,actions=NORMAL
    ovs-ofctl add-flow $br priority=80,udp,dl_src=$vm_mac,nw_dst=255.255.255.255,tp_dst=67,actions=NORMAL
else
    ovs-ofctl del-flows --strict $br priority=120,dl_src=$vm_mac,dl_dst=$dhcp_mac
    ovs-ofctl del-flows --strict $br priority=80,udp,dl_src=$vm_mac,nw_dst=255.255.255.255,tp_dst=67
fi
