#!/usr/bin/env bash
# $Id: createdatadisk.sh 9132 2010-06-04 20:17:43Z manuel $ $HeadURL: svn://svn.lab.vmops.com/repos/vmdev/java/scripts/storage/zfs/iscsi/createdatadisk.sh $
# createdatadisk.sh -- create a thin-provisioned data disk
#

usage() {
  printf "Usage: %s:  -i <instance-fs> -s <disk size in GB>\n" $(basename $0) >&2
}


#set -x

iflag=
sflag=
cflag=
disknum=
datadisk=

while getopts 'i:s:c:' OPTION
do
  case $OPTION in
  i)	iflag=1
		instancefs="$OPTARG"
		;;
  c)	cflag=1
		disknum="$OPTARG"
                datadisk="datadisk-$disknum"
		;;
  s)	sflag=1
		diskfs=""
		disksize="$OPTARG"
		;;
  ?)	usage
		exit 2
		;;
  esac
done

if [ "$iflag$sflag$cflag" != "111" ]
then
 usage
 exit 2
fi

if [ ${instancefs:0:1} == / ]
then
  instancefs=${instancefs:1}
fi


if [ -n "$disksize" ]
then
  suffix=${disksize:(-1)}
  echo $suffix
  case $suffix in
    G)   
         ;;
    [0-9])   disksize=${disksize}G
         ;;
    *)   printf "Error in disk size: expect G as a suffix or no suffix\n"
         exit 2
         ;;
  esac
  
fi

if [ -n "$disksize" ]
then
  zfs create -V $disksize -s $instancefs/$datadisk #-s for sparse
  rc=$?
fi

if [ $rc -gt 0 ] 
then
  printf "Failed to create data disk $instancefs/$datadisk\n" >&2
  exit 6
fi

exit 0
