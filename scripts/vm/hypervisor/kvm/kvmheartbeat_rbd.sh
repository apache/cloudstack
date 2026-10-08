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

# Ceph RBD flavor of kvmheartbeat.sh/kvmsmpheartbeat.sh.
#
# There is no shared POSIX mount point to write a heartbeat file to when the
# primary storage pool is Ceph RBD, so the heartbeat timestamp is instead
# stored as a small RADOS object (one object per host) in the same RBD pool.
# Any host with a working path to the Ceph cluster can write/read this object,
# which gives the same semantics as the NFS/SharedMountPoint heartbeat file.

help() {
  printf "Usage: $0
                    -s ceph monitor host(s), comma separated
                    -o ceph/rbd pool name
                    -n cephx auth user (optional)
                    -k cephx auth key, base64 (optional, required if -n is set)
                    -h host
                    -r write/read hb log
                    -c cleanup
                    -t interval between read hb log\n"
  exit 1
}
#set -x
MonHosts=
PoolName=
CephUser=
CephKey=
HostIP=
interval=
rflag=0
cflag=0

while getopts 's:o:n:k:h:t:rc' OPTION
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
  r)
     rflag=1
     ;;
  t)
     interval="$OPTARG"
     ;;
  c)
    cflag=1
     ;;
  *)
     help
     ;;
  esac
done

if [ -z "$MonHosts" ] || [ -z "$PoolName" ]
then
   exit 1
fi

# the host IP names the heartbeat object, so it is required except for the self-fencing (-c)
if [ "$cflag" != "1" ] && [ -z "$HostIP" ]
then
   exit 1
fi

if [ -n "$CephUser" ] && [ -z "$CephKey" ]
then
   exit 1
fi

RadosOpts=(--mon-host "$MonHosts")
if [ -n "$CephUser" ]
then
   RadosOpts+=(--id "$CephUser" --key "$CephKey")
fi

hbObject="KVMHA-hb-$HostIP"

write_hbLog() {
  tmpFile=$(mktemp)
  date +%s > "$tmpFile"
  rados -p "$PoolName" "${RadosOpts[@]}" put "$hbObject" "$tmpFile" &> /dev/null
  rc=$?
  rm -f "$tmpFile"
  return $rc
}

check_hbLog() {
  now=$(date +%s)
  hb=$(rados -p "$PoolName" "${RadosOpts[@]}" get "$hbObject" - 2> /dev/null)
  if ! [[ "$hb" =~ ^[0-9]+$ ]]
  then
    # Either the RADOS object doesn't exist yet (host never wrote a heartbeat)
    # or the Ceph cluster can't be reached right now. Either way we can't
    # confirm the host is alive, so fail safe and report it as DEAD.
    hbAge=
    return 1
  fi
  # the age is kept in a variable, not in the return status, as a status above 255 wraps around
  hbAge=$(expr $now - $hb)
  if [ $hbAge -gt $interval ]
  then
    return 1
  fi
  return 0
}

if [ "$rflag" == "1" ]
then
  if check_hbLog
  then
    echo "=====> ALIVE <====="
  elif [ -z "$hbAge" ]
  then
    echo "=====> Considering host as DEAD because RADOS object [$hbObject] in pool [$PoolName] could not be read <======"
  else
    echo "=====> Considering host as DEAD because last write to RADOS object [$hbObject] in pool [$PoolName] was [$hbAge] seconds ago, but the max interval is [$interval] <======"
  fi
  exit 0
elif [ "$cflag" == "1" ]
then
  /usr/bin/logger -t heartbeat "kvmheartbeat_rbd.sh will reboot system because it was unable to write the heartbeat to the Ceph RBD storage."
  sync &
  sleep 5
  echo b > /proc/sysrq-trigger
  exit $?
else
  write_hbLog
  exit $?
fi
