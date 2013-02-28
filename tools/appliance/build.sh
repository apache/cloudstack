#!/bin/bash -xl
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

set -x
set -e

appliance="systemvmtemplate"
build_date=`date +%Y-%m-%d`
branch="master"
rootdir=$PWD

# Initialize veewee and dependencies
bundle

# Clean and start building the appliance
veewee vbox destroy $appliance
veewee vbox build $appliance --nogui
veewee vbox halt $appliance

while [[ `vboxmanage list runningvms | grep $appliance | wc -l` -ne 0 ]];
do
  echo "Waiting for $appliance to shutdown"
  sleep 2;
done

# Get appliance uuids
machine_uuid=`vboxmanage showvminfo $appliance | grep UUID | head -1 | awk '{print $2}'`
hdd_uuid=`vboxmanage showvminfo $appliance | grep vdi | head -1 | awk '{print $8}' | cut -d ')' -f 1`
hdd_path=`vboxmanage list hdds | grep $appliance | grep vdi | cut -c 14-`

# Compact the virtual hdd
vboxmanage modifyhd $hdd_uuid --compact

# Start exporting
rm -fr dist
mkdir dist

# Export for Xen
vboxmange internalcommands converttoraw $hdd_path dist/raw.img
vhd-util convert -s 0 -t 1 -i dist/raw.img  -o dist/$appliance-$build_date-$branch-xen.vhd
bzip2 dist/$appliance-$build_date-$branch-xen.vhd
echo "$appliance exported for Xen: dist/$appliance-$build_date-$branch-xen.vhd.bz2"

# Export for KVM
vboxmange internalcommands converttoraw $hdd_path dist/raw.img
qemu-img convert -f raw -O qcow2 dist/raw.img dist/$appliance-$build_date-$branch-kvm.qcow2
rm dist/raw.img
bzip2 dist/$appliance-$build_date-$branch-kvm.qcow2
echo "$appliance exported for KVM: dist/$appliance-$build_date-$branch-kvm.qcow2.bz2"

# Export for VMWare vSphere
vboxmanage export $machine_uuid --output dist/$appliance-$build_date-$branch-vmware.ova
echo "$appliance exported for VMWare: dist/$appliance-$build_date-$branch-vmware.ova"

# Export for HyperV
vboxmanage clonehd $hdd_uuid dist/$appliance-$build_date-$branch-hyperv.vhd --format VHD
bzip2 dist/$appliance-$build_date-$branch-hyperv.vhd
echo "$appliance exported for HyperV: dist/$appliance-$build_date-$branch-hyperv.vhd.bz2"

