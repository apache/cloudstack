#!/usr/bin/env bash
# $Id: managesnapshot.sh 9132 2010-06-04 20:17:43Z manuel $ $HeadURL: svn://svn.lab.vmops.com/repos/branches/2.1.x.beta/java/scripts/storage/qcow2/managesnapshot.sh $
# managesnapshot.sh -- manage snapshots for a single disk (create, destroy, rollback)
#

usage() {
  printf "Usage: %s: -c <path to disk> -n <snapshot name>\n" $(basename $0) >&2
  printf "Usage: %s: -d <path to disk> -n <snapshot name>\n" $(basename $0) >&2
  printf "Usage: %s: -r <path to disk> -n <snapshot name>\n" $(basename $0) >&2
  exit 2
}

create_snapshot() {
  local disk=$1
  local snapshotname=$2
  local failed=0

  qemu-img snapshot -c $snapshotname $disk
  
  if [ $? -gt 0 ]
  then
    failed=1
    printf "***Failed to create snapshot $snapshotname for path $disk\n" >&2
    qemu-img snapshot -d $snapshotname $disk
    
    if [ $? -gt 0 ]
    then
      printf "***Failed to delete snapshot $snapshotname for path $disk\n" >&2
    fi
  fi

  return $failed 
}

destroy_snapshot() {
  local disk=$1
  local snapshotname=$2
  local failed=0

  qemu-img snapshot -d $snapshotname $disk
  
  if [ $? -gt 0 ]
  then
    printf "***Failed to delete snapshot $snapshotname for path $disk\n" >&2
    failed=1
  fi

  return $failed 
}

rollback_snapshot() {
  local disk=$1
  local snapshotname=$2
  local failed=0

  qemu-img snapshot -a $snapshotname $disk
  
  if [ $? -gt 0 ]
  then
    printf "***Failed to apply snapshot $snapshotname for path $disk\n" >&2
    failed=1
  fi
  
  return $failed 
}

#set -x

cflag=
dflag=
rflag=
nflag=
pathval=
snapshot=

while getopts 'c:d:r:n:' OPTION
do
  case $OPTION in
  c)	cflag=1
		pathval="$OPTARG"
		;;
  d)    dflag=1
        pathval="$OPTARG"
        ;;
  r)    rflag=1
        pathval="$OPTARG"
        ;;
  n)	nflag=1
		snapshot="$OPTARG"
		;;
  ?)	usage
		;;
  esac
done

if [ "$nflag" != "1" ]
then
  usage
fi

if [ "$cflag$dflag$rflag" != "1" ]  
then
  printf "***Specify one of -c (create), -d (destroy), or -r (rollback) and a path for the target snapshot\n" >&2
  usage
fi

if [ "$cflag" == "1" ]
then
  create_snapshot $pathval $snapshot
  exit $?
elif [ "$dflag" == "1" ]
then
  destroy_snapshot $pathval $snapshot
  exit $?
elif [ "$rflag" == "1" ]
then
  rollback_snapshot $pathval $snapshot
  exit $?
fi

exit 0
