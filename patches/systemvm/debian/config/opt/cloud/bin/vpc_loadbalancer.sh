#!/usr/bin/env bash
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.

# @VERSION@

do_ilb_if_ilb () {
  local typ=""
  local pattern="type=(.*)"

  for keyval in $(cat /var/cache/cloud/cmdline)
  do    
     if [[ $keyval =~ $pattern ]]; then      
        typ=${BASH_REMATCH[1]}; 
     fi 
  done
  if [ "$typ" == "ilbvm" ]
  then
     logger -t cloud "$(basename $0): Detected that we are running in an internal load balancer vm"
     $(dirname $0)/ilb.sh "$@"
     exit $?
  fi

}

logger -t cloud "$(basename $0): Entering $(dirname $0)/$(basename $0)"

do_ilb_if_ilb "$@"

source /root/func.sh
source /opt/cloud/bin/vpc_func.sh

lock="biglock"
locked=$(getLockFile $lock)
if [ "$locked" != "1" ]
then
    exit 1
fi

usage() {
  printf "Usage: %s:  -i <domR eth1 ip>  -a <added public ip address ip:port> -d <removed ip:port> -f <load balancer config> -s <stats ip ip:port:cidr>  \n" $(basename $0) >&2
}

# set -x

fw_remove_backup() {
  sudo iptables -F back_load_balancer 2> /dev/null
  sudo iptables -D INPUT -p tcp  -j back_load_balancer 2> /dev/null
  sudo iptables -X back_load_balancer 2> /dev/null
  sudo iptables -F back_lb_stats 2> /dev/null
  sudo iptables -D INPUT -p tcp  -j back_lb_stats 2> /dev/null
  sudo iptables -X back_lb_stats 2> /dev/null
}

fw_remove() {
  sudo iptables -F load_balancer 2> /dev/null
  sudo iptables -D INPUT -p tcp  -j load_balancer 2> /dev/null
  sudo iptables -X load_balancer 2> /dev/null
  sudo iptables -F lb_stats 2> /dev/null
  sudo iptables -D INPUT -p tcp  -j lb_stats 2> /dev/null
  sudo iptables -X lb_stats 2> /dev/null
}

fw_backup() {
  fw_remove_backup
  sudo iptables -E load_balancer back_load_balancer 2> /dev/null
  sudo iptables -E lb_stats back_lb_stats 2> /dev/null
}

fw_restore() {
  fw_remove
  sudo iptables -E back_load_balancer load_balancer 2> /dev/null
  sudo iptables -E back_lb_stats lb_stats 2> /dev/null
}

fw_chain_create () {
  fw_backup
  sudo iptables -N load_balancer 2> /dev/null
  sudo iptables -A INPUT -p tcp  -j load_balancer 2> /dev/null
  sudo iptables -N lb_stats 2> /dev/null
  sudo iptables -A INPUT -p tcp  -j lb_stats 2> /dev/null
}

# firewall entry to ensure that haproxy can receive on specified port
fw_entry() {
  local added=$1
  local removed=$2
  local stats=$3
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
  fw_chain_create
  success=0
  while [ 1 ]
  do
    for i in $a
    do
      local pubIp=$(echo $i | cut -d: -f1)
      local dport=$(echo $i | cut -d: -f2)    
      sudo iptables -A load_balancer -p tcp -d $pubIp --dport $dport -j ACL_INBOUND_$dev 2>/dev/null
      success=$?
      if [ $success -gt 0 ]
      then
        break
      fi
    done
    if [ "$stats" != "none" ]
    then
      local pubIp=$(echo $stats | cut -d: -f1)
      local dport=$(echo $stats | cut -d: -f2)    
      local cidrs=$(echo $stats | cut -d: -f3 | sed 's/-/,/')
      sudo iptables -A lb_stats -s $cidrs -p tcp -d $pubIp --dport $dport -j ACCEPT 2>/dev/null
      success=$?
    fi
    break
  done
  if [ $success -gt 0 ]
  then
    fw_restore
  else
    fw_remove_backup
  fi  
  return $success
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

iflag=
aflag=
dflag=
sflag=

while getopts 'i:a:d:s:' OPTION
do
  case $OPTION in
  i)	iflag=1
		ip="$OPTARG"
		;;
  a)	aflag=1
		addedIps="$OPTARG"
		;;
  d)	dflag=1
		removedIps="$OPTARG"
		;;
  s)	sflag=1
		statsIp="$OPTARG"
		;;
  ?)	usage
                unlock_exit 2 $lock $locked
		;;
  esac
done


dev=$(getEthByIp $ip)

if [ "$addedIps" == "" ]
then
  addedIps="none"
fi

if [ "$removedIps" == "" ]
then
  removedIps="none"
fi

# hot reconfigure haproxy
reconfig_lb

if [ $? -gt 0 ]
then
  logger -t cloud "Reconfiguring loadbalancer failed"
  unlock_exit 1 $lock $locked
fi

# iptables entry to ensure that haproxy receives traffic
fw_entry $addedIps $removedIps $statsIp
result=$?  	
if [ $result -gt 0 ]
then
  logger -t cloud "Failed to apply firewall rules for load balancing, reverting HA Proxy config"
  # Restore the LB
  restore_lb
fi
 
unlock_exit $result $lock $locked
