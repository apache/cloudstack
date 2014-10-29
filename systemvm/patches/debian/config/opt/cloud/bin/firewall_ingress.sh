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
# firewall_rule.sh -- allow some ports / protocols to vm instances
# @VERSION@

source /root/func.sh

lock="biglock"
locked=$(getLockFile $lock)
if [ "$locked" != "1" ]
then
    exit 1
fi

usage() {
  printf "Usage: %s:  -a <public ip address:protocol:startport:endport:sourcecidrs>  \n" $(basename $0) >&2
  printf "sourcecidrs format:  cidr1-cidr2-cidr3-...\n"
}
#set -x
#FIXME: eating up the error code during execution of iptables
fw_remove_backup() {
  local pubIp=$1
  sudo iptables -t mangle -F _FIREWALL_$pubIp 2> /dev/null
  sudo iptables -t mangle -D PREROUTING  -d $pubIp -j _FIREWALL_$pubIp  2> /dev/null
  sudo iptables -t mangle -X _FIREWALL_$pubIp 2> /dev/null
}

fw_restore() {
  local pubIp=$1
  sudo iptables -t mangle -F FIREWALL_$pubIp 2> /dev/null
  sudo iptables -t mangle -D PREROUTING  -d $pubIp  -j FIREWALL_$pubIp  2> /dev/null
  sudo iptables -t mangle -X FIREWALL_$pubIp 2> /dev/null
  sudo iptables -t mangle -E _FIREWALL_$pubIp FIREWALL_$pubIp 2> /dev/null
}

fw_chain_for_ip () {
  local pubIp=$1
  fw_remove_backup $1
  sudo iptables -t mangle -E FIREWALL_$pubIp _FIREWALL_$pubIp 2> /dev/null
  sudo iptables -t mangle -N FIREWALL_$pubIp 2> /dev/null
  # drop if no rules match (this will be the last rule in the chain)
  sudo iptables -t mangle -A FIREWALL_$pubIp -j DROP> /dev/null
  # ensure outgoing connections are maintained (first rule in chain)
  sudo iptables -t mangle -I FIREWALL_$pubIp -m state --state RELATED,ESTABLISHED -j ACCEPT> /dev/null
  #ensure that this table is after VPN chain
  sudo iptables -t mangle -I PREROUTING 2 -d $pubIp -j FIREWALL_$pubIp
  success=$?
  if [ $success -gt 0 ]
  then
  # if VPN chain is not present for various reasons, try to add in to the first slot */
     sudo iptables -t mangle -I PREROUTING -d $pubIp -j FIREWALL_$pubIp
  fi
}

fw_entry_for_public_ip() {
  local rules=$1

  local pubIp=$(echo $rules | cut -d: -f1)
  local prot=$(echo $rules | cut -d: -f2)
  local sport=$(echo $rules | cut -d: -f3)    
  local eport=$(echo $rules | cut -d: -f4)    
  local scidrs=$(echo $rules | cut -d: -f5 | sed 's/-/ /g')
  
  logger -t cloud "$(basename $0): enter apply firewall rules for public ip $pubIp:$prot:$sport:$eport:$scidrs"  


  # note that rules are inserted after the RELATED,ESTABLISHED rule 
  # but before the DROP rule
  for src in $scidrs
  do
    [ "$prot" == "reverted" ] && continue;
    if [ "$prot" == "icmp" ]
    then
      typecode="$sport/$eport"
      [ "$eport" == "-1" ] && typecode="$sport"
      [ "$sport" == "-1" ] && typecode="any"
      sudo iptables -t mangle -I FIREWALL_$pubIp 2 -s $src -p $prot \
                    --icmp-type $typecode  -j RETURN
    else
       sudo iptables -t mangle -I FIREWALL_$pubIp 2 -s $src -p $prot \
                    --dport $sport:$eport -j RETURN
    fi
    result=$?
    [ $result -gt 0 ] && 
       logger -t cloud "Error adding iptables entry for $pubIp:$prot:$sport:$eport:$src" &&
       break
  done
      
  logger -t cloud "$(basename $0): exit apply firewall rules for public ip $pubIp"  
  return $result
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
  
  logger -t cloud "FirewallRule public interfaces = $vif_list"
  echo $vif_list
}

shift 
rules=
while getopts 'a:' OPTION
do
  case $OPTION in
  a)	aflag=1
		rules="$OPTARG"
		;;
  ?)	usage
                unlock_exit 2 $lock $locked
		;;
  esac
done

VIF_LIST=$(get_vif_list)

if [ "$rules" == "" ]
then
  rules="none"
fi

#-a 172.16.92.44:tcp:80:80:0.0.0.0/0:,172.16.92.44:tcp:220:220:0.0.0.0/0:,172.16.92.44:tcp:222:222:192.168.10.0/24-75.57.23.0/22-88.100.33.1/32
#    if any entry is reverted , entry will be in the format <ip>:reverted:0:0:0
# example : 172.16.92.44:tcp:80:80:0.0.0.0/0:,172.16.92.44:tcp:220:220:0.0.0.0/0:,200.1.1.2:reverted:0:0:0 
# The reverted entries will fix the following partially 
#FIXME: rule leak: when there are multiple ip address, there will chance that entry will be left over if the ipadress  does not appear in the current execution when compare to old one 
# example :  In the below first transaction have 2 ip's whereas in second transaction it having one ip, so after the second trasaction 200.1.2.3 ip will have rules in mangle table.
#  1)  -a 172.16.92.44:tcp:80:80:0.0.0.0/0:,200.16.92.44:tcp:220:220:0.0.0.0/0:,
#  2)  -a 172.16.92.44:tcp:80:80:0.0.0.0/0:,172.16.92.44:tcp:220:220:0.0.0.0/0:,


success=0
publicIps=
rules_list=$(echo $rules | cut -d, -f1- --output-delimiter=" ")
for r in $rules_list
do
  pubIp=$(echo $r | cut -d: -f1)
  publicIps="$pubIp $publicIps"
done

unique_ips=$(echo $publicIps| tr " " "\n" | sort | uniq | tr "\n" " ")

for u in $unique_ips
do
  fw_chain_for_ip $u
done

for r in $rules_list
do
  pubIp=$(echo $r | cut -d: -f1)
  fw_entry_for_public_ip $r
  success=$?
  if [ $success -gt 0 ]
  then
    logger -t cloud "$(basename $0): failure to apply fw rules for ip $pubIp"
    break
  else
    logger -t cloud "$(basename $0): successful in applying fw rules for ip $pubIp"
  fi
done

if [ $success -gt 0 ]
then
    for p in $unique_ips
    do
      logger -t cloud "$(basename $0): restoring from backup for ip: $p"
      fw_restore $p
    done
fi 
for p in $unique_ips
do
   logger -t cloud "$(basename $0): deleting backup for ip: $p"
   fw_remove_backup $p
done

unlock_exit $success $lock $locked

