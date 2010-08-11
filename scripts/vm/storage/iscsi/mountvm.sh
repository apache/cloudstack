#!/usr/bin/env bash
# $Id: mountvm.sh 9132 2010-06-04 20:17:43Z manuel $ $HeadURL: svn://svn.lab.vmops.com/repos/vmdev/java/scripts/vm/storage/iscsi/mountvm.sh $
# mountvm.sh -- mount  image directory from NFS and ISCSI server
#
#

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
 while [ -z "$dev" -a $n -gt 0 ]
 do
   sleep 3;
   let n=n-1;
   dev=$(ls -l /dev/disk/by-path/ip-$1*$2*| awk '{print $NF}' | awk -F"/" '{print $NF}' | head -1)
 done
 
 if [ $n -eq 0 ]
 then
   printf "****Timed out waiting for $3 device to register**\n"
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
  umount  $localdir >&2  #ignore errors
  rc=$?
  if [ $rc -gt 0 ]
  then
    printf "*****Warning: unmount failed rc=$rc\n"
    let err=err+1
  fi
  rm -fr $localdir

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
tgtname=
swpname=
datatgtname=
DISKDIR="/dev/disk/by-vm/"

while getopts 'umxw:t:n:h:r:l:1:' OPTION
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
  do_iscsi_logout $tgtname #ignore error, tgtname could be null
  do_iscsi_logout $swpname
  do_iscsi_logout $datatgtname
  exit $rc
fi


#create the local dir if necessary
if ! mkdir -p $localdir
then
  printf "***Unable to create local directory, exiting\n" >&2
  exit 2
fi

#create disk directory if needed
if [ ! -d $DISKDIR ]; 
then   
  mkdir $DISKDIR 
fi

#check if the iscsi target portal is up and running
if ! check_iscsi_server "$iscsitgthost" 4
then
   printf "***Unable to ping the iscsi target host $iscsitgthost, exiting\n" >&2
   exit 3
fi

#mount the local dir (for kernel, ramdisk, etc)
mount -t nfs $iscsitgthost:$remotedir $localdir -o intr,rsize=32768,wsize=32768,hard
if [ $? -gt 0 ] 
then
  printf "***Failed to mount $remotedir at $localdir\n" >&2
  exit 5
fi

do_iscsi_login $tgtname $iscsitgthost
if [ $? -gt 0 ]
then
  printf "***Failed to login to $tgtname at $iscsitgthost\n" >&2
  unmount_all $localdir $vmname
  exit 5
fi
sleep 1

if [ -n "$swpname" ]
then
  do_iscsi_login $swpname $iscsitgthost
  if [ $? -gt 0 ]
  then
    printf "***Failed to login to $swapname at $iscsitgthost\n" >&2
    unmount_all $localdir $vmname
    exit 6
  fi
  sleep 1
fi

if [ -n "$datatgtname" ]
then
  do_iscsi_login $datatgtname $iscsitgthost
  if [ $? -gt 0 ]
  then
    printf "***Failed to login to $datatgtname at $iscsitgthost\n" >&2
    unmount_all $localdir $vmname
    exit 7
  fi
  sleep 1
fi

#figure out the device number and make a softlink 
rootdev=$(get_device_links $iscsitgthost $tgtname root)
if [ "$rootdev" == "" ]
then
    printf "***Failed to get device links for  $tgtname\n" >&2
    unmount_all $localdir $vmname
    exit 8
fi

ln -s /dev/$rootdev $DISKDIR/$vmname-root

swapdev=
if [ -n "$swpname" ] 
then
  swapdev=$(get_device_links $iscsitgthost $swpname swap)
  if [ "$swapdev" == "" ]
  then
      printf "***Failed to get device links for  $swapname\n" >&2
      unmount_all $localdir $vmname
      exit 9
  fi
  ln -s /dev/$swapdev $DISKDIR/$vmname-swap
fi

datadev=
if [ -n "$datatgtname" ] 
then
  datadev=$(get_device_links $iscsitgthost $datatgtname data)
  if [ "$datadev" == "" ]
  then
      printf "***Failed to get device links for  $datatgtname\n" >&2
      unmount_all $localdir $vmname
      exit 10 
  fi
  ln -s /dev/$datadev $DISKDIR/$vmname-datadisk-1
fi


if [ $? -gt 0 ] 
then
  printf "***Failed to mount $tgtname at $localdir\n" >&2
  exit 5
fi


exit 0
