#!/usr/bin/env bash
# $Id: listvmdisk.sh 9132 2010-06-04 20:17:43Z manuel $ $HeadURL: svn://svn.lab.vmops.com/repos/branches/2.1.x.beta/java/scripts/storage/zfs/iscsi/comstar/filebacked/listvmdisk.sh $
# listvmdisk.sh -- list disks of a VM (iscsi mode)
# OpenSolaris
# Bugs: does not handle hexadecimal numbers. Decimal only!

usage() {
  printf "Usage: %s: -i <instance-fs> [-r | -w | -d <num> ] \n" $(basename $0) >&2
}

hosted() {
	uname -a | grep "101b" > /dev/null
	return $?
}

path_and_iqn() {
  local ifs=$1
  local pattern=$2 

  local diskfs=$(zfs list -r -H -o name $ifs | grep  $pattern)
  if [ "$diskfs" != "" ]
  then
      local luname=$(sbdadm list-lu | grep $diskfs | awk '{print $1}')
      local tgtname=$(itadm list-target | tail -1 | awk '{print $1}')
      if [ "$tgtname" != "" -a "$luname" != "" ]
      then
        tgtname=$tgtname:lu:$luname
      else
        tgtname=""
      fi
  fi
  if [ $? -gt 0 ] 
  then
    return  6
  fi
  if [ "$diskfs" != "" -a "$tgtname" != "" ]
  then
    printf "$diskfs,$tgtname\n"
    return 0
  fi
  return 0
}

#set -x

iflag=
rflag=
dflag=
wflag=
disknum=
instancefs=

while getopts 'i:d:rw' OPTION
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
  w)	wflag=1
		;;
  ?)	usage
		exit 2
		;;
  esac
done

if [ "$iflag" != "1"  -a "$rflag$dflag$wflag" != "1" ]
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
  path_and_iqn $instancefs "root"
  exit $? 
fi

if [ "$wflag" == 1 ] 
then
  path_and_iqn $instancefs "swap"
  exit $?
fi

if [ "$dflag" == 1 ] 
then
  if [[ $disknum -eq 0 ]]
  then 
    path_and_iqn $instancefs "root"
  else 
    path_and_iqn $instancefs "datadisk"$disknum
  fi

  exit $? 
fi
exit 0
