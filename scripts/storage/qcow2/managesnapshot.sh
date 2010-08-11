#!/usr/bin/env bash
# $Id: managesnapshot.sh 11601 2010-08-11 17:26:15Z kris $ $HeadURL: svn://svn.lab.vmops.com/repos/branches/2.1.refactor/java/scripts/storage/qcow2/managesnapshot.sh $
# managesnapshot.sh -- manage snapshots for a single disk (create, destroy, rollback)
#

usage() {
  printf "Usage: %s: -c <path to disk> -n <snapshot name>\n" $(basename $0) >&2
  printf "Usage: %s: -d <path to disk> -n <snapshot name>\n" $(basename $0) >&2
  printf "Usage: %s: -r <path to disk> -n <snapshot name>\n" $(basename $0) >&2
  printf "Usage: %s: -b <path to disk> -n <snapshot name> -p <dest name>\n" $(basename $0) >&2
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
  local backupSnapDir=$1
  local snapshotname=$2
  local failed=0

  if [ -f $backupSnapDir/$snapshotname ]
  then
     rm -f $backupSnapDir/$snapshotname
  
     if [ $? -gt 0 ]
     then
        printf "***Failed to delete snapshot $snapshotname for path $backupSnapDir\n" >&2
        failed=1
     fi
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
backup_snapshot() {
  local disk=$1
  local snapshotname=$2
  local destPath=$3

  if [ ! -d $destPath ]
  then
     mkdir -p $destPath >& /dev/null
     if [ $? -gt 0 ]
     then
        printf "Failed to create $destPath" >&2
        return 3
     fi
  fi

  # Does the snapshot exist? 
  cloud-qemu-img snapshot -l $disk|grep -w "$snapshotname" >& /dev/null
  if [ $? -gt 0 ]
  then
    printf "there is no $snapshotname on disk $disk" >&2
    return 1
  fi

  cloud-qemu-img convert -f qcow2 -O qcow2 -s $snapshotname $disk $destPath/$snapshotname >& /dev/null
  if [ $? -gt 0 ]
  then
    printf "Failed to backup $snapshotname for disk $disk to $destPath" >&2
    return 2
  fi
  return 0
}
#set -x

cflag=
dflag=
rflag=
bflag=
nflag=
pathval=
snapshot=

while getopts 'c:d:r:n:b:p:' OPTION
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
  b)    bflag=1
        pathval="$OPTARG"
        ;;
  n)	nflag=1
	snapshot="$OPTARG"
	;;
  p)    destPath="$OPTARG"
        ;;
  ?)	usage
	;;
  esac
done


if [ "$cflag" == "1" ]
then
  create_snapshot $pathval $snapshot
  exit $?
elif [ "$dflag" == "1" ]
then
  destroy_snapshot $pathval $snapshot
  exit $?
elif [ "$bflag" == "1" ]
then
  backup_snapshot $pathval $snapshot $destPath
  exit $?
elif [ "$rflag" == "1" ]
then
  rollback_snapshot $pathval $snapshot $destPath
  exit $?
fi


exit 0
