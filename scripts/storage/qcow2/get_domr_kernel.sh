#/bin/bash
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



 

# $Id: get_domr_kernel.sh 9132 2010-06-04 20:17:43Z manuel $ $HeadURL: svn://svn.lab.vmops.com/repos/vmdev/java/scripts/storage/qcow2/get_domr_kernel.sh $

set -x

mount_local() {
   local disk=$2
   local path=$1

   /sbin/lsmod | grep nbd &> /dev/null
   local nbd_loaded=$?
   if [ $nbd_loaded -ne 0 ]
   then
        modprobe nbd max_part=8 &> /dev/null 
        if [ $? -ne 0 ]
        then
            printf "No nbd module installed, failed to mount qcow2 image\n"
            return 1
        fi
    fi
    
    qemu-nbd -c /dev/nbd0 $disk &> /dev/null
    if [ $? -ne 0 ]
    then
        printf "failed to create /dev/nbd0\n"   
        return 2
    fi

    mkdir -p ${path}
    retry=5
    while [ $retry -gt 0 ]
    do
        sleep 2
        mount -o sync /dev/nbd0p1 ${path}  &> /dev/null
        if [ $? -eq 0 ]
        then
            break
        fi
        retry=$(($retry-1))
    done
        

    if [ $retry -eq 0 ]
    then
        qemu-nbd -d /dev/nbd0p1 &> /dev/null
        sleep 0.5
        qemu-nbd -d /dev/nbd0 &> /dev/null
        printf "Faild to mount qcow2 image\n"
        return 3
    fi
    return $?
}

umount_local() {
    local path=$1

    umount  $path
    qemu-nbd -d /dev/nbd0p1
    sleep 0.5
    qemu-nbd -d /dev/nbd0
    local ret=$?

    rm -rf $path
    return $ret
}

sflag=
kflag=
iflag=
while getopts 'k:s:i:' OPTION
do
    case $OPTION in
    k)  kflag=1
        domrKern="$OPTARG"
        ;;
    i)  iflag=1
        domrRamfs="$OPTARG"
        ;;
    s)  sflag=1
        image="$OPTARG"
        ;;
    *)  ;;
    esac
done

if [ "$kflag$iflag$sflag" != "111" ]
then
    printf "Error: no enough parameters" >&2
    exit 1
fi

mntPoint=`mktemp -d`
mount_local $mntPoint $image
if [ $? -gt 0 ]
then
    printf "Failed to mount disk" >&2
    exit 2
fi

if [ -f $domrKern ]
then
    rm -rf $domrKern
fi

if [ -f $domrRamfs ]
then
    rm -rf $domrRamfs
fi

cp $mntPoint/boot/vmlinuz* $domrKern -f
cp $mntPoint/boot/initramfs* $domrRamfs -f

umount_local $mntPoint
