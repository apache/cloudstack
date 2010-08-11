#!/usr/bin/env bash
# $Id: get_iqn.sh 9132 2010-06-04 20:17:43Z manuel $ $HeadURL: svn://svn.lab.vmops.com/repos/vmdev/java/scripts/vm/storage/iscsi/get_iqn.sh $
# get_iqn.sh -- return iSCSI iqn of initiator (Linux) or target (OpenSolaris)

usage() {
  printf "Usage:  %s \n" $(basename $0) >&2
}

linux() {
  uname -a | grep "Linux" > /dev/null
  return $?
}

opensolaris() {
  uname -a | grep "SunOS" > /dev/null
  return $?
}

hosted() {
  uname -a | grep "101b" > /dev/null
  return $?
}

if [ $# -ne 0 ]
then
  usage
  exit 1
fi

if linux
then
  initiator_iqn=$(cat /etc/iscsi/initiatorname.iscsi | cut -d'=' -f2)
  printf "%s\n" $initiator_iqn
  exit 0
fi

if opensolaris && hosted
then
  printf "unique_iqn_per_zvol\n"
  exit 0
fi

if opensolaris
then
  tgt_iqn=$(itadm list-target | tail -1 | awk '{print $1}')
  printf "%s\n" $tgt_iqn
  exit 0
fi

printf "Unexpected operating system!\n" >&2
exit 2