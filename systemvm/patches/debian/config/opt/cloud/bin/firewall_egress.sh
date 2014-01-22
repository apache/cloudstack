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
# $Id: firewallRule_egress.sh 9947 2013-01-17 19:34:24Z manuel $ $HeadURL: svn://svn.lab.vmops.com/repos/vmdev/java/patches/xenserver/root/firewallRule_egress.sh $
# firewallRule_egress.sh -- allow some ports / protocols from vm instances
# @VERSION@

source /root/func.sh

lock="biglock"
locked=$(getLockFile $lock)
if [ "$locked" != "1" ]
then
    exit 1
fi
#set -x
usage() {
  printf "Usage: %s:  -a protocol:startport:endport:sourcecidrs>  \n" $(basename $0) >&2
  printf "sourcecidrs format:  cidr1-cidr2-cidr3-...\n"
}

fw_egress_remove_backup() {
  sudo iptables -D FW_OUTBOUND -j _FW_EGRESS_RULES 
  sudo iptables -F _FW_EGRESS_RULES 
  sudo iptables -X _FW_EGRESS_RULES 
}

fw_egress_save() {
  sudo iptables -E FW_EGRESS_RULES _FW_EGRESS_RULES 
}

fw_egress_chain () {
#supress errors 2>/dev/null
  fw_egress_remove_backup
  fw_egress_save
  sudo iptables -N FW_EGRESS_RULES 
  sudo iptables -A FW_OUTBOUND -j FW_EGRESS_RULES
}

fw_egress_backup_restore() {
   sudo iptables -A FW_OUTBOUND -j FW_EGRESS_RULES
   sudo iptables -E _FW_EGRESS_RULES FW_EGRESS_RULES 
   fw_egress_remove_backup
}


fw_entry_for_egress() {
  local rule=$1

  local prot=$(echo $rule | cut -d: -f2)
  local sport=$(echo $rule | cut -d: -f3)
  local eport=$(echo $rule | cut -d: -f4)
  local cidrs=$(echo $rule | cut -d: -f5 | sed 's/-/ /g')
  if [ "$sport" == "0" -a "$eport" == "0" ]
  then
      DPORT=""
  else
      DPORT="--dport $sport:$eport"
  fi
  logger -t cloud "$(basename $0): enter apply fw egress rules for guest $prot:$sport:$eport:$cidrs"  
  
  for lcidr in $cidrs
  do
    [ "$prot" == "reverted" ] && continue;
    if [ "$prot" == "icmp" ]
    then
      typecode="$sport/$eport"
      [ "$eport" == "-1" ] && typecode="$sport"
      [ "$sport" == "-1" ] && typecode="any"
      sudo iptables -A FW_EGRESS_RULES -p $prot -s $lcidr --icmp-type $typecode \
                     -j $target
      result=$?
    elif [ "$prot" == "all" ]
    then
	    sudo iptables -A FW_EGRESS_RULES -p $prot -s $lcidr -j $target
	    result=$?
    else
	    sudo iptables -A FW_EGRESS_RULES -p $prot -s $lcidr  $DPORT -j $target
	    result=$?
    fi
  
    [ $result -gt 0 ] && 
       logger -t cloud "Error adding iptables entry for guest network $prot:$sport:$eport:$cidrs" &&
       break
  done

  logger -t cloud "$(basename $0): exit apply egress firewall rules for guest network"  
  return $result
}


aflag=0
rules=""
rules_list=""
ip=""
dev=""
pflag=0
shift
shift
while getopts 'a:P:' OPTION
do
  case $OPTION in
  a)	aflag=1
		rules="$OPTARG"
		;;
  P)   pflag=1
       pvalue="$OPTARG"
       ;;
  ?)	usage
                unlock_exit 2 $lock $locked
		;;
  esac
done

if [ "$aflag" != "1" ]
then
  usage
  unlock_exit 2 $lock $locked
fi

if [ -n "$rules" ]
then
  rules_list=$(echo $rules | cut -d, -f1- --output-delimiter=" ")
fi

# rule format
# protocal:sport:eport:cidr
#-a tcp:80:80:0.0.0.0/0::tcp:220:220:0.0.0.0/0:,tcp:222:222:192.168.10.0/24-75.57.23.0/22-88.100.33.1/32
#    if any entry is reverted , entry will be in the format reverted:0:0:0
# example : tcp:80:80:0.0.0.0/0:, tcp:220:220:0.0.0.0/0:,200.1.1.2:reverted:0:0:0 

success=0

if [ "$pvalue" == "0" -o "$pvalue" == "2" ]
  then
     target="ACCEPT"
  else
     target="DROP"
  fi

fw_egress_chain
for r in $rules_list
do
  fw_entry_for_egress $r
  success=$?
  if [ $success -gt 0 ]
  then
    logger -t cloud "failure to apply fw egress rules "
    break
  else
    logger -t cloud "successful in applying fw egress rules"
  fi
done

if [ $success -gt 0 ]
then
  logger -t cloud "restoring from backup for guest network"
  fw_egress_backup_restore
else
  logger -t cloud "deleting backup for guest network"
    if [ "$pvalue" == "1" -o "$pvalue" == "2" ]
       then
       #Adding default policy rule
       sudo iptables -A FW_EGRESS_RULES  -j ACCEPT
    fi

fi

fw_egress_remove_backup

unlock_exit $success $lock $locked


