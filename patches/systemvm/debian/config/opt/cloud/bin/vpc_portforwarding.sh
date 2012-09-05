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

lock="biglock"
locked=$(getLockFile $lock)
if [ "$locked" != "1" ]
then
    exit 1
fi

usage() {
  printf "Usage: %s: (-A|-D)   -r <target-instance-ip> -P protocol (-p port_range | -t icmp_type_code)  -l <public ip address> -d <target port> -s <source cidrs> [-G]   \n" $(basename $0) >&2
}

#set -x

#Port (address translation) forwarding for tcp or udp
tcp_or_udp_nat() {
  local op=$1
  local proto=$2
  local publicIp=$3
  local ports=$4
  local instIp=$5
  local dports=$6

  logger -t cloud "$(basename $0): creating port fwd entry for PAT: public ip=$publicIp \
  instance ip=$instIp proto=$proto port=$port dport=$dport op=$op"

  #if adding, this might be a duplicate, so delete the old one first
  [ "$op" == "-A" ] && tcp_or_udp_nat "-D" $proto $publicIp $ports $instIp $dports
  # the delete operation may have errored out but the only possible reason is 
  # that the rules didn't exist in the first place
  # shortcircuit the process if error and it is an append operation
  # continue if it is delete
  local PROTO=""
  if [ "$proto" != "any" ]
  then
    PROTO="--proto $proto"
  fi

  local DEST_PORT=""
  if [ "$ports" != "any" ]
  then
    DEST_PORT="--destination-port $ports"
  fi
  
  local TO_DEST="--to-destination $instIp"
  if [ "$dports" != "any" ]
  then
    TO_DEST="--to-destination $instIp:$dports"
  fi

  sudo iptables -t nat $op PREROUTING $PROTO -d $publicIp  $DEST_PORT -j DNAT  \
           $TO_DEST &>> $OUTFILE 
        
  local result=$?
  logger -t cloud "$(basename $0): done port fwd entry for PAT: public ip=$publicIp op=$op result=$result"
  # the rule may not exist
  if [ "$op" == "-D" ]
  then
    return 0
  fi
  return $result
}


rflag=
Pflag=
pflag=
lflag=
dflag=
op=""
protocal="any"
ports="any"
dports="any"
while getopts 'ADr:P:p:l:d:' OPTION
do
  case $OPTION in
  A)    op="-A"
        ;;
  D)    op="-D"
        ;;
  r)    rflag=1
        instanceIp="$OPTARG"
        ;;
  P)    Pflag=1
        protocol="$OPTARG"
        ;;
  p)    pflag=1
        ports="$OPTARG"
        ;;
  l)    lflag=1
        publicIp="$OPTARG"
        ;;
  d)    dflag=1
        dports="$OPTARG"
        ;;
  ?)    usage
        unlock_exit 2 $lock $locked
        ;;
  esac
done

OUTFILE=$(mktemp)

tcp_or_udp_nat $op $protocol $publicIp $ports $instanceIp $dports
result=$?
unlock_exit $result $lock $locked
