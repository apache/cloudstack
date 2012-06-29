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

# propagate VLANs to other host

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
      pif=$(xe pif-list host-uuid=$host network-uuid=$untagged_network --minimal)
      xe vlan-create network-uuid=$vlan_network pif-uuid=$pif vlan=$vlan
    fi
  done
done



# propagates local link network
local_networkname="cloud_link_local_network"
local_network=$(xe network-list name-label=$local_networkname --minimal)
for dom0 in $(xe vm-list is-control-domain=true | grep ^uuid | awk '{print $NF}')
do
  local_vif=$(xe vif-list vm-uuid=$dom0 network-name-label=$local_networkname | grep ^uuid | awk '{print $NF}')
  if [ -z $local_vif ]; then
      local_vif=$(xe vif-create network-uuid=$local_network vm-uuid=$dom0 device=0 mac=fe:ff:ff:ff:ff:ff)
      xe vif-param-set uuid=$local_vif other-config:nameLabel=link_local_network_vif
      xe vif-plug uuid=$local_vif
  fi
done


# eject all CD


for vmname in $(xe vbd-list type=CD empty=false | grep vm-name-label | awk '{print $NF}')
do
  xe vm-cd-eject vm=$vmname
  if [ $? -ne 0 ]; then
    echo "Need to eject CD for VM $vmname"
  fi
done

# fake PV for PV VM

fake_pv_driver() {
  local vm=$1
  res=$(xe vm-param-get uuid=$vm param-name=PV-drivers-version)
  if [ ! "$res" = "<not in database>" ]; then
    return 1
  fi
  res=$(xe vm-param-get uuid=$vm param-name=HVM-boot-policy)
  if [ ! -z $res ]; then
    echo "Warning VM $vm is HVM, but PV driver is not installed, you may need to stop it manually"
    return 0
  fi
  host=$(xe vm-param-get uuid=$vm param-name=resident-on)
  xe host-call-plugin host-uuid=$host plugin=vmops fn=preparemigration args:uuid=$vm
}

vms=$(xe vm-list is-control-domain=false| grep ^uuid | awk '{print $NF}')
for vm in $vms
do  
  state=$(xe vm-param-get uuid=$vm param-name=power-state)
  if [ $state = "running" ]; then
    fake_pv_driver $vm
  elif [ $state = "halted" ]; then
    echo "VM $vm is in $state"
  else
    echo "Warning : Don't know how to handle VM $vm, it is in $state state"
  fi
done

