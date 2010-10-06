#!/bin/bash

. /etc/rc.d/init.d/functions
source $(dirname $0)/hosts.properties

set -x



cleanup_xen_host(){
ssh root@$1 "for vm in \`xe vm-list | grep name-label | grep -v Control | awk '{print \$4}'\`; do uuid=\`xe vm-list name-label=\$vm | grep uuid | awk '{print \$5}'\`; echo \$uuid; xe vm-shutdown --force uuid=\$uuid; xe vm-destroy uuid=\$uuid; done"
ssh root@$1 "for vlan in \`xe vlan-list | grep uuid | awk '{print \$5}'\`; do echo \$vlan; xe vlan-destroy uuid=\$vlan; done"
ssh root@$1 "for vlan in \`xe network-list | grep name-label | grep VLAN| awk '{print \$4}'\`; do echo \$vlan; uuid=\`xe network-list name-label=\$vlan | grep uuid | awk '{print \$5}'\`; xe network-destroy uuid=\$uuid; done"
ssh root@$1 "for sr in \`xe sr-list type=nfs name-description=storage | grep uuid | awk '{print \$5}'\`; do pbd=\`xe pbd-list sr-uuid=\$sr | grep ^uuid | awk '{ print \$5}'\` ; echo \$pbd; xe pbd-unplug uuid=\$pbd; xe pbd-destroy uuid=\$pbd; xe sr-forget uuid=\$sr; done"
ssh root@$1 "for sr in \`xe sr-list type=nfs | grep uuid | awk '{print \$5}'\`; do pbd=\`xe pbd-list sr-uuid=\$sr | grep ^uuid | awk '{ print \$5}'\` ; echo \$pbd; xe pbd-unplug uuid=\$pbd; xe pbd-destroy uuid=\$pbd; xe sr-forget uuid=\$sr; done"
ssh root@$1 "sr=\`xe sr-list type=lvm | grep uuid | awk '{print \$5}'\`; for vdi in \`xe vdi-list sr-uuid=\$sr | grep ^uuid | awk '{ print \$5}'\` ; do echo \$vdi; xe vdi-destroy uuid=\$vdi; done"
}

cleanup_primary() {
  ssh root@nfs1.lab.vmops.com 'rm -vf /export/home/chiradeep/primary/*.vhd'
}

dir=$(dirname "$0")
if [ -f $dir/../deploy.properties ]; then
  . "$dir/../deploy.properties"
fi

#Kill vms and delete vlans on all computing hosts
for i in $COMPUTE
do
  cleanup_xen_host $i
done

cleanup_primary
