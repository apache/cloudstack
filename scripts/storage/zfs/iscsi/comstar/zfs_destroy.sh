#!/usr/bin/env bash
# $Id: zfs_destroy.sh 9132 2010-06-04 20:17:43Z manuel $ $HeadURL: svn://svn.lab.vmops.com/repos/vmdev/java/scripts/storage/zfs/iscsi/comstar/zfs_destroy.sh $
# zfs_destroy.sh -- delete a cloned image used for a vm
#
# Usage:  zfs_destroy <zfs-path>
#
# Removes outstanding views, deletes LU's, and then performs a
# "zfs destroy -f -r <zfs-path>".
#
# OpenSolaris

usage() {
  printf "Usage: %s: <zfs-path>\n" $(basename $0) >&2
}

#set -x

get_instance() {
  echo $(basename $1)
}

zfspath=$1

if [ -z "$1" ]
then
 usage
 exit 1
fi

paths=$(zfs list -Hro name $zfspath)

if [ -z "$paths" ]
then
  printf "%s: zfs path %s does not exist.\n" $(basename $0) >&2
  exit 2
fi

lu_names=$(sbdadm list-lu 2>&1 | grep $zfspath | awk '{print $1}')

for lu in $lu_names
do
  stmfadm remove-view  -a -l $lu 0 2>/dev/null
  sbdadm delete-lu $lu
done

zfs destroy -r -f $zfspath

exit 0
