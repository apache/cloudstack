#!/bin/bash

# $1 = new key

#set -x

TMP=/tmp
SYSTEMVM_PATCH_DIR=../../../vms/
MOUNTPATH=/mnt/cloud/systemvm
TMPDIR=${TMP}/cloud/systemvm


inject() {
  local isofile=${SYSTEMVM_PATCH_DIR}/$1
  local newpubkey=$2
  local backup=${isofile}.bak
  local tmpiso=${TMP}/$1
  rm -rf $TMPDIR
  mkdir -p $TMPDIR
  [ ! -d $TMPDIR  ] && echo "$(basename $0): Could not find/create temporary dir $TMPDIR" && return 1
  [ ! -f $isofile ] && echo "$(basename $0): Could not find systemvm iso patch file $isofile" && return 1
  cp -b $isofile $backup
  [ $? -ne 0 ] && echo "$(basename $0): Failed to backup original iso $isofile" && return 1
  mount -o loop $isofile $MOUNTPATH 
  [ $? -ne 0 ] && echo "$(basename $0): Failed to mount original iso $isofile" && return 1
  cp -fr $MOUNTPATH/* $TMPDIR/
  [ $? -ne 0 ] && echo "$(basename $0): Failed to copy from original iso $isofile" && return 1
  cp $newpubkey $TMPDIR/authorized_keys
  [ $? -ne 0 ] && echo "$(basename $0): Failed to copy key $newpubkey from original iso to new iso " && return 1
  mkisofs -quiet -r -o $tmpiso $TMPDIR
  [ $? -ne 0 ] && echo "$(basename $0): Failed to create new iso $tmpiso from $TMPDIR" && return 1
  umount $MOUNTPATH
  [ $? -ne 0 ] && echo "$(basename $0): Failed to unmount old iso from $MOUNTPATH" && return 1
  cp -f $tmpiso $isofile
  [ $? -ne 0 ] && echo "$(basename $0): Failed to overwrite old iso $isofile with $tmpiso" && return 1
  rm -rf $TMPDIR
}

mkdir -p $MOUNTPATH

[ $# -ne 1 ] && echo "Usage: $(basename $0)  <new keyfile>" && exit 3
newpubkey=$1
[ ! -f $newpubkey ] && echo "$(basename $0): Could not open $newpubkey" && exit 3
[ $EUID -ne 0 ] && echo  "$(basename $0): You have to be root to run this script" && exit 3

command -v mkisofs > /dev/null   || (echo "$(basename $0): mkisofs not found, please install or ensure PATH is accurate" ; exit 4)

inject systemvm.iso $newpubkey
#inject systemvm-premium.iso $newpubkey

exit $?
