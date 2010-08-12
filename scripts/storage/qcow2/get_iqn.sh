#!/usr/bin/env bash
# $Id: get_iqn.sh 9132 2010-06-04 20:17:43Z manuel $ $HeadURL: svn://svn.lab.vmops.com/repos/branches/2.1.x.beta/java/scripts/storage/qcow2/get_iqn.sh $
# get_iqn.sh -- return iSCSI iqn of initiator (Linux) or target (OpenSolaris)

usage() {
  printf "Usage:  %s \n" $(basename $0) >&2
}


if [ $# -ne 0 ]
then
  usage
  exit 1
fi

ip link show eth0| grep link | awk '{print $2}'
