#!/usr/bin/env bash
# $Id: mirror.sh 9132 2010-06-04 20:17:43Z manuel $ $HeadURL: svn://svn.lab.vmops.com/repos/vmdev/java/scripts/vm/storage/iscsi/mirror.sh $
# mirror.sh -- modify a mirrored disk
#
#

dir=$(dirname $0)
. $dir/iscsi_common.sh
. $dir/mirror_common.sh

usage() {
  printf "Usage: %s: -m  -h <iscsi-target-host> -t <root disk target-name> -w <swap disk target name> -n <vm-name> \n" $(basename $0) >&2
  printf "Usage: %s: -m  -h <iscsi-target-host> -d <data disk target-name> -c <disk num> -n <vm-name> \n" $(basename $0) >&2
  printf "Usage: %s: -u  -h <iscsi-target-host> -T  -W  -n <vm-name> \n" $(basename $0) >&2
  printf "Usage: %s: -u  -h <iscsi-target-host> -D -c <disknum> -n <vm-name> \n" $(basename $0) >&2
  exit 2
}

login() {
  local vmname=$1
  local iqn=$2
  local host=$3
  do_iscsi_login $iqn $host
  if [ $? -gt 0 ]
  then
    printf "***Failed to login to $iqn at $host\n" >&2
    return  5
  fi
}

make_links() {
  local vmname=$1
  local host=$2
  local iqn=$3
  local disktype=$4
  blkdev=$(get_device_links $host $iqn $disktype)
  if [ "$blkdev" == "" ]
  then
      printf "***Failed to get device links for  $iqn $vmname $disktype\n" >&2
      return 8
  fi
  echo "/dev/$blkdev"
  ln -s /dev/$blkdev $DISKDIR/$vmname-$disktype-$host
  return $?
}

login_and_add_to_mirror() {
  local vmname=$1
  local iqn=$2
  local host=$3
  local disktype=$4

  part_of_mirror $vmname $disktype $iqn
  if [ $? -eq 0 ]
  then
    return 0
  fi

  login $vmname $iqn $host 
  if [ $? -gt 0 ]
  then
    printf "Failed ISCSI login: $disktype disk for $vmname on $host\n"  >&2
    return 5
  fi
  blkdev=$(make_links $vmname $host $iqn $disktype)
  if [ $? -gt 0 ]
  then
     unmount_disk $vmname $disktype-$host
     printf "Failed to get block dev for $disktype disk on $host\n"  >&2
     return 5
  fi
  add_disk_to_mirror $vmname $disktype $blkdev
  if [ $? -gt 0 ]
  then
     unmount_disk $vmname $disktype-$host
     printf "Failed to add $disktype disk on $host to mirror for $vmname\n" >&2
     return 6
  fi
  echo $blkdev
}

validate_flags() {
  if [ "$mflag" == "1" ]
  then
    if [ "$hflag$nflag" != "11" ] && [ "$Dflag$Tflag$Wflag" != "1" ] 
    then
     usage
    fi
    if [ "$dflag$cflag" != "11" ] && [ "$tflag" != "1" ]  
    then
      usage
    fi
  fi
  
  if [ "$uflag" == "1" ]
  then
    if [ "$uflag$hflag" != "11" ]
    then
     usage
    fi
    
    if [ "$uflag$Tflag" != "11" ] && [ "$uflag$Dflag$cflag" != "111" ]
    then
     usage
    fi
  fi 
  
  if [ "$uflag$mflag" != "1" ]  || [ "$uflag$mflag" == "" ]
  then
    printf "***Specify one of -u (unmount) or -m (mount)\n" >&2
    usage
  fi
}


hflag=
uflag=
mflag=
nflag=
cflag=
tflag=
Dflag=
Tflag=
Wflag=
rootname=
swpname=
dataname=
disknum=
datadisk=

#set -x


while getopts 'umw:t:n:h:d:c:TWD' OPTION
do
  case $OPTION in
  h)	hflag=1
		tgthost="$OPTARG"
		;;
  t)	tflag=1
		rootname="$OPTARG"
		;;
  T)	Tflag=1
		;;
  w)	wflag=1
		swpname="$OPTARG"
		;;
  W)	Wflag=1
		;;
  d)	dflag=1
		dataname="$OPTARG"
		;;
  c)	cflag=1
		disknum="$OPTARG"
                datadisk=datadisk-$disknum
		;;
  D)	Dflag=1
		;;
  n)	nflag=1
		vmname="$OPTARG"
		;;
  u)    uflag=1
		;;
  m)    mflag=1
		;;
  ?)	usage
		exit 2
		;;
  esac
done

validate_flags

#create disk directory if needed
if [ ! -d $DISKDIR ]; 
then   
  mkdir $DISKDIR 
fi

if [ "$uflag" == "1" ]  #unmount
then
  if [ "$Tflag" == "1" ] #root disk
  then
    disk=$(get_blkdev $vmname root-$tgthost)
    remove_disk_from_mirror $vmname root $disk
    rc=$?
    if [ $rc -eq 0 ]
    then
      unmount_disk $vmname root-$tgthost
      rc=$?
    fi
  fi
  if [ "$Wflag" == "1" ]  #swap disk
  then
    disk=$(get_blkdev $vmname swap-$tgthost)
    remove_disk_from_mirror $vmname swap $disk
    rc=$?
    if [ $rc -eq 0 ]
    then
      unmount_disk $vmname swap-$tgthost
      rc=$?
    fi
  fi
  if [ "$Dflag" == "1" ] #data disk
  then
    disk=$(get_blkdev $vmname $datadisk-$tgthost)
    remove_disk_from_mirror $vmname $datadisk $disk
    rc=$?
    if [ $rc -eq 0 ]
    then
      unmount_disk $vmname $datadisk-$tgthost
      rc=$?
    fi
  fi
  exit $rc
fi

# rest of the script deals with mounting and adding a disk

#check if the iscsi target portal is up and running
if ! check_iscsi_server "$tgthost" 4
then
   printf "***Unable to ping the iscsi target host $tgthost, exiting\n" >&2
   exit 3
fi

if [ -n "$rootname" ]
then
  rootdev=$(login_and_add_to_mirror $vmname $rootname $tgthost root)
  if [ $? -gt 0 ]
  then
    exit 5
  fi
  if [ -n "$swpname" ]
  then
    swapdev=$(login_and_add_to_mirror $vmname $swpname $tgthost swap)
    if [ $? -gt 0 ]
    then
      # undo the root disk configuration
      remove_disk_from_mirror $vmname root $rootdev
      unmount_disk $vmname root-$tgthost
      unmount_disk $vmname swap-$tgthost
      exit 5
    fi
  fi
  exit 0
fi


if [ -n "$dataname" ]
then
  datadev=$(login_and_add_to_mirror $vmname $dataname $tgthost $datadisk)
  if [ $? -gt 0 ]
  then
     exit 5
  fi
fi

exit 0
