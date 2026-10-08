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

# Ceph RBD flavor of kvmvmactivity.sh.
#
# On NFS/SharedMountPoint storage, VM disk activity is detected via the mtime
# of the volume files on the shared mount point. RBD volumes aren't files on
# a mount point, so instead activity is detected via RBD watchers: as long as
# qemu has an RBD image open (i.e. a VM using that volume is running
# somewhere), the image will have a live watcher. The most recent
# suspect-time/watcher-state is persisted as a RADOS object (per host)
# in place of the "ac-<host>" file used by the NFS/SMP scripts.

help() {
  printf "Usage: $0
                    -s ceph monitor host(s), comma separated
                    -o ceph/rbd pool name
                    -n cephx auth user (optional)
                    -k cephx auth key, base64 (optional, required if -n is set)
                    -h host
                    -u volume (rbd image) uuid list
                    -t current time in seconds (accepted for compatibility with kvmvmactivity.sh, not used)
                    -d suspect time\n"
  exit 1
}

#set -x

MonHosts=
PoolName=
CephUser=
CephKey=
HostIP=
UUIDList=
SuspectTime=

while getopts 's:o:n:k:h:u:t:d:' OPTION
do
  case $OPTION in
  s)
     MonHosts="$OPTARG"
     ;;
  o)
     PoolName="$OPTARG"
     ;;
  n)
     CephUser="$OPTARG"
     ;;
  k)
     CephKey="$OPTARG"
     ;;
  h)
     HostIP="$OPTARG"
     ;;
  u)
     UUIDList="$OPTARG"
     ;;
  t)
     # not used, see help
     ;;
  d)
     SuspectTime="$OPTARG"
     ;;
  *)
     help
     ;;
  esac
done

if [ -z "$MonHosts" ] || [ -z "$PoolName" ]
then
   exit 2
fi

if [ -z "$SuspectTime" ]
then
   exit 2
fi

# the host IP names the heartbeat and activity objects
if [ -z "$HostIP" ]
then
   exit 2
fi

if [ -n "$CephUser" ] && [ -z "$CephKey" ]
then
   exit 2
fi

RadosOpts=(--mon-host "$MonHosts")
RbdOpts=(--mon-host "$MonHosts")
if [ -n "$CephUser" ]
then
   # the key is given to rados and rbd in a file, to keep it out of the process list
   KeyFile=$(mktemp)
   trap 'rm -f "$KeyFile"' EXIT
   printf '%s' "$CephKey" > "$KeyFile"
   RadosOpts+=(--id "$CephUser" --keyfile "$KeyFile")
   RbdOpts+=(--id "$CephUser" --keyfile "$KeyFile")
fi

hbObject="KVMHA-hb-$HostIP"
acObject="KVMHA-ac-$HostIP"

# First check: heartbeat object, same as kvmheartbeat_rbd.sh
now=$(date +%s)
hb=$(rados -p "$PoolName" "${RadosOpts[@]}" get "$hbObject" - 2> /dev/null)
if [[ "$hb" =~ ^[0-9]+$ ]]
then
  diff=$(expr $now - $hb)
  if [ $diff -lt 61 ]
  then
    echo "=====> ALIVE <====="
    exit 0
  fi
fi

if [ -z "$UUIDList" ]
then
  echo "=====> Considering host as DEAD due to empty UUIDList <======"
  exit 0
fi

# Second check: RBD watcher based disk activity check.
# If any of the host's volumes still has a live watcher, something (most
# likely qemu on the host being checked) is actively using it right now.
latestUpdateTime=0
IFS=',' read -ra images <<< "$UUIDList"
for image in "${images[@]}"
do
  image=${image//[[:space:]]/}
  if [ -z "$image" ]
  then
    continue
  fi
  watcherCount=$(rbd status "$PoolName/$image" "${RbdOpts[@]}" --format json 2> /dev/null | \
    python3 -c 'import json,sys
try:
    print(len(json.load(sys.stdin).get("watchers", [])))
except Exception:
    print(0)' 2> /dev/null)
  if [ -n "$watcherCount" ] && [ "$watcherCount" -gt 0 ] 2> /dev/null
  then
    latestUpdateTime=$now
    break
  fi
done

if rados -p "$PoolName" "${RadosOpts[@]}" stat "$acObject" &> /dev/null
then
  acTime=$(rados -p "$PoolName" "${RadosOpts[@]}" get "$acObject" - 2> /dev/null)
else
  acTime=
fi

tmpFile=$(mktemp)
echo "$SuspectTime:$latestUpdateTime" > "$tmpFile"
rados -p "$PoolName" "${RadosOpts[@]}" put "$acObject" "$tmpFile" &> /dev/null
rm -f "$tmpFile"

if [ -z "$acTime" ]; then
    if [[ $latestUpdateTime -gt $SuspectTime ]]; then
        echo "=====> ALIVE <====="
    else
        echo "=====> Considering host as DEAD due to RADOS object [$acObject] did not exist and condition [latestUpdateTime -gt SuspectTime] has not been satisfied. <======"
    fi
else
    arrTime=(${acTime//:/ })
    lastSuspectTime=${arrTime[0]}
    lastUpdateTime=${arrTime[1]}

    suspectTimeDiff=$(expr $SuspectTime - $lastSuspectTime)
    if [[ $suspectTimeDiff -lt 0 ]]; then
        if [[ $latestUpdateTime -gt $SuspectTime ]]; then
            echo "=====> ALIVE <====="
        else
            echo "=====> Considering host as DEAD due to RADOS object [$acObject] exists, condition [suspectTimeDiff -lt 0] was satisfied and [latestUpdateTime -gt SuspectTime] has not been satisfied. <======"
        fi
    else
        if [[ $latestUpdateTime -gt $lastUpdateTime ]]; then
            echo "=====> ALIVE <====="
        else
            echo "=====> Considering host as DEAD due to RADOS object [$acObject] exists and conditions [suspectTimeDiff -lt 0] and [latestUpdateTime -gt SuspectTime] have not been satisfied. <======"
        fi
    fi
fi

exit 0
