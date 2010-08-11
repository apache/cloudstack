#!/usr/bin/env bash
# $Id: mapiscsi.sh 9132 2010-06-04 20:17:43Z manuel $ $HeadURL: svn://svn.lab.vmops.com/repos/vmdev/java/scripts/vm/storage/iscsi/comstar/mapiscsi.sh $
# mapiscsi.sh -- list of active iscsi sessions and the corresponding VM
#
# COMSTAR version
#
# typical output:
#

usage() {
  printf "Usage: %s:  \n" $(basename $0) >&2
}

#set -x

iqn="iqn.2009-99.unk.nown:02:00000000-0000-0000-0000-000000000000:lu:00000000000000000000000000000000"

for vm in $(ls -l /dev/disk/by-vm | grep dev | awk '{print $(NF-2)}') 
do   
  lasttoken=$(echo $vm | awk -F"-" '{print $NF}')
  case "$lasttoken" in
    "root");;
    "swap");;
    [0-9]) ;;
    *) vm=${vm%-*} #strip the ip address of storage host;;
  esac

  if [[ $vm =~ ^.*datadisk.*$ ]]; then vm=${vm%-*}; fi
  echo $iqn $vm 
done
