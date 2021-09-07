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
                    -i nfs server ip
                    -p nfs server path
                    -m mount point
                    -h host
                    -u volume uuid list
                    -t time on ms
                    -d suspect time\n"
  exit 1
}

#set -x

NfsSvrIP=
NfsSvrPath=
MountPoint=
HostIP=
UUIDList=
MSTime=
SuspectTime=

while getopts 'i:p:m:u:t:h:d:' OPTION
do
  case $OPTION in
  i)
     NfsSvrIP="$OPTARG"
     ;;
  p)
     NfsSvrPath="$OPTARG"
     ;;
  m)
     MountPoint="$OPTARG"
     ;;
  h)
     HostIP="$OPTARG"
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

if [ -z "$NfsSvrIP" ]
then
   exit 2
fi

if [ -z "$SuspectTime" ]
then
   exit 2
fi

hbFile="$MountPoint/KVMHA/hb-$HostIP"
acFile="$MountPoint/KVMHA/ac-$HostIP"

# First check: heartbeat file
now=$(date +%s)
hb=$(cat $hbFile)
diff=$(expr $now - $hb)
if [ $diff -lt 61 ]
then
  echo "=====> ALIVE <====="
  exit 0
fi

if [ -z "$UUIDList" ]
then
  echo "=====> Considering host as DEAD due to empty UUIDList <======"
  exit 0
fi

# Second check: disk activity check
cd $MountPoint
latestUpdateTime=$(stat -c %Y $(echo $UUIDList | sed 's/,/ /g') 2> /dev/null | sort -nr | head -1)

if [ ! -f $acFile ]; then
    echo "$SuspectTime:$latestUpdateTime:$MSTime" > $acFile

    if [[ $latestUpdateTime -gt $SuspectTime ]]; then
        echo "=====> ALIVE <====="
    else
        echo "=====> Considering host as DEAD due to file [$acFile] does not exists and condition [latestUpdateTime -gt SuspectTime] has not been satisfied. <======"
    fi
else
    acTime=$(cat $acFile)
    arrTime=(${acTime//:/ })
    lastSuspectTime=${arrTime[0]}
    lastUpdateTime=${arrTime[1]}
    echo "$SuspectTime:$latestUpdateTime:$MSTime" > $acFile

    suspectTimeDiff=$(expr $SuspectTime - $lastSuspectTime)
    if [[ $suspectTimeDiff -lt 0 ]]; then
        if [[ $latestUpdateTime -gt $SuspectTime ]]; then
            echo "=====> ALIVE <====="
        else
            echo "=====> Considering host as DEAD due to file [$acFile] exist, condition [suspectTimeDiff -lt 0] was satisfied and [latestUpdateTime -gt SuspectTime] has not been satisfied. <======"
        fi
    else
        if [[ $latestUpdateTime -gt $lastUpdateTime ]]; then
            echo "=====> ALIVE <====="
        else
            echo "=====> Considering host as DEAD due to file [$acFile] exist and conditions [suspectTimeDiff -lt 0] and [latestUpdateTime -gt SuspectTime] have not been satisfied. <======"
        fi
    fi
fi

exit 0
