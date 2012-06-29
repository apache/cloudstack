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



 


. /etc/rc.d/init.d/functions

set -x

kill_server() {
ssh root@$1 "service cloud-management stop && service cloud-usage stop 2>&1 &"
}



cleanup_server() {
  kill_server $1 $2
t=$(date  +"%h%d_%H_%M_%S")
ssh root@$1 "mkdir $2/logs.$t; mv $2/management-server.log $2/logs.$t/management-server.log.$t  "
ssh root@$1 "mkdir $2/logs.$t; mv $2/catalina.out $2/logs.$t/catalina.out.$t"
  return 0
}


cleanup_linux(){
ssh root@$1 "for vm in \`xe vm-list | grep name-label | grep -v Control | awk '{print \$4}'\`; do uuid=\`xe vm-list name-label=\$vm | grep uuid | awk '{print \$5}'\`; echo \$uuid; xe vm-shutdown --force uuid=\$uuid; xe vm-destroy uuid=\$uuid; done"

ssh root@$1 "for vlan in \`xe vlan-list | grep uuid | awk '{print \$5}'\`; do pif=\`xe pif-list VLAN=\$vlan | grep ^uuid | awk '{print \$5}'\`; xe pif-unplug uuid=\$pif;  xe vlan-destroy uuid=\$vlan; done"

ssh root@$1 "for vlan in \`xe network-list | grep name-label | grep VLAN| awk '{print \$4}'\`; do echo \$vlan; uuid=\`xe network-list name-label=\$vlan | grep uuid | awk '{print \$5}'\`; xe network-destroy uuid=\$uuid; done"

ssh root@$1 "for sr in \`xe sr-list type=nfs name-description=storage | grep uuid | awk '{print \$5}'\`; do pbd=\`xe pbd-list sr-uuid=\$sr | grep ^uuid | awk '{ print \$5}'\` ; echo \$pbd; xe pbd-unplug uuid=\$pbd; xe pbd-destroy uuid=\$pbd; xe sr-forget uuid=\$sr; done"

ssh root@$1 "for sr in \`xe sr-list type=iso name-description=iso | grep uuid | awk '{print \$5}'\`; do echo \$sr; for vdi in \`xe vdi-list sr-uuid=\$sr | grep ^uuid | awk '{ print \$5}'\` ; do echo \$vdi; xe vdi-destroy uuid=\$vdi; done; for pbd in \`xe pbd-list sr-uuid=\$sr | grep ^uuid | awk '{ print \$5}'\` ; do echo \$pbd; xe pbd-unplug uuid=\$pbd; xe pbd-destroy uuid=\$pbd; done; done"

ssh root@$1 "xentoolsiso=\$(xe sr-list name-label='XenServer Tools' | grep uuid | awk '{print \$5}'); for sr in \`xe sr-list type=iso content-type=iso | grep uuid | awk '{print \$5}' | grep -v \$xentoolsiso \`; do echo \$sr; for vdi in \`xe vdi-list sr-uuid=\$sr | grep ^uuid | awk '{ print \$5}'\` ; do echo \$vdi; xe vdi-destroy uuid=\$vdi; done; for pbd in \`xe pbd-list sr-uuid=\$sr | grep ^uuid | awk '{ print \$5}'\` ; do echo \$pbd; xe pbd-unplug uuid=\$pbd; xe pbd-destroy uuid=\$pbd; done;  done"

ssh root@$1 "for sr in \`xe sr-list type=nfs | grep uuid | awk '{print \$5}'\`; do echo \$sr;  for pbd in \`xe pbd-list sr-uuid=\$sr | grep ^uuid | awk '{ print \$5}'\` ; do echo \$pbd; xe pbd-unplug uuid=\$pbd; xe pbd-destroy uuid=\$pbd; done; xe sr-forget uuid=\$sr; done"

ssh root@$1 "for sr in \`xe sr-list type=lvmoiscsi | grep uuid | awk '{print \$5}'\`; do echo \$sr; for vdi in \`xe vdi-list sr-uuid=\$sr | grep ^uuid | awk '{ print \$5}'\` ; do echo \$vdi; xe vdi-destroy uuid=\$vdi; done; for pbd in \`xe pbd-list sr-uuid=\$sr | grep ^uuid | awk '{ print \$5}'\` ; do echo \$pbd; xe pbd-unplug uuid=\$pbd; xe pbd-destroy uuid=\$pbd; done; xe sr-forget uuid=\$sr; done"

ssh root@$1 "for sr in \`xe sr-list type=lvm | grep uuid | awk '{print \$5}'\`; do echo \$sr; for vdi in \`xe vdi-list sr-uuid=\$sr | grep ^uuid | awk '{ print \$5}'\` ; do echo \$vdi; xe vdi-destroy uuid=\$vdi; done; done"

ssh root@$1 "for mount in \`mount | grep '/var/run/sr-mount' | awk '{print \$3}'\`; do echo \$mount; umount \$mount; done"
ssh root@$1 "cd /opt/xensource/bin/ && rm -rf heartbeat"
}


cleanup_storage(){
echo 'Deleting snapshot directory'
ssh root@$1 "cd $2/ &&  rm -rf snapshots"
echo 'Deleteing private directories'
ssh root@$1 "cd $2/template/tmpl && for dir in \`ls | grep -v 1\$\`; do rm -rf \$dir; done"
echo 'Deleting system templates except for 1,2 and 3'
ssh root@$1 "cd $2/template/tmpl/1 && for dir in \`ls | grep -v \^1\$ | grep -v \^2\$ | grep -v \^3\$\`; do rm -rf \$dir; done"
}


dir=$(dirname "$0")
if [ -f $dir/../deploy.properties ]; then
  . "$dir/../deploy.properties"
fi

if [ "$USER" == "" ]; then
  printf "ERROR: Need tospecify the user\n"
  exit 4
fi


#Delete all active users and kill the management server
for h in "$SERVER"
do
  kill_server $h $VMOPSDIR
done

#Kill vms and delete vlans on all computing hosts
for i in $COMPUTE
do
  cleanup_linux $i
done

#Cleanup management server
for i in $SERVER
do
  echo "Starting cleanup on $i";
  cleanup_server $i $LOGDIR
done

#Cleanup secondary storage
for i in $SECONDARY_STORAGE
do
  echo "Starting cleanup on secondary storage $i";
  cleanup_storage $i $SECONDARY_STORAGE_DIR
done



