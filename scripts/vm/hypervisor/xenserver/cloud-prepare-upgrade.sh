#!/bin/bash
# Version @VERSION@

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

# eject all CD


for vm in $(xe vbd-list type=CD empty=false | grep vm-uuid  | awk '{print $NF}')
do
  vmname=$(xe vm-param-get uuid=f873bb90-2e86-f7c5-364c-f315ecea826e param-name=name-label)
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

