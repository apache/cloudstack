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

 

# netusage.sh -- create iptable rules to gather network stats, running within DomR

source /root/func.sh

lock="biglock"
locked=$(getLockFile $lock)
if [ "$locked" != "1" ]
then
    exit 1
fi

usage() {
  printf "Usage: %s -[c|g|r] [-[a|d] <public interface>]\n" $(basename $0)  >&2
}

create_usage_rules () {
  iptables-save|grep "INPUT -j NETWORK_STATS" > /dev/null
  if [ $? -eq 0 ]
  then
      return $?
  fi
  iptables -N NETWORK_STATS > /dev/null
  iptables -I FORWARD -j NETWORK_STATS > /dev/null
  iptables -I INPUT -j NETWORK_STATS > /dev/null
  iptables -I OUTPUT -j NETWORK_STATS > /dev/null
  iptables -A NETWORK_STATS -i eth0 -o eth2 > /dev/null
  iptables -A NETWORK_STATS -i eth2 -o eth0 > /dev/null
  iptables -A NETWORK_STATS -o eth2 ! -i eth0 -p tcp > /dev/null
  iptables -A NETWORK_STATS -i eth2 ! -o eth0 -p tcp > /dev/null
  return $?
}

add_public_interface () {
  local pubIf=$1
  iptables-save|grep "NETWORK_STATS -i eth0 -o $pubIf" > /dev/null
  if [ $? -eq 0 ]
  then
      return $?
  fi
  iptables -A NETWORK_STATS -i eth0 -o $pubIf > /dev/null
  iptables -A NETWORK_STATS -i $pubIf -o eth0 > /dev/null
  iptables -A NETWORK_STATS -o $pubIf ! -i eth0 -p tcp > /dev/null
  iptables -A NETWORK_STATS -i $pubIf ! -o eth0 -p tcp > /dev/null
  return $?
}

delete_public_interface () {
  local pubIf=$1
  echo $pubIf >> /root/removedVifs
  return $?
}

get_usage () {
  iptables -L NETWORK_STATS -n -v -x | awk '$1 ~ /^[0-9]+$/ { printf "%s:", $2}'; > /dev/null
  if [ -f /root/removedVifs ] ; then iptables -Z NETWORK_STATS ; fi; > /dev/null
  /root/clearUsageRules.sh > /dev/null
  if [ $? -gt 0  -a $? -ne 2 ]
  then
     printf $?
     return 1
  fi
}

reset_usage () {
  iptables -Z NETWORK_STATS > /dev/null
  if [ $? -gt 0  -a $? -ne 2 ]
  then
     return 1
  fi
}

#set -x

cflag=
gflag=
rflag=
iflag=
aflag=
dflag=

while getopts 'cgria:d:' OPTION
do
  case $OPTION in
  c)	cflag=1
		;;
  g)	gflag=1
		;;
  r)	rflag=1
		;;
  a)    aflag=1
        publicIf="$OPTARG"
        ;;
  d)    dflag=1
        publicIf="$OPTARG"
        ;;
  i)    #Do nothing, since it's parameter for host script
        ;;
  ?)	usage
                unlock_exit 2 $lock $locked
		;;
  esac
done

if [ "$cflag" == "1" ] 
then
  #create_usage_rules  
  unlock_exit $? $lock $locked
fi

if [ "$gflag" == "1" ] 
then
  get_usage 
  unlock_exit $? $lock $locked
fi

if [ "$rflag" == "1" ] 
then
  reset_usage  
  unlock_exit $? $lock $locked
fi

if [ "$aflag" == "1" ] 
then
  #add_public_interface $publicIf 
  unlock_exit $? $lock $locked
fi

if [ "$dflag" == "1" ] 
then
  #delete_public_interface $publicIf 
  unlock_exit $? $lock $locked
fi

unlock_exit 0 $lock $locked

