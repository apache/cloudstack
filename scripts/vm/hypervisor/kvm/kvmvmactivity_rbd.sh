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
                    -u volume uuid list
                    -t time on ms
                    -d suspect time\n"
  exit 1
}
#set -x
PoolName=
PoolAuthUserName=
PoolAuthSecret=
HostIP=
SourceHostIP=
UUIDList=
MSTime=
SuspectTime=

while getopts 'p:n:s:h:i:u:t:d:' OPTION
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
  u)
     UUIDList="$OPTARG"
     ;;
  t)
     MSTime="$OPTARG"
     ;;
  d)
     SuspectTime="$OPTARG"
     ;;
  *)
     help
     ;;
  esac
done

if [ -z "$PoolName" ]; then
  exit 2
fi

if [ -z "$SuspectTime" ]; then
  exit 2
fi

#Creating Ceph keyring and conf for executing rados commands
keyringFile="/etc/ceph/keyring.bin"
confFile="/etc/ceph/ceph.conf"

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

# First check: heartbeat file
now=$(date +%s)
hb=$(rados -p $PoolName get hb-$HostIP - --id $PoolAuthUserName)
diff=$(expr $now - $hb)
if [ $diff -lt 61 ]; then
    echo "=====> ALIVE <====="
    exit 0
fi

if [ -z "$UUIDList" ]; then
    echo "=====> Considering host as DEAD due to empty UUIDList <======"
    exit 0
fi

# Second check: disk activity check
lastestUUIDList=
for UUID in $(echo $UUIDList | sed 's/,/ /g'); do
    time=$(rbd info $UUID --id $PoolAuthUserName | grep modify_timestamp)
    time=${time#*modify_timestamp: }
    time=$(date -d "$time" +%s)
    lastestUUIDList+="${time}\n"
done

latestUpdateTime=$(echo -e $lastestUUIDList 2> /dev/null | sort -nr | head -1)
obj=$(rados -p $PoolName ls --id $PoolAuthUserName | grep ac-$HostIP)
if [ $? -gt 0 ]; then
    rados -p $PoolName create ac-$HostIP --id $PoolAuthUserName
    echo "$SuspectTime:$latestUpdateTime:$MSTime" | rados -p $PoolName put ac-$HostIP - --id $PoolAuthUserName
    if [[ $latestUpdateTime -gt $SuspectTime ]]; then
        echo "=====> ALIVE <====="
    else
        echo "=====> Considering host as DEAD due to file [RBD pool] does not exists and condition [latestUpdateTime -gt SuspectTime] has not been satisfied. <======"
    fi
else
    acTime=$(rados -p $PoolName get ac-$HostIP - --id $PoolAuthUserName)
    arrTime=(${acTime//:/ })
    lastSuspectTime=${arrTime[0]}
    lastUpdateTime=${arrTime[1]}
    echo "$SuspectTime:$latestUpdateTime:$MSTime" | rados -p $PoolName put ac-$HostIP - --id $PoolAuthUserName
    suspectTimeDiff=$(expr $SuspectTime - $lastSuspectTime)
    if [ $suspectTimeDiff -lt 0 ]; then
        if [[ $latestUpdateTime -gt $SuspectTime ]]; then
            echo "=====> ALIVE <====="
        else
            echo "=====> Considering host as DEAD due to file [RBD pool] exist, condition [suspectTimeDiff -lt 0] was satisfied and [latestUpdateTime -gt SuspectTime] has not been satisfied. <======"
        fi
    else
        if [[ $latestUpdateTime -gt $lastUpdateTime ]]; then
            echo "=====> ALIVE <====="
        else
            echo "=====> Considering host as DEAD due to file [RBD pool] exist and conditions [suspectTimeDiff -lt 0] and [latestUpdateTime -gt SuspectTime] have not been satisfied. <======"
        fi
    fi
fi

#Deleting Ceph keyring
if [ -f $keyringFile ]; then
    rm -rf $keyringFile
fi

exit 0