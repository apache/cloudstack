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
