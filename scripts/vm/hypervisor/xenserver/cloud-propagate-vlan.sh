#!/bin/bash
# Copyright (C) 2011 Citrix Systems, Inc.  All rights reserved
#     
# This software is licensed under the GNU General Public License v3 or later.
# 
# It is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation, either version 3 of the License, or any later version.
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
# 
# You should have received a copy of the GNU General Public License
# along with this program.  If not, see <http://www.gnu.org/licenses/>.
# 
# Version @VERSION@

#set -x


for vlan_networkname in $(xe network-list | grep "name-label ( RW): VLAN" | awk '{print $NF}');
do
  vlan_network=$(xe network-list name-label=$vlan_networkname --minimal)
  this_tagged_pif=$(xe pif-list  network-uuid=$vlan_network --minimal | cut -d, -f 1)
  if [ -x $this_tagged_pif ]; then
    continue
  fi
  vlan=$(xe pif-param-get uuid=$this_tagged_pif param-name=VLAN)
  this_host=$(xe pif-param-get uuid=$this_tagged_pif param-name=host-uuid)
  this_device=$(xe pif-param-get uuid=$this_tagged_pif param-name=device)
  untagged_pif=$(xe pif-list host-uuid=$this_host device=$this_device VLAN=-1 --minimal)
  untagged_network=$(xe pif-param-get uuid=$untagged_pif param-name=network-uuid)
  for host in $(xe host-list | grep ^uuid | awk '{print $NF}')
  do
    tagpif=$(xe pif-list network-uuid=$vlan_network host-uuid=$host --minimal)
    if [ -z $tagpif ]; then
      pif=$(xe pif-list host-uuid=$host network_uuid=$untagged_network --minimal)
      xe vlan-create network-uuid=$vlan_network pif-uuid=$pif vlan=$vlan
    fi
  done
done
