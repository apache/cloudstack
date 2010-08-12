#!/usr/bin/env bash
# $Id: lu_info.sh 9132 2010-06-04 20:17:43Z manuel $ $HeadURL: svn://svn.lab.vmops.com/repos/branches/2.1.x.beta/java/scripts/storage/zfs/iscsi/comstar/lu_info.sh $
# lu_info.sh -- provide info on an LU of the form:
# 
# Target: tank/vmops/vm/u000002/r000002/vmi-swap-routing
#    iSCSI Name: iqn.1986-03.com.sun:02:f8a76fae-6545-4756-9573-dc8154b8c0fa
#    Connections: 0
#
# OpenSolaris

usage() {
  printf "Usage:  %s path \n" $(basename $0) >&2
}

hosted() {
	uname -a | grep "101b" > /dev/null
	return $?
}

if [ $# -ne 1 ]
then
  usage
  exit 1
fi

if hosted
then
  iscsitadm list target $1
else
  path=$1
  luname=$(sbdadm list-lu | grep $1 | awk '{print $1}')
  tgtname=$(itadm list-target | tail -1 | awk '{print $1}')
  tgtname=$tgtname:lu:$luname
  conn_count=$(stmfadm list-lu -v $luname | grep View | awk '{print $5}')
  printf "Target: %s\n" $path
  printf "   iSCSI Name: %s\n" $tgtname
  printf "   Connections: %s\n" $conn_count
fi
