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
  printf "Usage:\n %s -A -l <public-ip-address>\n" $(basename $0) >&2
  printf " %s -D -l <public-ip-address>\n" $(basename $0) >&2
}


add_snat() {
  logger -t cloud "$(basename $0):Added SourceNAT $pubIp on interface $ethDev"
  vpccidr=$(getVPCcidr)
  sudo iptables -D FORWARD -s $vpccidr ! -d $vpccidr -j ACCEPT
  sudo iptables -A FORWARD -s $vpccidr ! -d $vpccidr -j ACCEPT
  sudo iptables -t nat -D POSTROUTING   -j SNAT -o $ethDev --to-source $pubIp
  sudo iptables -t nat -A POSTROUTING   -j SNAT -o $ethDev --to-source $pubIp
  return $?
}
remove_snat() {
  logger -t cloud "$(basename $0):Removing SourceNAT $pubIp on interface $ethDev"
  sudo iptables -t nat -D POSTROUTING   -j SNAT -o $ethDev --to-source $pubIp
  return $?
}

#set -x
lflag=0
cflag=0
op=""

while getopts 'ADl:c:' OPTION
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

if [ "$lflag$cflag" != "11" ]
then
  usage
  unlock_exit 2 $lock $locked
fi

if [ "$Aflag" == "1" ]
then
  add_snat  $publicIp
  unlock_exit $? $lock $locked
fi

if [ "$Dflag" == "1" ]
then
  remove_sat  $publicIp
  unlock_exit $? $lock $locked
fi

unlock_exit 1 $lock $locked
