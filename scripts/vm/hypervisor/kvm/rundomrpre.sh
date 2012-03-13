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



 

# $Id: rundomrpre.sh 10427 2010-07-09 03:30:48Z edison $ $HeadURL: svn://svn.lab.vmops.com/repos/vmdev/java/scripts/vm/hypervisor/kvm/rundomrpre.sh $

set -x
pubKey="/root/.ssh/id_rsa.pub.cloud"
mntpath() {
  local vmname=$1
  if [ ! -d /mnt/$vmname ]
  then
    mkdir -p /mnt/$vmname
  fi
  echo "/mnt/$vmname"
}

mount_raw_disk() {
    local vmname=$1
    local datadisk=$2
    local path=$(mntpath $vmname)
    if [ ! -f $datadisk -a ! -b $datadisk ]
    then
        printf "$datadisk doesn't exist" >&2
        return 2
    fi

    retry=10
    while [ $retry -gt 0 ]
    do
    if [ -b $datadisk ]; then
	mount $datadisk $path &>/dev/null
	ret=$?
    else
        mount $datadisk $path -o loop  &>/dev/null
	ret=$?
    fi
    sleep 10
    if [ $ret -gt 0 ]
    then
	sleep 5
    else
       break
    fi 
    retry=$(($retry-1))
    done
    return 0
}

umount_raw_disk() {
    local vmname=$1
    local datadisk=$2
    local path=$(mntpath $vmname)
    
    retry=10
    sync
    while [ $retry -gt 0 ]
    do
        umount $path &>/dev/null
    	if [ $? -gt 0 ]
    	then
	   sleep 5
    	else
           rm -rf $path
           break
    	fi
        retry=$(($retry-1))
    done
    return $?
}

patch_all() {
    local vmname=$1
    local cmdline=$2
    local datadisk=$3
    local path=$(mntpath $vmname)


    if [ -f $pubKey ]
    then
        cp $pubKey  $path/authorized_keys
    fi
    echo $cmdline > $path/cmdline 
    sed -i "s/%/\ /g" $path/cmdline
    return 0
}

lflag=
dflag=

while getopts 't:v:i:m:e:E:a:A:g:l:n:d:b:B:p:I:N:Mx:X:' OPTION
do
  case $OPTION in
  l)	lflag=1
	vmname="$OPTARG"
        ;;
  t)    tflag=1
        vmtype="$OPTARG"
        ;;
  d)    dflag=1
        rootdisk="$OPTARG"
        ;;
  p)    pflag=1
        cmdline="$OPTARG"
        ;;
  *)    ;;
  esac
done

if [ "$lflag$tflag$dflag" != "111" ]
then
  printf "Error: No enough parameter\n" >&2
  exit 1
fi

if [ "$vmtype" = "all" ]
then
    mount_raw_disk $vmname $rootdisk
    if [ $? -gt 0 ]
    then
        printf "Failed to mount $rootdisk"
        exit $?
    fi

    patch_all $vmname $cmdline $rootdisk

    umount_raw_disk $vmname $rootdisk    
    exit $?
fi


exit $?
