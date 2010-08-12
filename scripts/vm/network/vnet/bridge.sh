#!/usr/bin/env bash
# $Id: bridge.sh 9132 2010-06-04 20:17:43Z manuel $ $HeadURL: svn://svn.lab.vmops.com/repos/branches/2.1.x.beta/java/scripts/vm/network/vnet/bridge.sh $
#
# bridge.sh
#
# Brings down the bridges so that vms can not go out to the network.
#

usage() {
  printf "Usage: %s (up|down) \n" $(basename $0) >&2
}

#set -x

if [ "$1" == "up" -o "$1" == "down" ]
then 
  for bridge in `brctl show | grep vnbr | awk '{print $1}'`
  do
    ifconfig $bridge $1 
    echo "Bring $1 $bridge"
  done
  ifconfig xenbr1 > /dev/null 2>&1
  if [ $? -eq 0 ]
  then 
    ifconfig xenbr1 $1
  fi
else
  usage
  exit 1
fi
