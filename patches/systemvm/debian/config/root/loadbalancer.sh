#!/usr/bin/env bash



  #
  # Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
  # 
  # This software is licensed under the GNU General Public License v3 or later.
  # 
  # It is free software: you can redistribute it and/or modify
  # it under the terms of the GNU General Public License as published by
  # the Free Software Foundation, either version 3 of the License, or any later version.
  # This program is distributed in the hope that it will be useful,
  # but WITHOUT ANY WARRANTY; without even the implied warranty of
  # MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  # GNU General Public License for more details.
  # 
  # You should have received a copy of the GNU General Public License
  # along with this program.  If not, see <http://www.gnu.org/licenses/>.
  #
 

# $Id: loadbalancer.sh 9947 2010-06-25 19:34:24Z manuel $ $HeadURL: svn://svn.lab.vmops.com/repos/vmdev/java/patches/xenserver/root/loadbalancer.sh $
# loadbalancer.sh -- reconfigure loadbalancer rules
#
#
# @VERSION@

usage() {
  printf "Usage: %s:  -i <domR eth1 ip>  -a <added public ip address> -d <removed> -f <load balancer config> \n" $(basename $0) >&2
}

# set -x

# ensure that the nic has the public ip we are load balancing on
ip_entry() {
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
    logger -t cloud "Adding  public ips for load balancing"  
    local pubIp=$(echo $i | cut -d: -f1)
    for vif in $VIF_LIST; do 
      sudo ip addr add dev $vif $pubIp/32
      #ignore error since it is because the ip is already there
    done      
  done

  for i in $r
  do
    logger -t cloud "Removing  public ips for deleted loadbalancers"  
    local pubIp=$(echo $i | cut -d: -f1)
    for vif in $VIF_LIST; do 
      sudo ip addr del $pubIp/32 dev $vif 
    done
  done
  
  return 0
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
    logger -t cloud "Opening up firewall (INPUT chain) for load balancing" 
    local pubIp=$(echo $i | cut -d: -f1)
    local dport=$(echo $i | cut -d: -f2)
    
    for vif in $VIF_LIST; do 
      sudo iptables -D INPUT -i $vif -p tcp -d $pubIp --dport $dport -j ACCEPT 2> /dev/null
      sudo iptables -A INPUT -i $vif -p tcp -d $pubIp --dport $dport -j ACCEPT
      
      if [ $? -gt 0 ]
      then
        return 1
      fi
    done      
  done

  for i in $r
  do
    logger -t cloud "Closing up firewall (INPUT chain) for deleted load balancers" 
    local pubIp=$(echo $i | cut -d: -f1)
    local dport=$(echo $i | cut -d: -f2)
    
    for vif in $VIF_LIST; do 
      sudo iptables -D INPUT -i $vif -p tcp -d $pubIp --dport $dport -j ACCEPT
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
  logger -t cloud "Restoring HA Proxy to previous state"
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
  if [ "$vif_list" == "" ]
  then
      vif_list="eth0"
  fi
  
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

if [ "$addedIps" == "" ]
then
  addedIps="none"
fi

if [ "$removedIps" == "" ]
then
  removedIps="none"
fi

VIF_LIST=$(get_vif_list)


if [ "$addedIps" == "" ]
then
  addedIps="none"
fi

if [ "$removedIps" == "" ]
then
  removedIps="none"
fi

#FIXME: make this explicit via check on vm type or passed in flag
if [ "$VIF_LIST" == "eth0"  ]
then
   ip_entry $addedIps $removedIps
fi


# hot reconfigure haproxy
reconfig_lb $cfgfile

if [ $? -gt 0 ]
then
  printf "Reconfiguring loadbalancer failed\n"
  #FIXME: make this explicit via check on vm type or passed in flag
  if [ "$VIF_LIST" == "eth0"  ]
  then
     ip_entry $removedIps $addedIps
  fi
  exit 1
fi

# iptables entry to ensure that haproxy receives traffic
fw_entry $addedIps $removedIps
  	
if [ $? -gt 0 ]
then
  logger -t cloud "Failed to apply firewall rules for load balancing, reverting HA Proxy config"
  # Restore the LB
  restore_lb

  logger -t cloud "Reverting firewall config"
  # Revert iptables rules on DomR, with addedIps and removedIps swapped 
  fw_entry $removedIps $addedIps

  #FIXME: make this explicit via check on vm type or passed in flag
  if [ "$VIF_LIST" == "eth0"  ]
  then
     logger -t cloud "Reverting ip address changes to eth0"
     ip_entry $removedIps $addedIps
  fi

  exit 1
fi
 
exit 0
  	

