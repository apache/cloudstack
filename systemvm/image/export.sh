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
# Requirements: apt-get install qemu-utils
# Usage: sudo bash -x <this script> <qcow2 systemvm image>
set -x

IMAGE=$1

# Export for KVM
virt-sparsify $IMAGE --compress systemvmtemplate-kvm.qcow2

# Export for VMware
qemu-img convert -f qcow2 -O vmdk systemvmtemplate-kvm.qcow2 systemvmtemplate-vmware.vmdk
cat <<EOF > systemvmtemplate-vmware.vmx
.encoding = "UTF-8"
displayname = "systemvmtemplate-vmware"
annotation = "systemvmtemplate-vmware"
guestos = "otherlinux-64"
virtualHW.version = "11"
config.version = "8"
numvcpus = "1"
cpuid.coresPerSocket = "1"
memsize = "256"
pciBridge0.present = "TRUE"
pciBridge4.present = "TRUE"
pciBridge4.virtualDev = "pcieRootPort"
pciBridge4.functions = "8"
pciBridge5.present = "TRUE"
pciBridge5.virtualDev = "pcieRootPort"
pciBridge5.functions = "8"
pciBridge6.present = "TRUE"
pciBridge6.virtualDev = "pcieRootPort"
pciBridge6.functions = "8"
pciBridge7.present = "TRUE"
pciBridge7.virtualDev = "pcieRootPort"
pciBridge7.functions = "8"
vmci0.present = "TRUE"
floppy0.present = "FALSE"
ide0:0.clientDevice = "FALSE"
ide0:0.present = "TRUE"
ide0:0.deviceType = "atapi-cdrom"
ide0:0.autodetect = "TRUE"
ide0:0.startConnected = "FALSE"
mks.enable3d = "false"
svga.autodetect = "false"
svga.vramSize = "4194304"
scsi0:0.present = "TRUE"
scsi0:0.deviceType = "disk"
scsi0:0.fileName = "systemvmtemplate-vmware.vmdk"
scsi0:0.mode = "persistent"
scsi0:0.writeThrough = "false"
scsi0.virtualDev = "lsilogic"
scsi0.present = "TRUE"
vmci0.unrestricted = "false"
vcpu.hotadd = "false"
vcpu.hotremove = "false"
firmware = "bios"
mem.hotadd = "false"
EOF
ovftool systemvmtemplate-vmware.vmx systemvmtemplate-vmware.ova
chmod +r systemvmtemplate-vmware.ova
rm -f systemvmtemplate-vmware.vmx systemvmtemplate-vmware.vmdk

# Export for XenServer/XCP-ng
qemu-img convert -f qcow2 -O vpc systemvmtemplate-kvm.qcow2 systemvmtemplate-xen.vhd
bzip2 systemvmtemplate-xen.vhd

# Create checksums
md5sum systemvmtemplate* > md5sum.txt
sha512sum systemvmtemplate* > sha512sum.txt
