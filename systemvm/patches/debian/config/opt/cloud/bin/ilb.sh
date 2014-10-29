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

source /root/func.sh

lock="biglock"
locked=$(getLockFile $lock)
if [ "$locked" != "1" ]
then
    exit 1
fi

usage() {
  printf "Usage: %s:  -a <added public ip address ip:port> -d <removed ip:port> -f <load balancer config> -s <stats ip ip:port:cidr>  \n" $(basename $0) >&2
}

#set -x

fw_remove_backup() {
  logger -t cloud "$(basename $0): Entering fw_remove_backup"
  local lb_vif_list=eth0
  for vif in $lb_vif_list; do 
    sudo iptables -F back_load_balancer_$vif 2> /dev/null
    sudo iptables -D INPUT -i $vif -p tcp  -j back_load_balancer_$vif 2> /dev/null
    sudo iptables -X back_load_balancer_$vif 2> /dev/null
  done
  sudo iptables -F back_lb_stats 2> /dev/null
  sudo iptables -D INPUT -p tcp  -j back_lb_stats 2> /dev/null
  sudo iptables -X back_lb_stats 2> /dev/null
}

fw_restore() {
  logger -t cloud "$(basename $0): Entering fw_restore"
  local lb_vif_list="eth0"
  for vif in $lb_vif_list; do 
    sudo iptables -F load_balancer_$vif 2> /dev/null
    sudo iptables -D INPUT -i $vif -p tcp  -j load_balancer_$vif 2> /dev/null
    sudo iptables -X load_balancer_$vif 2> /dev/null
    sudo iptables -E back_load_balancer_$vif load_balancer_$vif 2> /dev/null
  done
  sudo iptables -F lb_stats 2> /dev/null
  sudo iptables -D INPUT -p tcp  -j lb_stats 2> /dev/null
  sudo iptables -X lb_stats 2> /dev/null
  sudo iptables -E back_lb_stats lb_stats 2> /dev/null
}

# firewall entry to ensure that haproxy can receive on specified port
fw_entry() {
  logger -t cloud "$(basename $0): Entering fw_entry"
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

# back up the iptable rules by renaming before creating new. 
  local lb_vif_list=eth0
  for vif in $lb_vif_list; do 
    sudo iptables -E load_balancer_$vif back_load_balancer_$vif 2> /dev/null
    sudo iptables -N load_balancer_$vif 2> /dev/null
    sudo iptables -A INPUT -i $vif -p tcp  -j load_balancer_$vif
  done
  sudo iptables -E lb_stats back_lb_stats 2> /dev/null
  sudo iptables -N lb_stats 2> /dev/null
  sudo iptables -A INPUT  -p tcp  -j lb_stats

  for i in $a
  do
    local pubIp=$(echo $i | cut -d: -f1)
    local dport=$(echo $i | cut -d: -f2)    
    local lb_vif_list="eth0"
    for vif in $lb_vif_list; do 
      sudo iptables -A load_balancer_$vif  -p tcp -d $pubIp --dport $dport -j ACCEPT
      if [ $? -gt 0 ]
      then
        return 1
      fi
    done      
  done
  local pubIp=$(echo $stats | cut -d: -f1)
  local dport=$(echo $stats | cut -d: -f2)    
  local cidrs=$(echo $stats | cut -d: -f3 | sed 's/-/,/')
  sudo iptables -A lb_stats -s $cidrs -p tcp -m state --state NEW -d $pubIp --dport $dport -j ACCEPT
 
  return 0
}

#Hot reconfigure HA Proxy in the routing domain
reconfig_lb() {
  logger -t cloud "Reconfiguring ilb using $1"
  /root/reconfigLB.sh $1
  return $?
}

# Restore the HA Proxy to its previous state, and revert iptables rules on loadbalancer
restore_lb() {
  logger -t cloud "Restoring HA Proxy to previous state"
  # Copy the old version of haproxy.cfg into the file that reconfigLB.sh uses
  cp /etc/haproxy/haproxy.cfg.old /etc/haproxy/haproxy.cfg.new
   
  if [ $? -eq 0 ]
  then
    # Run reconfigLB.sh again
    /root/reconfigLB.sh /etc/haproxy/haproxy.cfg.new
  fi
}


logger -t cloud "$(basename $0): Entering $(dirname $0)/$(basename $0)"

iflag=
aflag=
dflag=
fflag=
sflag=

while getopts 'i:a:d:f:s:' OPTION
do
  case $OPTION in
  i)	iflag=1
		domRIp="$OPTARG" #unused but passed in
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
  s)	sflag=1
		statsIp="$OPTARG"
		;;
  ?)	usage
                unlock_exit 2 $lock $locked
		;;
  esac
done

if [[ "$aflag$dflag" != "1" && "$aflag$dflag" != "11" ]]
then
   usage
   unlock_exit 2 $lock $locked
fi

if [ "$addedIps" == "" ]
then
  addedIps="none"
fi


if [ "$removedIps" == "" ]
then
  removedIps="none"
fi


# hot reconfigure haproxy
reconfig_lb $cfgfile

if [ $? -gt 0 ]
then
  logger -t cloud "Reconfiguring ilb failed"
  unlock_exit 1 $lock $locked
fi

logger -t cloud "HAProxy reconfigured successfully, configuring firewall"

# iptables entry to ensure that haproxy receives traffic
fw_entry $addedIps $removedIps $statsIp
  	
if [ $? -gt 0 ]
then
  logger -t cloud "Failed to apply firewall rules for internal load balancing, reverting HA Proxy config"
  # Restore the LB
  restore_lb

  logger -t cloud "Reverting firewall config"
  fw_restore

  unlock_exit 1 $lock $locked
else
  # Remove backedup iptable rules
  logger -t cloud "Firewall configured successfully, deleting backup firewall config"
  fw_remove_backup
fi
 
unlock_exit 0 $lock $locked
