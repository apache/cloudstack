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
# vpc_privategw_acl.sh_rule.sh -- allow/block some ports / protocols to vm instances
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

acl_switch_to_new() {
  sudo iptables -D FORWARD -o $dev  -j _ACL_INBOUND_$dev  2>/dev/null
  sudo iptables-save  | grep "\-j _ACL_INBOUND_$dev" | grep "\-A" | while read rule;
  do
    rule1=$(echo $rule | sed 's/\_ACL_INBOUND/ACL_INBOUND/')
    sudo iptables $rule1
    rule2=$(echo $rule | sed 's/\-A/\-D/')
    sudo iptables $rule2
  done
  sudo iptables -F _ACL_INBOUND_$dev 2>/dev/null
  sudo iptables -X _ACL_INBOUND_$dev 2>/dev/null
  sudo iptables -t mangle -F _ACL_OUTBOUND_$dev 2>/dev/null
  sudo iptables -t mangle -D PREROUTING -m state --state NEW -i $dev  -j _ACL_OUTBOUND_$dev  2>/dev/null
  sudo iptables -t mangle -X _ACL_OUTBOUND_$dev 2>/dev/null
}

acl_remove_backup() {
  sudo iptables -F _ACL_INBOUND_$dev 2>/dev/null
  sudo iptables -D FORWARD -o $dev  -j _ACL_INBOUND_$dev  2>/dev/null
  sudo iptables -X _ACL_INBOUND_$dev 2>/dev/null
  sudo iptables -t mangle -F _ACL_OUTBOUND_$dev 2>/dev/null
  sudo iptables -t mangle -D PREROUTING -m state --state NEW -i $dev  -j _ACL_OUTBOUND_$dev  2>/dev/null
  sudo iptables -t mangle -X _ACL_OUTBOUND_$dev 2>/dev/null
}

acl_remove() {
  sudo iptables -F ACL_INBOUND_$dev 2>/dev/null
  sudo iptables -D FORWARD -o $dev  -j ACL_INBOUND_$dev  2>/dev/null
  sudo iptables -X ACL_INBOUND_$dev 2>/dev/null
  sudo iptables -t mangle -F ACL_OUTBOUND_$dev 2>/dev/null
  sudo iptables -t mangle -D PREROUTING -m state --state NEW -i $dev  -j ACL_OUTBOUND_$dev  2>/dev/null
  sudo iptables -t mangle -X ACL_OUTBOUND_$dev 2>/dev/null
}

acl_restore() {
  acl_remove
  sudo iptables -E _ACL_INBOUND_$dev ACL_INBOUND_$dev 2>/dev/null
  sudo iptables -t mangle -E _ACL_OUTBOUND_$dev ACL_OUTBOUND_$dev 2>/dev/null
}

acl_save() {
  acl_remove_backup
  sudo iptables -E ACL_INBOUND_$dev _ACL_INBOUND_$dev 2>/dev/null
  sudo iptables -t mangle -E ACL_OUTBOUND_$dev _ACL_OUTBOUND_$dev 2>/dev/null
}

acl_chain_for_guest_network () {
  acl_save
  # inbound
  sudo iptables -N ACL_INBOUND_$dev 2>/dev/null
  # drop if no rules match (this will be the last rule in the chain)
  sudo iptables -A ACL_INBOUND_$dev -j DROP 2>/dev/null
  sudo iptables -A FORWARD -o $dev  -j ACL_INBOUND_$dev  2>/dev/null
  # outbound
  sudo iptables -t mangle -N ACL_OUTBOUND_$dev 2>/dev/null
  sudo iptables -t mangle -A PREROUTING -m state --state NEW -i $dev  -j ACL_OUTBOUND_$dev  2>/dev/null
}



acl_entry_for_guest_network() {
  local rule=$1

  local ttype=$(echo $rule | cut -d: -f1)
  local prot=$(echo $rule | cut -d: -f2)
  local sport=$(echo $rule | cut -d: -f3)
  local eport=$(echo $rule | cut -d: -f4)
  local cidrs=$(echo $rule | cut -d: -f5 | sed 's/-/ /g')
  local action=$(echo $rule | cut -d: -f6)
  if [ "$sport" == "0" -a "$eport" == "0" ]
  then
      DPORT=""
  else
      DPORT="--dport $sport:$eport"
  fi
  logger -t cloud "$(basename $0): enter apply acl rules on private gateway interface : $dev, inbound:$inbound:$prot:$sport:$eport:$cidrs"

  # note that rules are inserted after the RELATED,ESTABLISHED rule
  # but before the DROP rule
  for lcidr in $cidrs
  do
    [ "$prot" == "reverted" ] && continue;
    if [ "$prot" == "icmp" ]
    then
      typecode="$sport/$eport"
      [ "$eport" == "-1" ] && typecode="$sport"
      [ "$sport" == "-1" ] && typecode="any"
      if [ "$ttype" == "Ingress" ]
      then
        sudo iptables -I ACL_INBOUND_$dev -p $prot -s $lcidr  \
                    --icmp-type $typecode  -j $action
      else
        let egress++
        sudo iptables -t mangle -I ACL_OUTBOUND_$dev -p $prot -d $lcidr  \
                    --icmp-type $typecode  -j $action
      fi
    else
      if [ "$ttype" == "Ingress" ]
      then
        sudo iptables -I ACL_INBOUND_$dev -p $prot -s $lcidr \
                    $DPORT -j $action
      else
        let egress++
        sudo iptables -t mangle -I ACL_OUTBOUND_$dev -p $prot -d $lcidr \
                    $DPORT -j $action
      fi
    fi
    result=$?
    [ $result -gt 0 ] &&
       logger -t cloud "Error adding iptables entry for private gateway interface : $dev,inbound:$inbound:$prot:$sport:$eport:$cidrs" &&
       break
  done

  logger -t cloud "$(basename $0): exit apply acl rules for private gw interface : $dev"
  return $result
}


dflag=0
gflag=0
aflag=0
rules=""
rules_list=""
dev=""
while getopts 'd:a:' OPTION
do
  case $OPTION in
  d)    dflag=1
                dev="$OPTARG"
                ;;
  a)    aflag=1
        rules="$OPTARG"
        ;;
  ?)    usage
                unlock_exit 2 $lock $locked
        ;;
  esac
done

if [ "$dflag$aflag" != "11" ]
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
#-a tcp:80:80:0.0.0.0/0::tcp:220:220:0.0.0.0/0:,172.16.92.44:tcp:222:222:192.168.10.0/24-75.57.23.0/22-88.100.33.1/32
#    if any entry is reverted , entry will be in the format <ip>:reverted:0:0:0
# example : 172.16.92.44:tcp:80:80:0.0.0.0/0:ACCEPT:,172.16.92.44:tcp:220:220:0.0.0.0/0:DROP,200.1.1.2:reverted:0:0:0

success=0

acl_chain_for_guest_network
egress=0
for r in $rules_list
do
  acl_entry_for_guest_network $r
  success=$?
  if [ $success -gt 0 ]
  then
    logger -t cloud "$(basename $0): failure to apply acl rules on private gateway interface : $dev"
    break
  else
    logger -t cloud "$(basename $0): successful in applying acl rules on private gateway interface : $dev"
  fi
done

if [ $success -gt 0 ]
then
  logger -t cloud "$(basename $0): restoring from backup on private gateway interface : $dev"
  acl_restore
else
  logger -t cloud "$(basename $0): deleting backup on private gateway interface : $dev"
  if [ $egress -eq 0 ]
  then
    sudo iptables -t mangle -A ACL_OUTBOUND_$dev -j ACCEPT 2>/dev/null
  else
    sudo iptables -t mangle -A ACL_OUTBOUND_$dev -j DROP 2>/dev/null
  fi
  acl_switch_to_new
fi
unlock_exit $success $lock $locked
