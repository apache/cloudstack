#!/usr/bin/env bash
# $Id: loadbalancer.sh 9804 2010-06-22 18:36:49Z alex $ $HeadURL: svn://svn.lab.vmops.com/repos/branches/2.1.x.beta/java/patches/xenserver/root/loadbalancer.sh $
# loadbalancer.sh -- reconfigure loadbalancer rules
#
#
# @VERSION@

usage() {
  printf "Usage: %s:  -i <domR eth1 ip>  -a <added public ip address> -d <removed> -f <load balancer config> \n" $(basename $0) >&2
}

# set -x

# check if gateway domain is up and running
check_gw() {
  ping -c 1 -n -q $1 > /dev/null
  if [ $? -gt 0 ]
  then
    sleep 1
    ping -c 1 -n -q $1 > /dev/null
  fi
  return $?;
}

# firewall entry to ensure that haproxy can receive on specified port
fw_entry() {
  local added=$1
  local removed=$2
  
  if [ "$added" == "none" ]
  then
  	added=""
  fi
  
  if [ "$removed" == "none" ]
  then
  	removed=""
  fi
  
  local a=$(echo $added | cut -d, -f1- --output-delimiter=" ")
  local r=$(echo $removed | cut -d, -f1- --output-delimiter=" ")
  
  for i in $a
  do
    local pubIp=$(echo $i | cut -d: -f1)
    local dport=$(echo $i | cut -d: -f2)
    
    for vif in $VIF_LIST; do 
      iptables -D INPUT -i $vif -p tcp -d $pubIp --dport $dport -j ACCEPT 2> /dev/null
      iptables -A INPUT -i $vif -p tcp -d $pubIp --dport $dport -j ACCEPT
      
      if [ $? -gt 0 ]
      then
        return 1
      fi
    done      
  done

  for i in $r
  do
    local pubIp=$(echo $i | cut -d: -f1)
    local dport=$(echo $i | cut -d: -f2)
    
    for vif in $VIF_LIST; do 
      iptables -D INPUT -i $vif -p tcp -d $pubIp --dport $dport -j ACCEPT
    done
  done
  
  return 0
}

#Hot reconfigure HA Proxy in the routing domain
reconfig_lb() {
  /root/reconfigLB.sh
  return $?
}

# Restore the HA Proxy to its previous state, and revert iptables rules on DomR
restore_lb() {
  # Copy the old version of haproxy.cfg into the file that reconfigLB.sh uses
  cp /etc/haproxy/haproxy.cfg.old /etc/haproxy/haproxy.cfg.new
   
  if [ $? -eq 0 ]
  then
    # Run reconfigLB.sh again
    /root/reconfigLB.sh
  fi
}

get_vif_list() {
  local vif_list=""
  for i in /sys/class/net/eth*; do 
    vif=$(basename $i);
    if [ "$vif" != "eth0" ] && [ "$vif" != "eth1" ]
    then
      vif_list="$vif_list $vif";
    fi
  done
  
  echo $vif_list
}

mflag=
iflag=
aflag=
dflag=
fflag=

while getopts 'i:a:d:f:' OPTION
do
  case $OPTION in
  i)	iflag=1
		domRIp="$OPTARG"
		;;
  a)	aflag=1
		addedIps="$OPTARG"
		;;
  d)	dflag=1
		removedIps="$OPTARG"
		;;
  f)	fflag=1
		cfgfile="$OPTARG"
		;;
  ?)	usage
		exit 2
		;;
  esac
done

VIF_LIST=$(get_vif_list)

# hot reconfigure haproxy
reconfig_lb $cfgfile

if [ $? -gt 0 ]
then
  printf "Reconfiguring loadbalancer failed\n"
  exit 1
fi

if [ "$addedIps" == "" ]
then
  addedIps="none"
fi

if [ "$removedIps" == "" ]
then
  removedIps="none"
fi

# iptables entry to ensure that haproxy receives traffic
fw_entry $addedIps $removedIps
  	
if [ $? -gt 0 ]
then
  # Restore the LB
  restore_lb

  # Revert iptables rules on DomR, with addedIps and removedIps swapped 
  fw_entry $removedIps $addedIps

  exit 1
fi
 
exit 0
  	

