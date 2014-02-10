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

if [ ! -z "$1" ]
then
  appliance="$1"
else
  appliance="systemvmtemplate"
fi

build_date=`date +%Y-%m-%d`
branch="master"
rootdir=$PWD

# Initialize veewee and dependencies
bundle

# Clean and start building the appliance
bundle exec veewee vbox destroy $appliance
bundle exec veewee vbox build $appliance --nogui --auto
bundle exec veewee vbox halt $appliance

while [[ `vboxmanage list runningvms | grep $appliance | wc -l` -ne 0 ]];
do
  echo "Waiting for $appliance to shutdown"
  sleep 2;
done

# Get appliance uuids
machine_uuid=`vboxmanage showvminfo $appliance | grep UUID | head -1 | awk '{print $2}'`
hdd_uuid=`vboxmanage showvminfo $appliance | grep vdi | head -1 | awk '{print $8}' | cut -d ')' -f 1`
hdd_path=`vboxmanage list hdds | grep "$appliance\/" | grep vdi | cut -c 14-`

# Remove any shared folder
shared_folders=`vboxmanage showvminfo $appliance | grep Name | grep Host`
while [ "$shared_folders" != "" ]
do
  vboxmanage sharedfolder remove $appliance --name "`echo $shared_folders | head -1 | cut -c 8- | cut -d \' -f 1`"
  shared_folders=`vboxmanage showvminfo $appliance | grep Name | grep Host`
done

# Compact the virtual hdd
vboxmanage modifyhd $hdd_uuid --compact

# Start exporting
rm -fr dist *.ova *.vhd *.vdi *.qcow* *.bz2 *.vmdk *.ovf
mkdir dist

# Export for Xen
which faketime >/dev/null 2>&1 && which vhd-util >/dev/null 2>&1
if [ $? == 0 ]; then
  set -e
  vboxmanage internalcommands converttoraw -format vdi "$hdd_path" img.raw
  faketime '2010-01-01' vhd-util convert -s 0 -t 1 -i img.raw -o stagefixed.vhd
  faketime '2010-01-01' vhd-util convert -s 1 -t 2 -i stagefixed.vhd -o $appliance-$build_date-$branch-xen.vhd
  rm *.bak
  bzip2 $appliance-$build_date-$branch-xen.vhd
  echo "$appliance exported for Xen: dist/$appliance-$build_date-$branch-xen.vhd.bz2"
else
  echo "** Skipping $appliance export for Xen: faketime or vhd-util command is missing. **"
  echo "** faketime source code is available from https://github.com/wolfcw/libfaketime **"
fi

# Exit shell if exporting fails for any format
set -e

# Export for KVM
vboxmanage internalcommands converttoraw -format vdi "$hdd_path" raw.img
qemu-img convert -f raw -c -O qcow2 raw.img $appliance-$build_date-$branch-kvm.qcow2
rm raw.img
bzip2 $appliance-$build_date-$branch-kvm.qcow2
echo "$appliance exported for KVM: dist/$appliance-$build_date-$branch-kvm.qcow2.bz2"

# Export both ova and vmdk for VMWare
vboxmanage clonehd $hdd_uuid $appliance-$build_date-$branch-vmware.vmdk --format VMDK
bzip2 $appliance-$build_date-$branch-vmware.vmdk
echo "$appliance exported for VMWare: dist/$appliance-$build_date-$branch-vmware.vmdk.bz2"
vboxmanage export $machine_uuid --output $appliance-$build_date-$branch-vmware.ovf
mv $appliance-$build_date-$branch-vmware.ovf $appliance-$build_date-$branch-vmware.ovf-orig
java -cp convert Convert convert_ovf_vbox_to_esx.xslt $appliance-$build_date-$branch-vmware.ovf-orig $appliance-$build_date-$branch-vmware.ovf
tar -cf $appliance-$build_date-$branch-vmware.ova $appliance-$build_date-$branch-vmware.ovf $appliance-$build_date-$branch-vmware-disk1.vmdk
rm -f $appliance-$build_date-$branch-vmware.ovf $appliance-$build_date-$branch-vmware.ovf-orig $appliance-$build_date-$branch-vmware-disk1.vmdk
echo "$appliance exported for VMWare: dist/$appliance-$build_date-$branch-vmware.ova"

# Export for HyperV
vboxmanage clonehd $hdd_uuid $appliance-$build_date-$branch-hyperv.vhd --format VHD
# HyperV doesn't support import a zipped image from S3, but we create a zipped version to save space on the jenkins box
zip $appliance-$build_date-$branch-hyperv.vhd.zip $appliance-$build_date-$branch-hyperv.vhd
echo "$appliance exported for HyperV: dist/$appliance-$build_date-$branch-hyperv.vhd"

mv *-hyperv.vhd *-hyperv.vhd.zip *.bz2 *.ova dist/

