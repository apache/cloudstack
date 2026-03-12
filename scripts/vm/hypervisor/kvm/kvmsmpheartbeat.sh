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
                -i identifier (required for CLI compatibility; value ignored by local-only heartbeat)
                -p path (required for CLI compatibility; value ignored by local-only heartbeat)
                -m mount point (local path where heartbeat will be written)
                -h host (host IP/name to include in heartbeat filename)
                -r write/read hb log (read-check mode)
                -c cleanup (trigger emergency reboot)
                -t interval between read hb log\n"
  exit 1
}

#set -x
NfsSvrIP=
NfsSvrPath=
MountPoint=
HostIP=
interval=
rflag=0
cflag=0

while getopts 'i:p:m:h:t:rc' OPTION
do
  case $OPTION in
  i)
     NfsSvrIP="$OPTARG"
     ;; # retained for CLI compatibility but unused for local-only script
  p)
     NfsSvrPath="$OPTARG"
     ;; # retained for CLI compatibility but unused for local-only script
  m)
     MountPoint="$OPTARG"
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

# For local-only heartbeat we require a mountpoint
if [ -z "$MountPoint" ]
then
   echo "Mount point (-m) is required"
   help
fi

# Ensure mount point exists and is writable
if [ ! -d "$MountPoint" ]; then
  mkdir -p "$MountPoint" 2>/dev/null
  if [ $? -ne 0 ]; then
    echo "Failed to create mount point directory: $MountPoint" >&2
    exit 1
  fi
fi

# Determine a sensible HostIP if not provided
if [ -z "$HostIP" ]; then
  # try to get a non-loopback IPv4 address, fallback to hostname
  ipaddr=$(hostname -I 2>/dev/null | awk '{print $1}')
  if [ -n "$ipaddr" ]; then
    HostIP="$ipaddr"
  else
    HostIP=$(hostname)
  fi
fi

#delete VMs on this mountpoint (best-effort)
deleteVMs() {
  local mountPoint=$1
  vmPids=$(ps aux | grep qemu | grep "$mountPoint" | awk '{print $2}' 2> /dev/null)

  if [ -z "$vmPids" ]
  then
     return
  fi

  for pid in $vmPids
  do
     kill -9 $pid &> /dev/null
  done
}

#checking is there the mount point present under $MountPoint?
if grep -q "^[^ ]\+ $MountPoint " /proc/mounts
then
   # mount exists; if not in read-check mode, consider deleting VMs similar to original behavior
   if [ "$rflag" == "0" ]
   then
     deleteVMs $MountPoint
   fi
else
   # mount point not present — we don't remount in local-only script
   # nothing to do here; keep for compatibility with original flow
   :
fi

hbFolder="$MountPoint/KVMHA/"
hbFile="$hbFolder/hb-$HostIP"

write_hbLog() {
#write the heart beat log
  stat "$hbFile" &> /dev/null
  if [ $? -gt 0 ]
  then
     # create a new one
     mkdir -p "$hbFolder" &> /dev/null
     # touch will be done by atomic write below; ensure folder is writable
     if [ ! -w "$hbFolder" ]; then
       printf "Folder not writable: $hbFolder" >&2
       return 2
     fi
  fi

  timestamp=$(date +%s)
  # Write atomically to avoid partial writes (write to tmp then mv)
  tmpfile="${hbFile}.$$"
  printf "%s\n" "$timestamp" > "$tmpfile" 2>/dev/null
  if [ $? -ne 0 ]; then
    printf "Failed to write heartbeat to $tmpfile" >&2
    return 2
  fi
  mv -f "$tmpfile" "$hbFile" 2>/dev/null
  return $?
}

check_hbLog() {
  hb_diff=0
  if [ ! -f "$hbFile" ]; then
    # signal large difference if file missing
    hb_diff=999999
    return 1
  fi
  now=$(date +%s)
  hb=$(cat "$hbFile" 2>/dev/null)
  if [ -z "$hb" ]; then
    hb_diff=999998
    return 1
  fi
  diff=`expr $now - $hb 2>/dev/null`
  if [ $? -ne 0 ]
  then
    hb_diff=999997
    return 1
  fi
  if [ -z "$interval" ]; then
    # if no interval provided, consider 0 as success
    if [ $diff -gt 0 ]; then
      hb_diff=$diff
      return 1
    else
      hb_diff=0
      return 0
    fi
  fi
  if [ $diff -gt $interval ]
  then
    hb_diff=$diff
    return 1
  fi
  hb_diff=0
  return 0
}

if [ "$rflag" == "1" ]
then
  check_hbLog
  status=$?
  diff="${hb_diff:-0}"
  if [ $status -eq 0 ]
  then
    echo "=====> ALIVE <====="
  else
    echo "=====> Considering host as DEAD because last write on [$hbFile] was [$diff] seconds ago, but the max interval is [$interval] <======"
  fi
  exit 0
elif [ "$cflag" == "1" ]
then
  /usr/bin/logger -t heartbeat "kvmsmpheartbeat.sh will reboot system because it was unable to write the heartbeat to the storage."
  sync &
  sleep 5
  echo b > /proc/sysrq-trigger
  exit $?
else
  write_hbLog
  exit $?
fi
