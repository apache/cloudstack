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
                    -u volume uuid list"
  exit 1
}
#set -x
PoolName=
PoolAuthUserName=
PoolAuthSecret=
HostIP=
SourceHostIP=
UUIDList=

while getopts 'p:n:s:h:i:u:d:' OPTION
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
  *)
     help
     ;;
  esac
done

if [ -z "$PoolName" ]; then
  exit 2
fi

#Creating Ceph keyring for executing rbd commands
keyringFile="/etc/cloudstack/agent/keyring.bin"

if [ ! -f $keyringFile ]; then
    echo -e "[client.$PoolAuthUserName]\n key=$PoolAuthSecret" > $keyringFile
fi

# First check: heartbeat watcher
status=$(rbd status hb-$HostIP --pool $PoolName -m $SourceHostIP -k $keyringFile)
if [ "$status" != "Watchers: none" ]; then
    echo "=====> ALIVE <====="
    exit 0
fi

if [ -z "$UUIDList" ]; then
    echo "=====> Considering host as DEAD due to empty UUIDList <======"
    exit 0
fi

# Second check: disk activity check
statusFlag=true
for UUID in $(echo $UUIDList | sed 's/,/ /g'); do
    diskStatus=$(rbd status $UUID --pool $PoolName -m $SourceHostIP -k $keyringFile)
    if [ "$status" == "Watchers: none" ]; then
        statusFlag=false
        break
    fi
done

if [ statusFlag == "true" ]; then
    echo "=====> ALIVE <====="
else
    echo "=====> Considering host as DEAD due to [RBD '$PoolName' pool] Image Watcher does not exists <======"
fi

#Deleting Ceph keyring
if [ -f $keyringFile ]; then
    rm -rf $keyringFile
fi

exit 0