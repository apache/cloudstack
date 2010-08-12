#!/usr/bin/env bash
# $Id: mountdatadisk.sh 9132 2010-06-04 20:17:43Z manuel $ $HeadURL: svn://svn.lab.vmops.com/repos/branches/2.1.x.beta/java/scripts/vm/storage/iscsi/mountdatadisk.sh $
# mountdatadisk.sh -- mount/unmount  data disk from ISCSI server
#
#

dir=$(dirname $0)
. $dir/iscsi_common.sh
. $dir/mirror_common.sh

usage() {
  printf "Usage: %s: [-u | -m ] -h <iscsi-target-host> -d <datadisk target name> -c <0-9> -n <vm-name>\n" $(basename $0) >&2
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
      unmount_disk $vmname $disktype
      return 8
  fi
  echo "/dev/$blkdev"
  ln -s /dev/$blkdev $DISKDIR/$vmname-$disktype-$host
  return $?
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
cflag=
Hflag=
Dflag=
Mflag=
tgtname=
tgthost1=
swpname=
datatgt0=
disknum=
datadisk=
datatgt1=
DISKDIR="/dev/disk/by-vm/"

while getopts 'umn:h:d:c:MH:D:' OPTION
do
  case $OPTION in
  h)	hflag=1
		tgthost0="$OPTARG"
		;;
  H)	Hflag=1
		tgthost1="$OPTARG"
		;;
  d)	dflag=1
		datatgt0="$OPTARG"
		;;
  D)	Dflag=1
		datatgt1="$OPTARG"
		;;
  n)	nflag=1
		vmname="$OPTARG"
		;;
  c)	cflag=1
		disknum="$OPTARG"
                datadisk=datadisk-$disknum
		;;
  u)    uflag=1
		;;
  m)    mflag=1
		;;
  M)    Mflag=1
		;;
  ?)	usage
		;;
  esac
done

if [ "$hflag$nflag$cflag" != "111" ] && [ "$uflag$nflag$cflag" != "111" ]
then
 usage
fi

if [ "$uflag$mflag" != "1" ]  
then
  printf "***Specify one of -u (unmount) or -m (mount)\n" >&2
  usage
fi

if [ "$uflag" == "1" ]
then
  if [ "$uflag$Mflag" == "1" ] 
  then
    unmount_disk  $vmname $datadisk
    exit $?
  elif [ "$uflag$Mflag" == "11" ]
  then
    stop_mirror $vmname $datadisk
    unmount_disk  $vmname $datadisk
    exit $?
  fi
fi

#create disk directory if needed
if [ ! -d $DISKDIR ]; 
then   
  mkdir $DISKDIR 
fi

#check if the iscsi target portal is up and running
if ! check_iscsi_server "$tgthost0" 4
then
   printf "***Unable to ping the iscsi target host $tgthost0, exiting\n" >&2
   exit 3
fi

login $vmname $datatgt0 $tgthost0 

if [ $? -gt 0 ]
then
  exit 5
fi

#figure out the device number and make a softlink 
datadev0=$(make_links $vmname $tgthost0 $datatgt0 $datadisk )

if [ $? -gt 0 ]
then
  exit 6
fi

login $vmname $datatgt1 $tgthost1 

if [ $? -gt 0 ]
then
  unmount_disk $vmname $datadisk
  exit 5
fi

datadev1=$(make_links $vmname $tgthost1 $datatgt1 $datadisk )

if [ $? -gt 0 ]
then
  unmount_disk $vmname $datadisk
  exit 6
fi

if [ "$Mflag" == "1" ]
then
  build_mirror $vmname $datadisk $datadev0 $datadev1
  if [ $? -gt 0 ]
  then
    unmount_disk $vmname $datadisk
    exit 7
  fi
fi

exit 0
