#!/bin/bash
# Copies premium stuff to systemvm.iso

#set -x

TMP=${HOME}/tmp
MOUNTPATH=/mnt/cloud/systemvm
TMPDIR=${TMP}/cloud/systemvm
ISOPATH=`dirname $0`/../../../vms/systemvm.iso
STUFFPATH=`dirname $0`/../../../vms/premium-systemvm.zip



clean_up() {
  sudo umount $MOUNTPATH
  rm -rf $TMPDIR
}

inject_into_iso() {
  local isofile=${ISOPATH}
  local backup=${isofile}.bak
  local tmpiso=${TMP}/$1
  [ ! -f $isofile ] && echo "$(basename $0): Could not find systemvm iso patch file $isofile" && return 1
  sudo mount -o loop $isofile $MOUNTPATH 
  [ $? -ne 0 ] && echo "$(basename $0): Failed to mount original iso $isofile" && clean_up && return 1
  sudo cp -b $isofile $backup
  [ $? -ne 0 ] && echo "$(basename $0): Failed to backup original iso $isofile" && clean_up && return 1
  rm -rf $TMPDIR
  mkdir -p $TMPDIR
  [ ! -d $TMPDIR  ] && echo "$(basename $0): Could not find/create temporary dir $TMPDIR" && clean_up && return 1
  sudo cp -fr $MOUNTPATH/* $TMPDIR/
  [ $? -ne 0 ] && echo "$(basename $0): Failed to copy from original iso $isofile" && clean_up && return 1
  sudo cp -f $STUFFPATH $TMPDIR/systemvm.zip
  [ $? -ne 0 ] && echo "$(basename $0): Failed to copy sutff $STUFFPATH from to new iso " && clean_up && return 1
  mkisofs -quiet -r -o $tmpiso $TMPDIR
  [ $? -ne 0 ] && echo "$(basename $0): Failed to create new iso $tmpiso from $TMPDIR" && clean_up && return 1
  sudo umount $MOUNTPATH
  [ $? -ne 0 ] && echo "$(basename $0): Failed to unmount old iso from $MOUNTPATH" && return 1
  sudo cp -f $tmpiso $isofile
  [ $? -ne 0 ] && echo "$(basename $0): Failed to overwrite old iso $isofile with $tmpiso" && return 1
  rm -rf $TMPDIR
  return $?
}

sudo mkdir -p $MOUNTPATH

[ ! -f $STUFFPATH ] && echo "$(basename $0): Could not find $STUFFPATH" && exit 3
command -v mkisofs > /dev/null   || (echo "$(basename $0): mkisofs not found, please install or ensure PATH is accurate" ; exit 4)

inject_into_iso systemvm.iso

[ $? -ne 0 ] && exit 5

exit 0
