#!/usr/bin/env bash
# $Id: mountvm.sh 9132 2010-06-04 20:17:43Z manuel $ $HeadURL: svn://svn.lab.vmops.com/repos/vmdev/java/scripts/vm/storage/iscsi/comstar/mountvm.sh $
# mountvm.sh -- mount  image directory from NFS and ISCSI server
#
# COMSTAR version

usage() {
  printf "Usage: %s: [-u | -m ] -h <iscsi-target-host> -r <remote-dir> -t <root disk target-name> -w <swap disk target name> -1 <datadisk target name> -l <local dir> -n <vm-name>\n" $(basename $0) >&2
}

# check if server is up and running
check_iscsi_server() {
  local pings=1
  while  ! ping -c 1 -n -q  $1 > /dev/null  && [ $pings -ne $2 ] 
  do
   let pings=pings+1
  done

  printf "##check_iscsi_server: number of pings=%s\n" $pings
  [ $pings -eq $2 ] && return 1
  return 0
}

target_iqn() {  # either iscsitgtd <target-name> or comstar <target-iqn:lu:lu-name>
  echo $1 | cut -d':' -f1,2,3
}

get_lu_name() {  # <target-iqn:lu:lu-name>
  # <target-iqn:lu:lu-name>
  echo $1 | cut -d':' -f5
}

do_iscsi_login () { # target-iqn # ip-addr
  local t_iqn=$(target_iqn $1) 
  local rc=0

  local sid=  
  # if already logged in, rescan scsi and return success
  sid=$(iscsiadm -m session | grep $2 | grep $t_iqn | awk '{print $2}' | tr -d '[]')
  if [ -n "$sid" ]
  then
    return $sid
  fi
  
  #create a record in the client database
  iscsiadm -m node -T $t_iqn -p $2 -o new
  rc=$?
  if [ $rc -gt 0 ] 
  then
    printf "Failed to create ISCSI initiator record for target rc=$rc\n" >&2
    return -6
  fi
  # do not autologin upon restart
  iscsiadm -m node -T $t_iqn -p $2 -o update -n node.startup -v manual
  
  # login to the target
  iscsiadm -m node -T $t_iqn -p $2 -l
  rc=$?
  if [ $rc -gt 0 ] 
  then
    printf "Failed to login to target,  rc=$rc\n" >&2
    return -7
  fi
  
  sid=$(iscsiadm -m session | grep $2 | grep $t_iqn | awk '{print $2}' | tr -d '[]')
  if [ -n "$sid" ]
  then
    return $sid
  fi
}


get_device_links() {  # <sid> <tgt-iqn:lu:lu-name> <root|swap|data>
  local n=20
  local dev=
  local lu_name=$(get_lu_name $2)
  local disks=
  local lu=
  local sid=$1

  # Let's unplug all the unused devices first
  unplug_unused_scsi_by_sid $sid 1>&2

  while [ $n -gt 0 ]
  do
    let n=n-1
    iscsiadm -m session -r $sid --rescan > /dev/null 2>&1
    disks=$(iscsiadm -m session -r $sid -P 3 | grep Attached | grep running | awk '{print $4}')
    for d in $disks
    do
      lu=$(scsi_id -g -s /block/$d)
      if [ "$lu" == "3$lu_name" ]
      then
        # we found it but now wait for the /dev to appear.
        local m=10
        while [ $m -gt 0 ]
        do
          if ls -l /dev/$d > /dev/null 2>&1; then
            printf "$d\n"
            return 0
          fi
          let m=m-1
          printf "Waiting for /dev/$d to appear\n" >&2
          sleep 1
        done
        printf "Unable to get /dev/$d to appear\n" >&2
        return 2
      fi
    done  
    sleep 3
  done
 
  if [ $n -eq 0 ]
  then
    printf "****Timed out waiting for $3 device to register**\n" >&2
  fi
  return 1
}


#unplug scsi disk by lun
unplug_scsi_by_lu() { # lu
  local rc=0
  local unplug_lu=$1
  if [ -z "$unplug_lu" ]
  then
    return $rc
  fi
  disks=$(iscsiadm -m session -P 3 | grep Attached | grep running | awk '{print $4}')
  for d in $disks
  do
    lu=$(scsi_id -g -s /block/$d)
    if [ "$lu" == "$unplug_lu" ]
    then
      fsync /dev/$d > /dev/null 2>&1
      echo 1 >/sys/block/$d/device/delete
      if [ $? -gt 0 ]
      then
        printf "*****Warning: fail to unplug iscsi disk $d\n" >&2
      else 
        printf "*****Info: succeed to unplug iscsi disk $d\n" >&2
      fi
    fi
  done
}


#unplug scsi disk
unplug_scsi_by_disk() { # disk sd*
  local rc=0
  if [ "$1" == "" ]
  then
    return $rc
  fi
  echo 1 >/sys/block/$1/device/delete
  if [ $? -gt 0 ]
  then
    printf "*****Warning: fail to unplug iscsi disk $1\n" >&2
  else 
    printf "*****Info: succeed to unplug iscsi disk $1\n" >&2
  fi
}



# unmount a local directory and all data disks within
unmount_all() {
  local rc=0
  local err=0
  local localdir=$1
  local vmname=$2
  local disks=$(ls -1 $DISKDIR/$vmname*) #/dev/sdb
  for softlink in $disks 
  do 
    local disknum=$(ls -l $softlink | awk '{print $NF}' | awk -F"/" '{print $NF}') #sdb
    printf " $disknum \n" >&2
    if [ -n "$softlink" ]
    then 
      rm -f $softlink    #delete soft link
      rc=$?
      if [ $rc -gt 0 ]
      then
        printf "*****Warning: failed to delete $softlink rc=$rc\n" >&2
        let err=err+1
      fi
    fi
    
    lun=$(ls -l /dev/disk/by-path/ip* | grep -w $disknum | awk '{print $(NF-2)}' | grep -v part | cut -d: -f2- | cut -d- -f3-)
    printf " $lun \n" >&2 
    if [ -n "$lun" ] 
    then
        pathids=$(ls -l /dev/disk/by-path/ip*$lun | awk -F"/" '{print $NF}')
        printf " $pathids \n" >&2
        for pathid in $pathids
        do
            unplug_scsi_by_disk $pathid
            let err=err+$?
        done
     fi
  done
                
  #unmount the image filesystem from nfs
  local errmsg=$(umount $localdir 2>&1)
  rc=$?
  if [ $rc -gt 0 ]
  then
    echo $errmsg | grep "not mounted"
    if  [ $? -eq 1 ]
    then  
      printf "*****Warning: unmount failed rc=$rc\n" >&2
      let err=err+1
    fi
  fi
  rm -fr $localdir

  return $err
}


# unplug all unused scsi device under /dev/sd*
unplug_unused_scsi_by_sid() { # sid
  local rc=0
  # clean LUN without disk map
  local session=session$1 
  local targets=$(ls /sys/class/iscsi_session/$session/device/ -1 | grep target)
  if [ -z "$targets" ]
  then
    return 0
  fi
  for target in $targets
  do
    local luns=$(ls /sys/class/iscsi_session/$session/device/$target/ | grep :)
    if [ -z "$luns" ]
    then
      continue
    fi
    for lun in $luns
    do
      local disk=$(ls /sys/class/iscsi_session/$session/device/$target/$lun/ | grep "block:" | cut -d: -f2)
      if [ -z "$disk" ]
      then
        echo 1 > /sys/class/iscsi_session/$session/device/$target/$lun/delete
        if [ $? -gt 0 ]
        then
          printf "*****Warning: fail to delete lun $lun\n" >&2
        else
          printf "*****Info: succeed to delete lun $lun\n" >&2
        fi
      else
        local lu=$(scsi_id -g -s /block/$disk)
        if [ -z "$lu" ]
        then
          temp=$(ls -l /dev/disk/by-vm | grep "$disk$")
          if [ -z "$temp" ]
          then
            unplug_scsi_by_disk $disk
          fi
        fi
      fi
    done
  done

  return $err   
}
  
#set -x

hflag=
rflag=
lflag=
uflag=
mflag=
nflag=
tflag=
dflag=
bflag=
tgtname=
swpname=
datatgtname=
bootloader='PyGrub'
DISKDIR="/dev/disk/by-vm"

while getopts 'umxw:t:n:h:r:l:1:b:' OPTION
do
  case $OPTION in
  h)	hflag=1
		iscsitgthost="$OPTARG"
		;;
  r)	rflag=1
		remotedir="$OPTARG"
		;;
  t)	tflag=1
		tgtname="$OPTARG"
		;;
  w)	wflag=1
		swpname="$OPTARG"
		;;
  l)	lflag=1
		localdir="$OPTARG"
		;;
  1)	dflag=1
		datatgtname="$OPTARG"
		;;
  n)	nflag=1
		vmname="$OPTARG"
		;;
  b)	bflag=1
		bootloader="$OPTARG"
		;;
  u)    uflag=1
		;;
  m)    mflag=1
		;;
  x)    set -x	
		;;
  ?)	usage
		exit 2
		;;
  esac
done

bootloader=$(echo $bootloader | tr '[A-Z]' '[a-z]')

if [ "$hflag$rflag$lflag$nflag$tflag" != "11111" ] && [ "$uflag$lflag$nflag" != "111" ]
then
 usage
 exit 2
fi

if [ "$uflag$mflag" != "1" ]  && [ "$uflag$mflag" != "" ]
then
  printf "***Specify one of -u (unmount) or -m (mount)\n" >&2
  usage
  exit 2
fi

if [ "$uflag" == "1" ] 
then
  unmount_all $localdir $vmname
  rc=$?
  exit $rc
fi

#create the local dir if necessary
if ! mkdir -p $localdir
then
  printf "***Unable to create local directory, exiting\n" >&2
  exit 2
fi

#create disk directory if needed
if [ ! -d $DISKDIR ] 
then   
  mkdir $DISKDIR 
fi

#check if the iscsi target portal is up and running
if ! check_iscsi_server "$iscsitgthost" 4
then
   printf "***Unable to ping the iscsi target host $iscsitgthost, exiting\n" >&2
   exit 3
fi

if [ "$bootloader" == "external" ]
then
  #mount the local dir (for kernel, ramdisk, etc)
  mount -t nfs $iscsitgthost:$remotedir $localdir -o intr,rsize=32768,wsize=32768,hard
  if [ $? -gt 0 ] 
  then
    printf "***Failed to mount $remotedir at $localdir\n" >&2
    exit 4
  fi
fi

do_iscsi_login $tgtname $iscsitgthost
rootsid=$?
if [ $rootsid -lt 0 ]
then
  printf "***Failed to login to $tgtname at $iscsitgthost\n" >&2
  unmount_all $localdir $vmname
  exit 5
fi
sleep 1

swapsid=
if [ -n "$swpname" ]
then
  do_iscsi_login $swpname $iscsitgthost
  swapsid=$?
  if [ $swapsid -lt 0 ]
  then
    printf "***Failed to login to $swapname at $iscsitgthost\n" >&2
    unmount_all $localdir $vmname
    exit 6
  fi
  sleep 1
fi

datasid=
if [ -n "$datatgtname" ]
then
  do_iscsi_login $datatgtname $iscsitgthost
  datasid=$?
  if [ $datasid -lt 0 ]
  then
    printf "***Failed to login to $datatgtname at $iscsitgthost\n" >&2
    unmount_all $localdir $vmname
    exit 7
  fi
  sleep 1
fi

#figure out the device number and make a softlink 
rootdev=$(get_device_links $rootsid $tgtname root)
if [ "$rootdev" == "" ]
then
    printf "***Failed to get device links for  $tgtname\n" >&2
    unmount_all $localdir $vmname
    exit 8
fi

ln -s /dev/$rootdev $DISKDIR/$vmname-root
if [ $? -ne 0 ]
then
  printf "***Failed to create softlink from /dev/$rootdev to $DISKDIR/$vmname-root\n" >&2
  exit 9
fi

printf "$DISKDIR/$vmname-root = $rootdev"

swapdev=
if [ -n "$swpname" ] 
then
  swapdev=$(get_device_links $swapsid $swpname swap)
  if [ "$swapdev" == "" ]
  then
      printf "***Failed to get device links for  $swapname\n" >&2
      unmount_all $localdir $vmname
      exit 9
  fi
  ln -s /dev/$swapdev $DISKDIR/$vmname-swap
  if [ $? -ne 0 ]
  then
    printf "***Failed to create softlink from /dev/$swapdev to $DISKDIR/$vmname-swap\n" >&2
    exit 10
  fi
  printf "$DISKDIR/$vmname-swap = $swapdev"
fi
        
datadev=
if [ -n "$datatgtname" ] 
then
  datadev=$(get_device_links $datasid $datatgtname data)
  if [ "$datadev" == "" ]
  then
      printf "***Failed to get device links for  $datatgtname\n" >&2
      unmount_all $localdir $vmname
      exit 10 
  fi
  ln -s /dev/$datadev $DISKDIR/$vmname-datadisk-1
  if [ $? -ne 0 ]
  then
    printf "***Failed to create softlink from /dev/$datadev to $DISKDIR/$vmanme-datadisk-1\n" >&2
    exit 11
  fi
  printf "$DISKDIR/$vmname-datadisk-1 = $datadev"
fi

exit 0
