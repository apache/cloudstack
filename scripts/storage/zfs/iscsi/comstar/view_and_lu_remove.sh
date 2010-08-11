#!/usr/bin/env bash
# $Id: view_and_lu_remove.sh 9132 2010-06-04 20:17:43Z manuel $ $HeadURL: svn://svn.lab.vmops.com/repos/vmdev/java/scripts/storage/zfs/iscsi/comstar/view_and_lu_remove.sh $
# view_and_lu_remove.sh -- remove views and LU's under a ZFS file system tree
#
# Usage:  view_and_lu_remove.sh <zfs-path>
#
# Removes outstanding views and deletes LU's recursively under a ZFS file
# system path.
#
# OpenSolaris

usage() {
  printf "Usage: %s: <zfs-path>\n" $(basename $0) >&2
}

#set -x

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

lu_names=$(sbdadm list-lu | grep $zfspath | awk '{print $1}')

for lu in $lu_names
do
  stmfadm remove-view  -a -l $lu 0 2>/dev/null
  sbdadm delete-lu $lu
done

sleep 2

exit 0
