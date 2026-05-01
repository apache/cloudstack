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
                    -r write/read hb log
                    -c cleanup
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

if [ -z "$NfsSvrIP" ]
then
   exit 1
fi


#delete VMs on this mountpoint
deleteVMs() {
  local mountPoint=$1
  vmPids=$(ps aux| grep qemu | grep "$mountPoint" | awk '{print $2}' 2> /dev/null)
  if [ $? -gt 0 ]
  then
     return
  fi

  if [ -z "$vmPids" ]
  then
     return
  fi

  for pid in $vmPids
  do
     kill -9 $pid &> /dev/null
  done
}

#checking is there the same nfs server mounted under $MountPoint?
mounts=$(cat /proc/mounts |grep nfs|grep $MountPoint)
if [ $? -gt 0 ]
then
   # remount it
   mount $NfsSvrIP:$NfsSvrPath $MountPoint -o sync,soft,proto=tcp,acregmin=0,acregmax=0,acdirmin=0,acdirmax=0,noac &> /dev/null
   if [ $? -gt 0 ]
   then
      printf "Failed to remount $NfsSvrIP:$NfsSvrPath under $MountPoint"
      exit 1
   fi
   if [ "$rflag" == "0" ]
   then
     deleteVMs $MountPoint
   fi
fi

hbFolder=$MountPoint/KVMHA/
hbFile=$hbFolder/hb-$HostIP

write_hbLog() {
#write the heart beat log
  stat $hbFile &> /dev/null
  if [ $? -gt 0 ]
  then
     # create a new one
     mkdir -p $hbFolder &> /dev/null
     touch $hbFile &> /dev/null
     if [ $? -gt 0 ]
     then
 	printf "Failed to create $hbFile"
        return 2
     fi
  fi

  timestamp=$(date +%s)
  echo $timestamp > $hbFile
  return $?
}

check_hbLog() {
  now=$(date +%s)
  hb=$(cat $hbFile)
  diff=`expr $now - $hb`
  if [ $diff -gt $interval ]
  then
    return $diff
  fi
  return 0
}

if [ "$rflag" == "1" ]
then
  check_hbLog
  diff=$?
  if [ $diff == 0 ]
  then
    echo "=====> ALIVE <====="
  else
    echo "=====> Considering host as DEAD because last write on [$hbFile] was [$diff] seconds ago, but the max interval is [$interval] <======"
  fi
  exit 0
elif [ "$cflag" == "1" ]
then
  # Read fence action from agent.properties (default: reboot for backward compatibility).
  # Allowed values: reboot | graceful-reboot | restart-agent | log-only
  AGENT_PROPS="/etc/cloudstack/agent/agent.properties"
  FENCE_ACTION="reboot"
  if [ -r "$AGENT_PROPS" ]; then
    val=$(grep -E '^[[:space:]]*kvm\.heartbeat\.fence\.action[[:space:]]*=' "$AGENT_PROPS" | tail -n 1 | cut -d= -f2- | tr -d '[:space:]')
    [ -n "$val" ] && FENCE_ACTION="$val"
  fi

  case "$FENCE_ACTION" in
    log-only)
      /usr/bin/logger -t heartbeat "kvmheartbeat.sh: heartbeat write to storage failed; fence action 'log-only' selected — taking no automatic action. Operator must investigate."
      exit 0
      ;;
    restart-agent)
      /usr/bin/logger -t heartbeat "kvmheartbeat.sh: heartbeat write to storage failed; fence action 'restart-agent' — restarting cloudstack-agent (running VMs preserved)."
      sync &
      sleep 2
      systemctl restart cloudstack-agent
      exit $?
      ;;
    graceful-reboot)
      /usr/bin/logger -t heartbeat "kvmheartbeat.sh: heartbeat write to storage failed; fence action 'graceful-reboot' — rebooting via systemctl (allows running VMs to stop cleanly)."
      sync &
      sleep 5
      systemctl reboot
      exit $?
      ;;
    reboot|*)
      # Original behavior: immediate kernel-level reboot via sysrq-trigger
      /usr/bin/logger -t heartbeat "kvmheartbeat.sh will reboot system because it was unable to write the heartbeat to the storage."
      sync &
      sleep 5
      echo b > /proc/sysrq-trigger
      exit $?
      ;;
  esac
else
  write_hbLog
  exit $?
fi
