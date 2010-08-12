#!/usr/bin/env bash
# $Id: showdisks.sh 9132 2010-06-04 20:17:43Z manuel $ $HeadURL: svn://svn.lab.vmops.com/repos/branches/2.1.x.beta/java/scripts/storage/zfs/iscsi/showdisks.sh $
# mapiscsi.sh -- list of active iscsi sessions and the corresponding VM
#

usage() {
  printf "Usage: %s <vm name> \n" $(basename $0) >&2
  exit 2
}

get_disktype () {
  local vmdisk=$1
  if [[ $vmdisk =~ ^.*root.*$ ]]
  then
     echo "root";
     return 0
  fi
  if [[ $vmdisk =~ ^.*swap.*$ ]]
  then
     echo "swap";
     return 0
  fi
  if [[ $vmdisk =~ ^.*datadisk.*$ ]]
  then
     echo "datadisk";
     return 0
  fi
}

mirror_state0() {
  local vmname=$1
  local disktype=$2 
  mirr=$(ls -l /dev/md/ | grep $vmname-$disktype | awk '{print $(NF-2)}')
  mdadm --query /dev/md/$mirr  &> /dev/null
  if [ $? -ne 0 -o "$mirr" == "" ]
  then 
    state="Not mirrored"
  else
    state=$(mdadm --detail /dev/md/$mirr | grep "State : " | awk '{print $3 $4 $5}')
  fi
  echo $state
}

mirror_state1() {
  local vmname=$1
  local disktype=$2 
  local disk=$3 
  mirr=$(ls -l /dev/md/ | grep $vmname-$disktype | awk '{print $(NF-2)}')
  mdadm --query /dev/md/$mirr  &> /dev/null
  if [ $? -eq 0 -a "$mirr" != "" ]
  then 
    state=$(mdadm --detail /dev/md/$mirr | grep "$disk" | awk '{print $(NF-2), $(NF-1)}')
  fi
  echo $state
}

if [ "$1" == "" ]
then
  usage
fi
vmname=$1

if ! xm list | grep -w $vmname > /dev/null
then
  printf "%s: vm $vmname not found\n" $(basename $0)
  exit 2
fi

#set -x

disks=$(ls -l /dev/disk/by-vm | grep $vmname | awk '{print $NF}') 
for d in $disks 
do
  vmdisk=$(ls -l /dev/disk/by-vm | grep -w $d | awk '{print $(NF-2)}')
  disktype=$(get_disktype $vmdisk)
  lasttoken=$(echo $vmdisk | awk -F"-" '{print $NF}')
  case "$lasttoken" in
    "root");;
    "swap");;
    "[0-9]") ;;
    *) vmdisk=${vmdisk%-*} #strip the ip address of storage host;;
  esac

  if [[ $vmdisk =~ ^.*datadisk.*$ ]]; then vmdisk=${vm%-*}; fi
  disk=$d
  d=${d##*/}
  iqn=$(ls -l /dev/disk/by-path | grep -w $d | awk '{print $(NF-2)}')
  ip=$(echo $iqn | awk -F: '{print $1}')
  ip=${ip#ip-}
  
  mirrstate="[$(mirror_state0 $vmname $disktype)]"
  diskstate="[$(mirror_state1 $vmname $disktype $disk)]"
  
  echo $vmname $disktype $disk $ip $mirrstate $diskstate
  

done
exit 0

