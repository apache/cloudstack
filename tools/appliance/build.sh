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
hdd_uuid=`vboxmanage showvminfo $appliance | grep vmdk | head -1 | awk '{print $8}' | cut -d ')' -f 1`

# Start exporting
rm -fr dist
mkdir dist
cd dist

# Export for VMWare vSphere
vboxmanage export $machine_uuid --output $appliance-$build_date-$branch-vmware.ova

# Export for HyperV
vboxmanage clonehd $hdd_uuid $appliance-$build_date-$branch-hyperv.vhd --format VHD
bzip2 $appliance-$build_date-$branch-hyperv.vhd

# Export for KVM
vboxmanage clonehd $hdd_uuid raw.img --format RAW
qemu-img convert -f raw -O qcow2 raw.img $appliance-$build_date-$branch-kvm.qcow2
bzip2 $appliance-$build_date-$branch-kvm.qcow2

# Export for Xen
# This will be an overwrite convert so, do it at the end
vhd-util convert -s 0 -t 1 -i raw.img  -o $appliance-$build_date-$branch-xen.vhd
bzip2 $appliance-$build_date-$branch-xen.vhd

cd $rootdir
