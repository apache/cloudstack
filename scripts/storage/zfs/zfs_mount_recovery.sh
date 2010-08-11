#!/usr/bin/env bash
# $Id: zfs_mount_recovery.sh 9132 2010-06-04 20:17:43Z manuel $ $HeadURL: svn://svn.lab.vmops.com/repos/vmdev/java/scripts/storage/zfs/zfs_mount_recovery.sh $
# zfs_mount_recovery.sh -- recover from early boot zfs mount failure
#
# Usage:
#
#   zfs_mount_recovery.sh [-f]
#
# Command line options:
#
#   -f  Force: Unmount/remount nonroot zfs filesystem even if the
#              system/filesystem/local service is working properly.
#
# OpenSolaris

usage() {
  printf "Usage: %s [-f]\n" $(basename $0) >&2
}

#set -x

if [ $# -gt 2 ]
then
  usage
  exit 1
fi

force_flag=

while getopts 'f' OPTION
do
  case $OPTION in
  f)  force_flag=1
      ;;
  *)  usage
      exit 2
      ;;
  esac
done

state=$(svcs -a | grep system/filesystem/local | awk '{print $1}')

if [ "$state" == "online" -a -z "$force_flag" ]
then
  exit 0
fi

rootpool_name="rootpool"

pool_name=$(zpool list -H | grep -v $rootpool_name | awk '{print $1}')

if [ -z "$pool_name" ]
then
  printf "no storage pool configured\n" >&2
  exit 3
fi

# unmount any mounted child filesystems of affected storage pool

for path in $(zfs mount | grep $pool_name | awk '{print $1}')
do
  zfs unmount -f $path 2>/dev/null
done

rm -rf /${pool_name:-zzz}

for path in $(zfs list -H -o name,type | grep ^$pool_name | grep filesystem | awk '{print $1}')
do
  zfs mount -O $path
done

svcadm clear system/filesystem/local

exit 0
