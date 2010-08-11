#!/usr/bin/env bash
# $Id: mapiscsi.sh 9132 2010-06-04 20:17:43Z manuel $ $HeadURL: svn://svn.lab.vmops.com/repos/vmdev/java/scripts/vm/storage/iscsi/mapiscsi.sh $
# mapiscsi.sh -- list of active iscsi sessions and the corresponding VM
#

usage() {
  printf "Usage: %s:  \n" $(basename $0) >&2
}

#set -x
for i in $(iscsiadm -m session 2> /dev/null | awk '{print $4}') 
do   
  ls -l /dev/disk/by-path/*$i* > /dev/null 2>&1
  if [ $? -eq 0 ]; then 
    disknum=$(ls -l /dev/disk/by-path/*$i*| grep -v part | awk '{print $NF}' | awk -F"/" '{print $NF}');  # sdb etc
    vm=$(ls -l /dev/disk/by-vm | grep $disknum | awk '{print $(NF-2)}') 
    lasttoken=$(echo $vm | awk -F"-" '{print $NF}')
    case "$lasttoken" in
      "root");;
      "swap");;
      [0-9]) ;;
      *) vm=${vm%-*} #strip the ip address of storage host;;
    esac

    if [[ $vm =~ ^.*datadisk.*$ ]]; then vm=${vm%-*}; fi
  fi
  echo $i $vm 
done
