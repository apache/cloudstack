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
                    -c cleanup"
  exit 1
}
#set -x
cflag=0

while getopts 'c' OPTION
do
  case $OPTION in
  c)
    cflag=1
     ;;
  *)
     help
     ;;
  esac
done


#delete VMs on this mountpoint
deleteVMs() {
  vmPids=$(ps aux| grep qemu | grep 'storpool-byid' | awk '{print $2}' 2> /dev/null)
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

if [ "$cflag" == "1" ]
then
  /usr/bin/logger -t heartbeat "kvmspheartbeat.sh will reboot system because it was unable to write the heartbeat to the storage."
  sync &
  sleep 5
  echo b > /proc/sysrq-trigger
  exit $?
fi
