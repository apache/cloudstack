#!/bin/sh
# $Id: find_bond.sh 10510 2010-07-11 10:10:03Z alex $ $HeadURL: svn://svn.lab.vmops.com/repos/vmdev/java/scripts/vm/hypervisor/xenserver/find_bond.sh $

#set -x
 

cleanup_vlan() {
  for vlan in `xe vlan-list | grep uuid | awk '{print $NF}'`; do 
    untagged=$(xe vlan-param-list uuid=$vlan | grep untagged | awk '{print $NF}')
    network=$(xe pif-param-get param-name=network-uuid uuid=$untagged)
    xe vlan-destroy uuid=$vlan
    xe network-destroy uuid=$network
  done
}

usage() {
  echo "$0 device"
  exit 1
}

sflag=
dflag=

while getopts 'sd' OPTION
do
  case $OPTION in
  d)    dflag=1
         ;;
  s)    sflag=1
         ;;
  ?)    usage
         exit 1
         ;;
  esac
done

shift $(($OPTIND - 1))
nic=$1

[ -z "$nic" ] && usage

addr=$(ip addr | grep $nic | grep inet | awk '{print $2}')
addr=${addr%/*}
bridges=$(brctl show | grep -v bridge | awk '{print $1}')

host_uuid=$(xe host-list hostname=$(hostname) | grep uuid | awk '{print $NF}')
if [ -z "$host_uuid" ]; then
  printf "Unable to find host uuid using $(hostname)\n" >&2
  exit 2
fi


if [ -z "$addr" ]; then
  printf "Unable to find an ip address for $nic\n" >&2
  exit 3
fi

current=$(brctl show | grep $nic | awk '{print $NF}')
for dev in `ip addr | grep mtu | grep -v -E "\.[0-9]*@|lo|$nic|$current" | awk '{print $2}'`
do
  dev=${dev%:}
  echo $bridges | grep $dev >/dev/null 2>&1
  br=$?
  ifconfig $dev | grep UP >/dev/null 2>&1
  rc=$?
  if [ $rc -eq 1 ]; then
    ifconfig $dev up
    sleep 4
  fi
  arping -q -c 1 -w 2 -D -I $dev $addr >/dev/null 2>&1
  rc=$?
  if [ $rc -ne 1 ]; then
    continue;
  fi

  if [ $br -ne 0 ]; then
    # What we've found is the naked nic.
    pif_uuid=$(xe pif-list device=$dev host-uuid=$host_uuid | grep -B 3 "( RO): -1" | grep uuid | awk '{print $NF}')
    if [ -z "$pif_uuid" ]; then
      mac=$(ifconfig $dev | grep HWaddr | awk '{print $NF}')
      pif_uuid=$(xe pif-introduce host-uuid=$host_uuid device=$dev mac=$mac)
    fi

    if [ -z $pif_uuid ]; then
      continue;
    fi

    bridge=$(xe network-list PIF-uuids=$pif_uuid | grep bridge | awk '{print $NF}')
    if [ -z $bridge ]; then
      continue;
    fi

    xe pif-plug uuid=$pif_uuid

    echo ">>>$dev<<<"
    exit 0    
  else
    # What we've found is the bridge
    network_uuid=`xe network-list bridge=$dev | grep uuid | awk '{print $NF}'`
    if [ -z "$network_uuid" ]; then
      continue;
    fi
    pif=`xe pif-list network-uuid=$network_uuid host-uuid=$host_uuid VLAN=-1 | grep device | awk '{print $NF}'`
    if [ -z "$pif" ]; then
      continue;
    fi
    echo ">>>$pif<<<"
    exit 0
  fi
done
exit 4
