#!/usr/bin/env bash
# $Id: iscsi_common.sh 9132 2010-06-04 20:17:43Z manuel $ $HeadURL: svn://svn.lab.vmops.com/repos/branches/2.1.x.beta/java/scripts/vm/storage/iscsi/iscsi_common.sh $
# iscsi_common.sh -- mount  volume from ISCSI server
#
#

# check if server is up and running
check_iscsi_server() {
  local pings=1
  while  ! ping -c 1 -n -q  $1 > /dev/null  && [ $pings -ne $2 ] 
  do
   let pings=pings+1
  done

  printf "##check_iscsi_server: number of pings=$pings\n"
  [ $pings -eq $2 ] && return 1
  return 0;
}

do_iscsi_login () {
  local rc=0
  #create a record in the client database
  iscsiadm -m node -T $1 -p $2 -o new
  rc=$?
  if [ $rc -gt 0 ] 
  then
    printf "Failed to create ISCSI initiator record for target rc=$rc\n" >&2
    return 6
  fi
  # do not autologin upon restart
  iscsiadm -m node -T $1 -p $2 -o update -n node.startup -v manual
  
  #login to the target
  iscsiadm -m node -T $1 -p $2 -l
  rc=$?
  if [ $rc -gt 0 ] 
  then
    printf "Failed to login to target,  rc=$rc\n" >&2
    return 7
  fi
}


get_device_links() {
 local n=20
 local dev=
 local host=$1
 local iqn=$2
 local disktype=$3
 while [ -z "$dev" -a $n -gt 0 ]
 do
   sleep 3;
   let n=n-1;
   dev=$(ls -l /dev/disk/by-path/ip-$host*$iqn*| awk '{print $NF}' | awk -F"/" '{print $NF}' | head -1)
 done
 
 if [ $n -eq 0 ]
 then
   printf "****Timed out waiting for $disktype device to register**\n"
 fi
 printf "$dev\n"
}

do_iscsi_logout() {
  local rc=0
  if [ "$1" == "" ]
  then
     return $rc
  fi
  iscsiadm -m node -T $1 -u  #logout
  rc=$?
  if [ $rc -gt 0 ]
  then
    printf "*****Warning: failed to logout of $1 rc=$rc\n"
  fi
  iscsiadm -m node -T $1 -o delete  #delete record from db
  if [ $? -gt 0 ]
  then
    printf "*****Warning: failed to delete iscsi record $1 rc=$rc\n"
  fi
  return $rc
}

# unmount a local directory and all data disks within
unmount_all() {
  local rc=0
  local err=0
  local localdir=$1
  local vmname=$2
  local disks=$(ls  -l $DISKDIR | grep $vmname | awk '{print $NF}') #/dev/sdb
  for d in $disks ; 
  do 
    disknum=$(echo $d | awk -F"/" '{print $NF}') #sdb
    tgt=$(ls -l /dev/disk/by-path  | grep -w $disknum | awk '{print $(NF-2)}' | grep -v part |  cut -d: -f2- | cut -d- -f3-)
    softlink=$(ls -l $DISKDIR |grep $vmname| grep $d | awk '{print $(NF-2)}') #vmname-root or vmname-swap or vmname-data
    rm $DISKDIR/$softlink    #delete soft link
    rc=$?
    if [ $rc -gt 0 ]
    then
      printf "*****Warning: failed to delete $DISKDIR/$softlink rc=$rc\n"
      let err=err+1
    fi
    do_iscsi_logout $tgt
    let err=err+$?
  done

  #unmount the image filesystem from nfs
  local errmsg=$(umount $localdir 2>&1)
  rc=$?
  if [ $rc -gt 0 ]
  then
    echo $errmsg | grep "not mounted"
    if  [ $? -eq 1 ]
    then
      printf "*****Warning: unmount failed rc=$rc\n"
      let err=err+1
    fi
  fi
  rm -fr $localdir

  return $err
}

#find the block device for a particular vm disk
get_blkdev() {
  local vmname=$1
  local disktype=$2
  local disk=$(ls  -l $DISKDIR | grep $vmname-$disktype | awk '{print $NF}') #/dev/sdb
  echo $disk
}

# unmount an iscsi disk
unmount_disk() {
  local rc=0
  local err=0
  local vmname=$1
  local disktype=$2
  local disks=$(ls  -l $DISKDIR | grep $vmname | grep $disktype | awk '{print $NF}') #/dev/sdb
  for d in $disks ; 
  do 
    local disknum=$(echo $d | awk -F"/" '{print $NF}') #sdb
    local tgt=$(ls -l /dev/disk/by-path  | grep -w $disknum | awk '{print $(NF-2)}' | grep -v part |  cut -d: -f2- | cut -d- -f3-)
    local softlink=$(ls -l $DISKDIR |grep $vmname| grep $d | awk '{print $(NF-2)}') #vmname-root or vmname-swap or vmname-data
    rm $DISKDIR/$softlink    #delete soft link
    rc=$?
    if [ $rc -gt 0 ]
    then
      printf "*****Warning: failed to delete $DISKDIR/$softlink rc=$rc\n"
      let err=err+1
    fi
    do_iscsi_logout $tgt
    let err=err+$?
  done
  return $err
}

DISKDIR="/dev/disk/by-vm/"
