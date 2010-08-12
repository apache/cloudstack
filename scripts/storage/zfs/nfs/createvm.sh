#!/usr/bin/env bash
# $Id: createvm.sh 9132 2010-06-04 20:17:43Z manuel $ $HeadURL: svn://svn.lab.vmops.com/repos/branches/2.1.x.beta/java/scripts/storage/zfs/nfs/createvm.sh $
# createvm.sh -- create a vm image directory by cloning
#

usage() {
  printf "Usage: %s: -t <template-fs> -d <disk-fs> -i <instance-fs> -u <user fs>\n" $(basename $0) >&2
}

#ensure that the instance fs is mounted within the user fs
check_valid_userfs() {
  local ifs=$1
  local ufs=$2
  local child=${ifs#$ufs}
  if [ ${#child} -eq $(( ${#ifs}-${#ufs} )) ] 
  then  
    return 0
  else  
   printf "instance fs $ifs is not contained within user fs $ufs. Bailing\n" >&2
   exit 3;
  fi
}

get_latest_snapshot() {
  local fs=$1
  snap=$(zfs list -r -H -o name -S creation -t snapshot $fs| head -1)
  if [ -z $snap ] 
  then
    return 1
  fi
  echo $snap
  return 0
}

clone_contained_datadisks() {
  local templatefs=$1
  local instancefs=$2
  for diskfs in $(find /$templatefs -type d | grep datadisk)
  do
    diskfs=${diskfs:1}  #strip out leading slash
    disksnap=$(get_latest_snapshot $diskfs)
    if [ -z $disksnap ] 
    then
      printf "No snapshots exist of disk filesystem $diskfs..bailing\n" >&2
      return 6
    fi
    disk=$(basename $disksnap | cut -f1 -d'@')
    disk="$instancefs/$disk"
    printf "Cloning datadisk $disksnap to $disk\n" >&2
    zfs clone $disksnap $disk
    #printf "disksnap=$disksnap target=$disk\n"
    if [ $? -gt 0 ]
    then
      printf "Failed to clone datadisk $disksnap\n" >&2
      return 7
    fi
  done
}

#set -x

tflag=
iflag=
uflag=
dflag=

while getopts 't:i:u:d:' OPTION
do
  case $OPTION in
  t)	tflag=1
		templatefs="$OPTARG"
		;;
  i)	iflag=1
		instancefs="$OPTARG"
		;;
  u)	uflag=1
		userfs="$OPTARG"
		;;
  d)	dflag=1
		diskfs="$OPTARG"
		;;
  ?)	usage
		exit 2
		;;
  esac
done

if [ "$tflag$iflag$uflag" != "111" ]
then
 usage
 exit 2
fi

#if user has provided leading slash, strip it out
if [ ${userfs:0:1} == / ]
then
  userfs=${userfs:1}
fi

if [ ${templatefs:0:1} == / ]
then
  templatefs=${templatefs:1}
fi

if [ ${instancefs:0:1} == / ]
then
  instancefs=${instancefs:1}
fi

if [ -n "$diskfs" ]
then
  if [ ${diskfs:0:1} == / ]
  then
    diskfs=${diskfs:1}
  fi
fi

check_valid_userfs $instancefs $userfs

#if userfs doesn't exist, create it
if [ ! -d /$userfs ] 
then
  zfs create $userfs
  if [ $? -gt 0 ] 
  then
    printf "Failed to create user fs $userfs\n" >&2
    exit 5
  fi
fi

#if user has provided the exact snapshot of the template fs, use it, 
#else get the latest snapshot
tsnap=$(echo $templatefs | cut -f2 -d'@')
if [ $tsnap == $templatefs ]
then
  tsnap=$(get_latest_snapshot $templatefs)
  if [ -z $tsnap ] 
  then
    printf "No snapshots exist of filesystem $templatefs..bailing\n" >&2
    exit 4
  fi
else
  tsnap=$templatefs
  templatefs=$(echo $templatefs | cut -f1 -d'@')  #strip out snap version
fi

if [ -n "$diskfs" ]
then
  disksnap=$(get_latest_snapshot $diskfs)
  if [ -z $disksnap ] 
  then
    printf "No snapshots exist of disk filesystem $diskfs..bailing\n" >&2
    exit 6
  fi
fi

#Clone root disk and associated files
printf "Cloning root disk $tsnap to $instancefs\n" >&2
zfs clone $tsnap $instancefs  
if [ $? -gt 0 ] 
then
  printf "Failed to clone root disk $snap\n" >&2
  exit 5
fi

#Clone datadisk
if [ -n "$diskfs" ]
then
  zfs clone $disksnap $instancefs/datadisk1
else 
  clone_contained_datadisks $templatefs $instancefs
fi

exit 0
