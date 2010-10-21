#!/bin/bash
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
