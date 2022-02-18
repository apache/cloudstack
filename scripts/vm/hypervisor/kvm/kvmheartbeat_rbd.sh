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
                    -r cretae/read hb watcher
                    -c cleanup"
  exit 1
}
#set -x
PoolName=
PoolAuthUserName=
PoolAuthSecret=
HostIP=
SourceHostIP=
rflag=0
cflag=0

while getopts 'p:n:s:h:i:rc' OPTION
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

keyringFile="/etc/cloudstack/agent/keyring"

create_cephKeyring() {
#Creating Ceph keyring for executing rbd commands
  if [ ! -f $keyringFile ]; then
    echo -e "[client.$PoolAuthUserName]\n key=$PoolAuthSecret" > $keyringFile
  fi
}

delete_cephKeyring() {
#Deleting Ceph keyring
  if [ -f $keyringFile ]; then
    rm -rf $keyringFile
  fi
}

cretae_hbWatcher() {
#Create HB RBD Image and watcher
  status=$(rbd status hb-$HostIP -m $SourceHostIP -k $keyringFile)
  if [ $? == 2 ]; then
    rbd create hb-$HostIP --size 1 -m $SourceHostIP -k $keyringFile
    setsid sh -c 'exec rbd watch hb-'$HostIP' -m '$SourceHostIP' -k '$keyringFile' <> /dev/tty20 >&0 2>&1'
  fi

  if [ "$status" == "Watchers: none" ]; then
    setsid sh -c 'exec rbd watch hb-'$HostIP' -m '$SourceHostIP' -k '$keyringFile' <> /dev/tty20 >&0 2>&1'
  fi
  
  return 0
}

check_hbWatcher() {
#check the heart beat watcher
  hb=$(rbd status hb-$HostIP -m $SourceHostIP -k $keyringFile)
  if [ "$hb" == "Watchers: none" ]; then
    return 2
  else
    return 0
  fi
}

if [ "$rflag" == "1" ]; then
  create_cephKeyring
  check_hbWatcher
  hb=$?
  if [ "$hb" != "Watchers: none" ]; then
    echo "=====> ALIVE <====="
  else
    echo "=====> Considering host as DEAD due to [hb-$HostIP] watcher that the host is seeing is not running. <======"
  fi
  delete_cephKeyring
  exit 0
elif [ "$cflag" == "1" ]; then
  /usr/bin/logger -t heartbeat "kvmheartbeat_rbd.sh reboots the system because there is no heartbeat watcher."
  sync &
  sleep 5
  echo b > /proc/sysrq-trigger
  exit $?
else
  create_cephKeyring
  cretae_hbWatcher
  delete_cephKeyring
  exit 0
fi
