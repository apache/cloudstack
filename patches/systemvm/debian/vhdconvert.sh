#!/bin/bash
# BUILDING vhd-util on Linux
# The xen repository has a tool called vhd-util that compiles and runs on any linux system 
# (http://xenbits.xensource.com/xen-4.0-testing.hg?file/8e8dd38374e9/tools/blktap2/vhd/ or full Xen source at http://www.xen.org/products/xen_source.html).
# Apply this patch: http://lists.xensource.com/archives/cgi-bin/mesg.cgi?a=xen-devel&i=006101cb22f6%242004dd40%24600e97c0%24%40zhuo%40cloudex.cn.
# Build the vhd-util tool:
#    cd tools/blktap2
#    make
#    sudo make install
#

echo "Backing up systemvm.img"
cp systemvm.img systemvm.img.tmp
echo "Converting raw image to fixed vhd"
vhd-util convert -s 0 -t 1 -i systemvm.img.tmp -o systemvm.vhd
echo "Converting fixed vhd to dynamic vhd"
vhd-util convert -s 1 -t 2 -i systemvm.vhd -o systemvm.vhd
echo "Compressing..."
bzip2 -c systemvm.vhd > systemvm.vhd.bz2
echo "Done"
