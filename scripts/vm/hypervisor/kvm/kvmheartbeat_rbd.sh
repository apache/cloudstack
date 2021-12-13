#!/bin/bash
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

help() {
  printf "Usage: $0 
                    -p rbd pool name
                    -n pool auth username
                    -s pool auth secret
                    -h host
                    -i source host ip
                    -r write/read hb log 
                    -c cleanup
                    -t interval between read hb log\n"
  exit 1
}
#set -x
PoolName=
PoolAuthUserName=
PoolAuthSecret=
HostIP=
SourceHostIP=
interval=
rflag=0
cflag=0

while getopts 'p:n:s:h:i:t:rc' OPTION
do
  case $OPTION in
  p)
     PoolName="$OPTARG"
     ;;
  n)
     PoolAuthUserName="$OPTARG"
     ;;
  s)
     PoolAuthSecret="$OPTARG"
     ;;
  h)
     HostIP="$OPTARG"
     ;;
  i)
     SourceHostIP="$OPTARG"
     ;;
  t)
     interval="$OPTARG"
     ;;
  r)
     rflag=1 
     ;;
  c)
    cflag=1
     ;;
  *)
     help
     ;;
  esac
done

if [ -z "$PoolName" ]; then
  exit 2
fi

keyringFile="/etc/ceph/keyring"
confFile="/etc/ceph/ceph.conf"

create_cephConf() {
#Creating Ceph keyring and conf for executing rados commands
  if [ ! -f $keyringFile ]; then
    echo -e "[client.$PoolAuthUserName]\n key=$PoolAuthSecret" > $keyringFile
  fi
  
  if [ ! -f $confFile ]; then
    confContents="[global]\n mon host = "
    for ip in $(echo $SourceHostIP | sed 's/,/ /g'); do
        confContents+="[v2:${ip}:3300/0,v1:${ip}:6789/0], "
    done
    echo -e "$confContents" | sed 's/, $//' > $confFile
  fi
}

delete_cephConf() {
#Deleting Ceph keyring
  if [ -f $keyringFile ]; then
    rm -rf $keyringFile
  fi
}

write_hbLog() {
#write the heart beat log
  timestamp=$(date +%s)
  obj=$(rados -p $PoolName ls --id $PoolAuthUserName | grep hb-$HostIP)
  if [ $? -gt 0 ]; then
     rados -p $PoolName create hb-$HostIP --id $PoolAuthUserName
  fi
  echo $timestamp | rados -p $PoolName put hb-$HostIP - --id $PoolAuthUserName
  if [ $? -gt 0 ]; then
   	printf "Failed to create rbd file"
    return 2
  fi
  return 0
}

check_hbLog() {
#check the heart beat log
  now=$(date +%s)
  hb=$(rados -p $PoolName get hb-$HostIP - --id $PoolAuthUserName)
  diff=$(expr $now - $hb)
  if [ $diff -gt $interval ]; then
    return $diff
  fi
  return 0
}

if [ "$rflag" == "1" ]; then
  create_cephConf
  check_hbLog
  diff=$?
  if [ $diff == 0 ]; then
    echo "=====> ALIVE <====="
  else
    echo "=====> Considering host as DEAD because last write on [RBD pool] was [$diff] seconds ago, but the max interval is [$interval] <======"
  fi
  delete_cephConf
  exit 0
elif [ "$cflag" == "1" ]; then
  /usr/bin/logger -t heartbeat "kvmheartbeat_rbd.sh will reboot system because it was unable to write the heartbeat to the storage."
  sync &
  sleep 5
  echo b > /proc/sysrq-trigger
  exit $?
else
  create_cephConf
  write_hbLog
  delete_cephConf
  exit 0
fi
