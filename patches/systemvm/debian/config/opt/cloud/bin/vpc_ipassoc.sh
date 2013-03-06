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

 

# ipassoc.sh -- associate/disassociate a public ip with an instance
# @VERSION@

source /root/func.sh

lock="biglock"
locked=$(getLockFile $lock)
if [ "$locked" != "1" ]
then
  exit 1
fi

usage() {
  printf "Usage:\n %s -A -l <public-ip-address> -c <dev> [-f] \n" $(basename $0) >&2
  printf " %s -D -l <public-ip-address> -c <dev> [-f] \n" $(basename $0) >&2
}

add_routing() {
  logger -t cloud "$(basename $0):Add routing $pubIp on interface $ethDev"

  local tableName="Table_$ethDev"
  sudo ip route add $subnet/$mask dev $ethDev table $tableName proto static
  sudo ip route add default via $defaultGwIP table $tableName proto static
  sudo ip route flush cache
  sudo ip route | grep default
  if [ $? -gt 0 ]
  then
    sudo ip route add default via $defaultGwIP
  fi
  return 0
}


remove_routing() {
  return 0
}

add_an_ip () {
  # need to wait for eth device to appear before configuring it
  timer=0
  while ! `grep -q $ethDev /proc/net/dev` ; do
    logger -t cloud "$(basename $0):Waiting for interface $ethDev to appear, $timer seconds"
    sleep 1;
    if [ $timer -gt 15 ]; then
      logger -t cloud "$(basename $0):interface $ethDev never appeared"
      break
    fi
    timer=$[timer + 1]
  done

  logger -t cloud "$(basename $0):Adding ip $pubIp on interface $ethDev"
  sudo ip link show $ethDev | grep "state DOWN" > /dev/null
  local old_state=$?

  sudo ip addr add dev $ethDev $pubIp/$mask brd +
  if [ $old_state -eq 0 ]
  then
    sudo ip link set $ethDev up
    sudo arping -c 3 -I $ethDev -A -U -s $pubIp $pubIp
  fi
  local tableNo=${ethDev:3} 
  sudo iptables-save -t mangle | grep  "PREROUTING -i $ethDev -m state --state NEW -j CONNMARK --set-xmark" 2>/dev/null
  if [ $? -gt 0 ]
  then
    sudo iptables -t mangle -A PREROUTING -i $ethDev -m state --state NEW -j CONNMARK --set-mark $tableNo 2>/dev/null
  fi
  add_routing 
  return $?
}

remove_an_ip () {
  logger -t cloud "$(basename $0):Removing ip $pubIp on interface $ethDev"
  local existingIpMask=$(sudo ip addr show dev $ethDev | grep -v "inet6" | grep "inet " | awk '{print $2}')

  sudo ip addr del dev $ethDev $pubIp/$mask
  # reapply IPs in this interface
  for ipMask in $existingIpMask
  do
    if [ "$ipMask" == "$pubIp/$mask" ]
    then
      continue
    fi
    sudo ip addr add dev $ethDev $ipMask brd +
  done

  remove_routing
  return 0
}

#set -x
lflag=0
cflag=0
gflag=0
mflag=0
nflag=0
op=""


while getopts 'ADl:c:g:m:n:' OPTION
do
  case $OPTION in
  A)	Aflag=1
		op="-A"
		;;
  D)	Dflag=1
		op="-D"
		;;
  l)	lflag=1
		pubIp="$OPTARG"
		;;
  c)	cflag=1
  		ethDev="$OPTARG"
  		;;
  g)	gflag=1
  		defaultGwIP="$OPTARG"
  		;;
  m)	mflag=1
  		mask="$OPTARG"
  		;;
  n)	nflag=1
  		subnet="$OPTARG"
  		;;
  ?)	usage
                unlock_exit 2 $lock $locked
		;;
  esac
done


if [ "$Aflag$Dflag" != "1" ]
then
  usage
  unlock_exit 2 $lock $locked
fi

if [ "$lflag$cflag$gflag$mflag$nflag" != "11111" ] 
then
  usage
  unlock_exit 2 $lock $locked
fi


if [ "$Aflag" == "1" ]
then
  add_an_ip
  unlock_exit $? $lock $locked
fi


if [ "$Dflag" == "1" ]
then
  remove_an_ip
  unlock_exit $? $lock $locked
fi


unlock_exit 1 $lock $locked
