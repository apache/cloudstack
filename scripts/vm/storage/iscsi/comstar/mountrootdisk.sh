#!/usr/bin/env bash
# $Id: mountrootdisk.sh 9132 2010-06-04 20:17:43Z manuel $ $HeadURL: svn://svn.lab.vmops.com/repos/vmdev/java/scripts/vm/storage/iscsi/comstar/mountrootdisk.sh $
# mountrootdisk.sh -- mount  image directory from NFS and ISCSI server
#
#

dir=$(dirname $0)
. $dir/iscsi_common.sh
. $dir/mirror_common.sh

usage() {
  printf "Usage: %s: [-u | -m ] -h <iscsi-target-host> -r <remote-dir> -t <root disk target-name> -w <swap disk target name>  -l <local dir> -n <vm-name> -b <bootloader> [-M -H <mirror-host> -T <mirror-disk-root-target> -W <mirror-swap-disk-target>]\n" $(basename $0) >&2
  exit 2
}

login() {
  local vmname=$1
  local iqn=$2
  local host=$3
  local localdir=$4
  do_iscsi_login $iqn $host
  if [ $? -gt 0 ]
  then
    printf "***Failed to login to $iqn at $host\n" >&2
    unmount_all $localdir $vmname
    exit  5
  fi
}

make_links() {
  local vmname=$1
  local host=$2
  local iqn=$3
  local disktype=$4
  local localdir=$5
  blkdev=$(get_device_links $host $iqn $disktype)
  if [ "$blkdev" == "" ]
  then
      printf "***Failed to get device links for  $iqn $vmname $disktype\n" >&2
      unmount_all $localdir $vmname
      exit 8
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
Mflag=
Tflag=
Wflag=
bflag=
rootname=
swpname=
rootmirror=
swpmirror=
tgthost=
tgtmirror=
bootloader="PyGrub"

while getopts 'umxw:t:n:h:H:r:l:T:W:Mb:' OPTION
do
  case $OPTION in
  h)	hflag=1
		tgthost="$OPTARG"
		;;
  H)	Hflag=1
		tgtmirror="$OPTARG"
		;;
  r)	rflag=1
		remotedir="$OPTARG"
		;;
  t)	tflag=1
		rootname="$OPTARG"
		;;
  T)	Tflag=1
		rootmirror="$OPTARG"
		;;
  w)	wflag=1
		swpname="$OPTARG"
		;;
  b)	bflag=1
		bootloader="$OPTARG"
		;;
  W)	Wflag=1
		swpmirror="$OPTARG"
		;;
  l)	lflag=1
		localdir="$OPTARG"
		;;
  n)	nflag=1
		vmname="$OPTARG"
		;;
  u)    uflag=1
		;;
  m)    mflag=1
		;;
  M)    Mflag=1
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
fi

if [ "$uflag$mflag" != "1" ] 
then
  printf "***Specify one of -u (unmount) or -m (mount)\n" >&2
  usage
fi


if [ "$uflag" == "1" ]
then
  if [ "$Mflag$uflag" == "1" ] 
  then
    unmount_all $localdir $vmname
    rc=$?
    exit $rc
  elif [ "$Mflag$uflag" == "11" ]
  then
    stop_mirror $vmname root 
    rc=$?
    stop_mirror $vmname swap 
    unmount_all $localdir $vmname
    rc=$?
    exit $rc
  fi
fi


#create the local dir if necessary
if ! mkdir -p $localdir
then
  printf "***Unable to create local directory, exiting\n" >&2
fi

#create disk directory if needed
if [ ! -d $DISKDIR ]; 
then   
  mkdir $DISKDIR 
fi

#check if the iscsi target portal is up and running
if ! check_iscsi_server "$tgthost" 4
then
   printf "***Unable to ping the iscsi target host $tgthost, exiting\n" >&2
   exit 3
fi

if [ "$Mflag" == 1 ]
then
  if ! check_iscsi_server "$tgtmirror" 4
  then
     printf "***Unable to ping the iscsi target host $tgtmirror, exiting\n" >&2
     exit 3
  fi
fi

bootloader=$(echo $bootloader | tr [A-Z] [a-z])
if [ "$bootloader" == "external" ]
then
  #mount the local dir (for kernel, ramdisk, etc)
  mount -t nfs $tgthost:$remotedir $localdir -o intr,rsize=32768,wsize=32768,hard
  if [ $? -gt 0 ] 
  then
    printf "***Failed to mount $remotedir at $localdir\n" >&2
    exit 5
  fi
fi

login $vmname $rootname $tgthost $localdir
sleep 1

if [ -n "$swpname" ]
then
  login $vmname $swpname $tgthost $localdir
  sleep 1
fi

if [ -n "$rootmirror" ]
then
  login $vmname $rootmirror $tgtmirror $localdir
  sleep 1
fi

if [ -n "$swpmirror" ]
then
  login $vmname $swpmirror $tgtmirror $localdir
  sleep 1
fi

#figure out the device number and make a softlink 
root0dev=$(make_links $vmname $tgthost $rootname root $localdir)

if [ -n "$swpname" ] 
then
  swap0dev=$(make_links $vmname $tgthost $swpname swap $localdir)
fi

if [ -n "$rootmirror" ] 
then
  root1dev=$(make_links $vmname $tgtmirror $rootmirror root $localdir)
fi

if [ -n "$swpmirror" ] 
then
  swap1dev=$(make_links $vmname $tgtmirror $swpmirror swap $localdir)
fi

if [ "$Mflag" == "1" ]
then
  build_mirror $vmname root $root0dev $root1dev
  if [ "$swap0dev" ]
  then
    build_mirror $vmname swap $swap0dev $swap1dev
  fi
fi

exit 0
