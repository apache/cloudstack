#!/usr/bin/env bash
# $Id: managesnapshot.sh 9132 2010-06-04 20:17:43Z manuel $ $HeadURL: svn://svn.lab.vmops.com/repos/branches/2.1.x.beta/java/scripts/storage/zfs/iscsi/managesnapshot.sh $
# managesnapshot.sh -- manage snapshots (create, destroy, rollback)
#

usage() {
  printf "Usage: %s: -c <path> -n <snapshot name>\n" $(basename $0) >&2
  printf "Usage: %s: -d <path> -n <snapshot name>\n" $(basename $0) >&2
  printf "Usage: %s: -r <path> -n <snapshot name>\n" $(basename $0) >&2
  exit 2
}

create_snapshot() {
  local fspath=$1
  local snapshotname=$2

  zfs snapshot -r $fspath@$snapshotname
  if [ $? -gt 0 ]
  then
    printf "***Failed to create snapshot $snapshotname for path $fspath\n" >&2
    return  5
  fi
}

destroy_snapshot() {
  local fspath=$1
  local snapshotname=$2

  zfs destroy -rRf $fspath@$snapshotname
  if [ $? -gt 0 ]
  then
    printf "***Failed to destroy snapshot $snapshotname for path $fspath\n" >&2
    return  6
  fi
}

rollback_snapshot() {
  local fspath=$1
  local snapshotname=$2

  zfs rollback -r $fspath/datadisk1@$snapshotname
  zfs rollback -r $fspath/rootdisk@$snapshotname
  zfs rollback -r $fspath@$snapshotname
  if [ $? -gt 0 ]
  then
    printf "***Failed to rollback to snapshot $snapshotname for path $fspath\n" >&2
    return  7
  fi
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
