#!/usr/bin/env bash
# $Id: mirror_common.sh 9132 2010-06-04 20:17:43Z manuel $ $HeadURL: svn://svn.lab.vmops.com/repos/vmdev/java/scripts/vm/storage/iscsi/mirror_common.sh $
# mirror_common.sh -- md operations
#
#
export MDADM_NO_UDEV=1
# build a mirror of 2 disks
# $1 = vm name
# $2 = disk type (root/swap/data)
# $3 = block device #1 (/dev/sdX)
# $4 = block device #2 (/dev/sdX)
build_mirror() {
  local vmname=$1
  local disktype=$2
  local bd1=$3
  local bd2=$4
  mkdir -p /var/md
  mkdir -p /dev/md
  mdadm --build /dev/md/$vmname-$disktype --level=mirror --raid-devices=2 $bd1 $bd2 --assume-clean -b /var/md/$vmname-$disktype
  return $?
}

# stop a mirror of 2 disks
# $1 = vm name
# $2 = disk type (root/swap/data)
stop_mirror () {
  local vmname=$1
  local disktype=$2
  mdadm --stop /dev/md/$vmname-$disktype
  rm -f /var/md/$vmname-$disktype
  rm -f /dev/md/$vmname-$disktype
  return $?
}


# remove one disk of a mirrored pair
# $1 = vm name
# $2 = disk type (root/swap/data)
# $3 = block device to remove
remove_disk_from_mirror() {
  local vmname=$1
  local disktype=$2
  local bd=$3
  mdadm  /dev/md/$vmname-$disktype --fail $bd 
  local rc=$?
  local i=0
  while [ $rc -gt 0 -a $i -lt 5 ] #sometimes get device busy
  do
    sleep 2;
    mdadm  /dev/md/$vmname-$disktype --fail $bd 
    rc=$?
    let i=i+1
  done
  i=0
  mdadm  /dev/md/$vmname-$disktype --remove $bd
  rc=$?
  while [ $rc -gt 0  -a $i -lt 5 ] #sometimes get device busy
  do
    sleep 2;
    mdadm  /dev/md/$vmname-$disktype --remove $bd 
    rc=$?
    let i=i+1
  done
  return $?
}

# add one disk to a mirrored set
# $1 = vm name
# $2 = disk type (root/swap/data)
# $3 = block device to add
add_disk_to_mirror() {
  local vmname=$1
  local disktype=$2
  local bd=$3
  mdadm  /dev/md/$vmname-$disktype --add $bd
  return $?
}

# is iscsi disk part of the mirror already?
# $1 = vm name
# $2 = disk type (root/swap/data)
# $3 = iqn of the iscsi disk
part_of_mirror() {
  local vmname=$1
  local disktype=$2
  local iqn=$3
  local mdisks=$(mdadm --detail /dev/md/$vmname-$disktype  | grep "/dev/sd" | awk '{print $NF}')
  local idisk=$(ls -al /dev/disk/by-path/ | grep -v part |  grep $iqn | awk '{print $NF}' )
  idisk=${idisk##*/}   #strip everything till last slash
  for md in $mdisks
  do
    md=${md##*/}
    if [ "$md" == "$idisk" ]
    then
      return 0
    fi
  done
  return 1
}


