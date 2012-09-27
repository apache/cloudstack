#!/bin/sh
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

set -e

usage() {
  echo "$0 [-g |-d] <ip address>"
  echo " -g  output the gateway for this ip address"
  exit 1
}

gflag=

while getopts 'g' OPTION
do
  case $OPTION in
  g)    gflag=1
         ;;
  ?)    usage
         exit 1
         ;;
  esac
done

if [ "$gflag" != "1" ]
then
  usage
fi

shift $(($OPTIND - 1))
ipaddr=$1

[ -z "$ipaddr" ] && usage

device=$(ip addr | grep $1 | head -1 | awk '{print $NF}')
defaultdev=$(ip route | grep default | awk '{print $NF}')
if [ "$device" == "$defaultdev" ]
then
  gateway=$(ip route | grep default | awk '{print $3}')
fi

[ -n "$gflag" ] && echo $gateway && exit 0

