#!/usr/bin/env bash
# $Id: listvmdisk.sh 9132 2010-06-04 20:17:43Z manuel $ $HeadURL: svn://svn.lab.vmops.com/repos/branches/2.1.x.beta/java/scripts/storage/zfs/nfs/listvmdisk.sh $
# listvmdisk.sh -- list disks of a VM
# Bugs: does not handle hexadecimal numbers. Decimal only!

usage() {
  printf "Usage: %s: -i <instance-fs> [-r | -d <num> ] \n" $(basename $0) >&2
}


#set -x

iflag=
rflag=
dflag=
disknum=
instancefs=

while getopts 'i:d:r' OPTION
do
  case $OPTION in
  i)	iflag=1
		instancefs="$OPTARG"
		;;
  d)	dflag=1
		disknum="$OPTARG"
		;;
  r)	rflag=1
		;;
  ?)	usage
		exit 2
		;;
  esac
done

if [ "$iflag" != "1"  -a "$rflag$dflag" != "1" ]
then
 usage
 exit 2
fi

if [ ${instancefs:0:1} == / ]
then
  instancefs=${instancefs:1}
fi


if [[ $(zfs get -H -o value -p type $instancefs) != filesystem  ]]
then
  printf "Supplied instance fs doesn't exist\n" >&2
  exit 1
fi


if [ "$rflag" == 1 ] 
then
  zfs list -r -H -o name  -t filesystem $instancefs | grep -v datadisk
  if [ $? -gt 0 ] 
  then
    exit 5
  fi
  exit 0
fi

if [ "$dflag" == 1 ] 
then
  if [[ $disknum -eq 0 ]]
  then 
    zfs list -r -H -o name  -t filesystem $instancefs | grep -v datadisk
  else 
    zfs list -r -H -o name  -t filesystem $instancefs | grep  datadisk$disknum
  fi
  if [ $? -gt 0 ] 
  then
    exit 0
  fi
  exit 0
fi
exit 0
