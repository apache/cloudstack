#!/bin/bash

echo "Backing up systemvm.img"
cp systemvm.img systemvm.img.tmp
echo "Converting raw image to fixed vhd"
vhd-util convert -s 0 -t 1 -i systemvm.img.tmp -o systemvm.vhd
echo "Converting fixed vhd to dynamic vhd"
vhd-util convert -s 1 -t 2 -i systemvm.vhd -o systemvm.vhd
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
echo "Compressing vmdk..."
bzip2 -c systemvm.vmdk > systemvm.vmdk.bz2
echo "Done vmdk"

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
md5sum systemvm.vmdk  >> md5sum
md5sum systemvm.vmdk.bz2  >> md5sum
