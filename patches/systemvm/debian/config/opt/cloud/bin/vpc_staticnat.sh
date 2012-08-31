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

source /root/func.sh
source /opt/cloud/bin/vpc_func.sh
lock="biglock"
locked=$(getLockFile $lock)
if [ "$locked" != "1" ]
then
    exit 1
fi

usage() {
  printf "Usage: %s: (-A|-D)   -r <target-instance-ip>  -l <public ip address> -d < eth device>  \n" $(basename $0) >&2
}

#set -x

vpnoutmark="0x525"

static_nat() {
  local op=$1
  local publicIp=$2
  local instIp=$3
  local op2="-D"
  local tableNo=${ethDev:3}

  logger -t cloud "$(basename $0): static nat: public ip=$publicIp \
  instance ip=$instIp  op=$op"
  #if adding, this might be a duplicate, so delete the old one first
  [ "$op" == "-A" ] && static_nat "-D" $publicIp $instIp 
  # the delete operation may have errored out but the only possible reason is 
  # that the rules didn't exist in the first place
  [ "$op" == "-A" ] && op2="-I"
  if [ "$op" == "-A" ]
  then
    # put static nat rule one rule after VPN no-NAT rule
    # rule chain can be used to improve it later
    iptables-save -t nat|grep "POSTROUTING" | grep $vpnoutmark > /dev/null
    if [ $? -eq 0 ]
    then
      rulenum=2
    else
      rulenum=1
    fi
  fi

  # shortcircuit the process if error and it is an append operation
  # continue if it is delete
  (sudo iptables -t nat $op  PREROUTING -d $publicIp -j DNAT \
           --to-destination $instIp &>>  $OUTFILE || [ "$op" == "-D" ]) &&
  # add mark to force the package go out through the eth the public IP is on
  #(sudo iptables -t mangle $op PREROUTING -s $instIp -j MARK \
  #         --set-mark $tableNo &> $OUTFILE ||  [ "$op" == "-D" ]) &&
  (sudo iptables -t nat $op2 POSTROUTING $rulenum -o $ethDev -s $instIp -j SNAT \
           --to-source $publicIp &>> $OUTFILE )
  result=$?
  logger -t cloud "$(basename $0): done static nat entry public ip=$publicIp op=$op result=$result"
  if [ "$op" == "-D" ]
  then
    return 0
  fi
  return $result
}



rflag=
lflag=
dflag=
op=""
while getopts 'ADr:l:' OPTION

do
  case $OPTION in
  A)    op="-A"
        ;;
  D)    op="-D"
        ;;
  r)    rflag=1
        instanceIp="$OPTARG"
        ;;
  l)    lflag=1
        publicIp="$OPTARG"
        ;;
  ?)    usage
        unlock_exit 2 $lock $locked
        ;;
  esac
done

ethDev=$(getEthByIp $publicIp)
result=$?
if [ $result -gt 0 ]
then
  if [ "$op" == "-D" ]
  then 
    removeRulesForIp $publicIp
    unlock_exit 0 $lock $locked
  else
    unlock_exit $result $lock $locked
  fi
fi
OUTFILE=$(mktemp)

static_nat $op $publicIp $instanceIp
result=$?
unlock_exit $result $lock $locked
