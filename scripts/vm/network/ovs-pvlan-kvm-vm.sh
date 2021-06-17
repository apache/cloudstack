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
  printf "Usage: %s: (-A|-D) (-P/I/C) -b <bridge/switch> -p <primary vlan> -s <secondary vlan> -m <VM MAC> -h \n" $(basename $0) >&2
  exit 2
}

br=
pri_vlan=
sec_vlan=
vm_mac=
op=
type=

while getopts 'ADPICb:p:s:m:h' OPTION
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

find_port() {
  mac=`echo "$1" | sed -e 's/:/\\\:/g'`
  port=`ovs-vsctl --column ofport find interface external_ids:attached-mac="$mac" | tr -d ' ' | cut -d ':' -f 2`
  echo $port
}

find_port_group() {
  ovs-ofctl -O OpenFlow13 dump-groups $br | grep group_id=$1, | sed -e 's/.*type=all,//g' -e 's/bucket=actions=//g' -e 's/resubmit(,1)//g' -e 's/strip_vlan,//g' -e 's/pop_vlan,//g' -e 's/output://g' -e 's/^,//g' -e 's/,$//g' -e 's/,,/,/g' -e 's/ //g'
}

# try to find the physical link to outside, only supports eth and em prefix now
trunk_port=`ovs-ofctl show $br | egrep "\((eth|em)[0-9]" | cut -d '(' -f 1|tr -d ' '`
vm_port=$(find_port $vm_mac)

# craft the vlan headers. Adding 4096 as in hex, it must be of the form 0x1XXX
pri_vlan_header=$((4096 + $pri_vlan))
sec_vlan_header=$((4096 + $sec_vlan))

# Get the groups for broadcast. Ensure we end the group id with ',' so that we wont accidentally match groupid 111 with 1110.
# We're using the header value for the pri vlan port group, as anything from a promiscuous device has to go to every device in the vlan.
# Since we're creating a separate group for just the promiscuous devices, adding 4096 so that it'll be unique. Hence we're restricted to 4096 vlans!
# Not a big deal because if you have vxlan, why do you even need pvlan!!
pri_vlan_ports=$(find_port_group $pri_vlan_header)
sec_vlan_ports=$(find_port_group $sec_vlan)

add_to_ports() {
  if [ -z "$1" ]
  then
    # To ensure that we don't get trailing commas
    echo "$2"
  else
    # Dont add it if it already exists
    echo "$1" | grep -w -q "$2" && echo "$1" && return
    echo "$2,$1"
  fi
}

del_from_ports() {
  # Delete when only, begining, middle and end of string
  echo "$1" | sed -e "s/^$2$//g" -e "s/^$2,//g" -e "s/,$2$//g" -e "s/,$2,/,/g"
}

mod_group() {
  # Ensure that we don't delete the prom port group, because if we do, the rules that have it go away!
  actions=`echo "$2" | sed -e 's/,/,bucket=actions=/g'`
  if [ "$1" == "$pri_vlan" ]
  then
    actions=`echo "$2" | sed -e 's/,/,bucket=actions=strip_vlan,output:/g'`
    if [ -z "$2" ]
    then
      ovs-ofctl -O OpenFlow13 mod-group --may-create $br group_id=$1,type=all,bucket=resubmit\(,1\)
    else
      ovs-ofctl -O OpenFlow13 mod-group --may-create $br group_id=$1,type=all,bucket=resubmit\(,1\),bucket=actions=strip_vlan,output:$actions
    fi
    return
  fi
  if [ -z "$2" ]
  then
    ovs-ofctl -O OpenFlow13 del-groups $br group_id=$1
  else
    ovs-ofctl -O OpenFlow13 mod-group --may-create $br group_id=$1,type=all,bucket=actions=$actions
  fi
}

cleanup_flows() {
  ovs-ofctl del-flows $br --strict table=0,priority=70,dl_vlan=$pri_vlan,dl_dst=ff:ff:ff:ff:ff:ff
  ovs-ofctl del-flows $br --strict table=1,priority=70,dl_vlan=$pri_vlan,dl_dst=ff:ff:ff:ff:ff:ff
  ovs-ofctl -O OpenFlow13 del-groups $br group_id=$pri_vlan
}

# Allow the neccessary protocols and QinQ
ovs-vsctl set bridge $br protocols=OpenFlow10,OpenFlow11,OpenFlow12,OpenFlow13
ovs-vsctl set Open_vSwitch . other_config:vlan-limit=2

# So that we're friendly to non pvlan devices
ovs-ofctl add-flow $br priority=0,actions=NORMAL

if [ "$op" == "add" ]
then
  # From our pri vlan
  if [ "$type" == "P" ]
  then
    ovs-ofctl add-flow $br table=0,priority=70,dl_vlan=$pri_vlan,dl_dst=$vm_mac,actions=strip_vlan,strip_vlan,output:$vm_port
  else
    ovs-ofctl add-flow $br table=0,priority=70,dl_vlan=$pri_vlan,dl_dst=$vm_mac,actions=strip_vlan,resubmit\(,1\)
  fi

  # Accept from promiscuous
  ovs-ofctl add-flow $br table=1,priority=70,dl_vlan=$pri_vlan,dl_dst=$vm_mac,actions=strip_vlan,output:$vm_port
  # From others in our own community
  if [ "$type" == "C" ]
  then
    ovs-ofctl add-flow $br table=1,priority=70,dl_vlan=$sec_vlan,dl_dst=$vm_mac,actions=strip_vlan,output:$vm_port
  fi
  # Allow only dhcp to isolated vm
  if [ "$type" == "I" ]
  then
    ovs-ofctl add-flow $br table=1,priority=70,udp,dl_vlan=$sec_vlan,dl_dst=$vm_mac,tp_src=67,actions=strip_vlan,output:$vm_port
  fi

  # Security101
  ovs-ofctl add-flow $br table=1,priority=0,actions=drop

  # If the dest isn't on our switch send it out
  ovs-ofctl add-flow $br table=0,priority=60,dl_vlan=$pri_vlan,dl_src=$vm_mac,actions=output:$trunk_port
  # QinQ the packet. Outter header is the primary vlan and inner is the secondary
  ovs-ofctl add-flow -O OpenFlow13 $br table=0,priority=50,vlan_tci=0x0000,dl_src=$vm_mac,actions=push_vlan:0x8100,set_field:$sec_vlan_header-\>vlan_vid,push_vlan:0x8100,set_field:$pri_vlan_header-\>vlan_vid,resubmit:$trunk_port

  # BROADCASTS
  # Create the respective groups
  # pri_vlan_ports are the list of ports of all iso & comm dev for a give pvlan
  if [ "$type" != "P" ]
  then
    pri_vlan_ports=$(add_to_ports "$pri_vlan_ports" "$vm_port")
    mod_group $pri_vlan_header $pri_vlan_ports
  fi
  # sec_vlan_ports are the list of ports for a given secondary pvlan
  sec_vlan_ports=$(add_to_ports "$sec_vlan_ports" "$vm_port")
  mod_group $sec_vlan $sec_vlan_ports

  # Ensure we have the promiscuous port group because if we don't, it'll fail to create the following rule
  prom_ports=$(find_port_group $pri_vlan)
  mod_group $pri_vlan $prom_ports

  # From a device on this switch. Pass it to the trunk port and process it ourselves for other devices on the switch.
  ovs-ofctl add-flow $br table=0,priority=300,dl_vlan=$pri_vlan,dl_src=$vm_mac,dl_dst=ff:ff:ff:ff:ff:ff,actions=output:$trunk_port,strip_vlan,group:$pri_vlan
  # Got a packet from the trunk port from out pri vlan, pass it to pri_vlan_group which sends the packet out to the promiscuous devices as well as passes it onto table 1
  ovs-ofctl add-flow $br table=0,priority=70,dl_vlan=$pri_vlan,dl_dst=ff:ff:ff:ff:ff:ff,actions=strip_vlan,group:$pri_vlan
  # From a promiscuous device, so send it to all community and isolated devices on this switch. Passed to all promiscuous devices in the prior step ^^
  ovs-ofctl add-flow $br table=1,priority=70,dl_vlan=$pri_vlan,dl_dst=ff:ff:ff:ff:ff:ff,actions=strip_vlan,group:$pri_vlan_header
  # Since it's from a community, gotta braodcast it to all community devices
  if [ "$type" == "C" ]
  then
    ovs-ofctl add-flow $br table=1,priority=70,dl_vlan=$sec_vlan,dl_dst=ff:ff:ff:ff:ff:ff,actions=strip_vlan,group:$sec_vlan
  fi
  # Allow only dhcp form isolated router to isolated vm
  if [ "$type" == "I" ]
  then
    ovs-ofctl add-flow $br table=1,priority=70,udp,dl_vlan=$sec_vlan,tp_src=67,dl_dst=ff:ff:ff:ff:ff:ff,actions=strip_vlan,group:$sec_vlan
  fi

else
  # Delete whatever we've added that's vm specific
  ovs-ofctl del-flows $br --strict table=0,priority=70,dl_vlan=$pri_vlan,dl_dst=$vm_mac

  # Need to ge the vmport from the rules as it's already been removed from the switch
  vm_port=`ovs-ofctl dump-flows $br | grep "priority=70,dl_vlan=$pri_vlan,dl_dst=$vm_mac" | tr ':' '\n' | tail -n 1`
  ovs-ofctl del-flows $br --strict table=1,priority=70,dl_vlan=$pri_vlan,dl_dst=$vm_mac
  if [ "$type" == "C" ]
  then
    ovs-ofctl del-flows $br --strict table=1,priority=70,dl_vlan=$sec_vlan,dl_dst=$vm_mac
  fi
  if [ "$type" == "I" ]
  then
    ovs-ofctl del-flows $br --strict table=1,priority=70,udp,dl_vlan=$sec_vlan,dl_dst=$vm_mac,tp_src=67
  fi

  ovs-ofctl del-flows $br --strict table=0,priority=60,dl_vlan=$pri_vlan,dl_src=$vm_mac
  ovs-ofctl del-flows $br --strict table=0,priority=50,vlan_tci=0x0000,dl_src=$vm_mac
  # For some ovs versions
  ovs-ofctl del-flows $br --strict table=0,priority=50,vlan_tci=0x0000/0x1fff,dl_src=$vm_mac

  # Remove the port from the groups
  pri_vlan_ports=$(del_from_ports "$pri_vlan_ports" "$vm_port")
  mod_group $pri_vlan_header $pri_vlan_ports
  sec_vlan_ports=$(del_from_ports "$sec_vlan_ports" "$vm_port")
  mod_group $sec_vlan $sec_vlan_ports

  # Remove vm specific rules
  ovs-ofctl del-flows $br --strict table=0,priority=300,dl_vlan=$pri_vlan,dl_src=$vm_mac,dl_dst=ff:ff:ff:ff:ff:ff

  # If the vm is going to be migrated but not yet removed. Remove the rules if it's the only vm in the vlan
  res1=`ovs-vsctl --column _uuid find port tag=$pri_vlan | wc -l`
  res2=`find_port $vm_mac | wc -l`
  if [ "$res1" -eq 1 ] && [ "$res2" -eq 1 ]
  then
    cleanup_flows
  fi

  # If no more vms exist on this host, clear up all the rules
  result=`ovs-vsctl find port tag=$pri_vlan`
  if [ -z "$result" ]
  then
    cleanup_flows
  fi

fi
