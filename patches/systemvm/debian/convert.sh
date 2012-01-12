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



 


begin=$(date +%s)
echo "Backing up systemvm.img"
cp systemvm.img systemvm.img.tmp
echo "Converting raw image to fixed vhd"
vhd-util convert -s 0 -t 1 -i systemvm.img.tmp -o systemvm.vhd &> /dev/null
echo "Converting fixed vhd to dynamic vhd"
vhd-util convert -s 1 -t 2 -i systemvm.vhd -o systemvm.vhd &> /dev/null
echo "Compressing vhd..."
bzip2 -c systemvm.vhd > systemvm.vhd.bz2
echo "Done VHD"

echo "Converting raw image to qcow2"
qemu-img  convert -f raw -O qcow2 systemvm.img systemvm.qcow2
echo "Compressing qcow2..."
bzip2 -c systemvm.qcow2 > systemvm.qcow2.bz2
echo "Done qcow2"
echo "Converting raw image to vmdk"
qemu-img  convert -f raw -O vmdk systemvm.img systemvm.vmdk
echo "Done creating vmdk"
echo "Creating ova appliance "
ovftool systemvm.vmx systemvm.ova
echo "Done creating OVA"
echo "Cleaning up..."
rm -vf systemvm.vmdk
rm -vf systemvm.vhd.bak

echo "Compressing raw image..."
bzip2 -c systemvm.img > systemvm.img.bz2
echo "Done compressing raw image"

echo "Generating md5sums"
md5sum systemvm.img  > md5sum
md5sum systemvm.img.bz2  >> md5sum
md5sum systemvm.vhd  >> md5sum
md5sum systemvm.vhd.bz2  >> md5sum
md5sum systemvm.qcow2  >> md5sum
md5sum systemvm.qcow2.bz2  >> md5sum
md5sum systemvm.ova  >> md5sum
fin=$(date +%s)
t=$((fin-begin))
echo "Finished compressing/converting image in $t seconds"
