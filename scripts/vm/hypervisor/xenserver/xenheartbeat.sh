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

#set -x

usage() {
  printf "Usage: %s [uuid of this host] [timeout in seconds] [interval in seconds]\n" $(basename $0) >&2

}

if [ -z $1 ]; then
  usage
  exit 2
else
  host=$1
fi

if [ -z $2 ]; then
  usage
  exit 3
else
  timeout=$2
fi

if [ ! -z $3 ]; then
  interval=$3
else
  interval=5
fi

if [ $interval -gt $timeout ]; then
  usage
  exit 3
fi

file=/opt/cloud/bin/heartbeat
lastdate=$(($(date +%s) + $interval))

while [ $(date +%s) -lt $(($lastdate + $timeout)) ]
do
  sleep $interval

  if [ ! -f $file  ]
  then
    continue
  fi

  # test heartbeat file
  dirs=$(cat $file | grep "sr-mount\|VG_XenStorage")
  if [ ! -n "$dirs" ];then
    /usr/bin/logger -t heartbeat "Problem with heartbeat, no iSCSI or NFS mount defined in $file!"
    lastdate=$(date +%s)
    continue
  fi

  # for iscsi
  dirs=$(cat $file | grep VG_XenStorage)
  for dir in $dirs
  do
    if [ -d $dir ]; then
      hb=$dir/hb-$host
      date +%s | dd of=$hb count=100 bs=1 2>/dev/null
      if [ $? -ne 0 ]; then
        /usr/bin/logger -t heartbeat "Potential problem with $hb: not reachable since $(($(date +%s) - $lastdate)) seconds"
      else
        lastdate=$(date +%s)
      fi
    else
      /usr/bin/logger -t heartbeat "Potential problem with heartbeat, dir not found for $dir"
      lastdate=$(date +%s)
      sed -i /${dir##/*/}/d $file
    fi
  done

  # for nfs
  dirs=$(cat $file | grep sr-mount)
  for dir in $dirs
  do
    mp=`mount | grep $dir`
    if [ -n "$mp" ]; then
      hb=$dir/hb-$host
      date +%s | dd of=$hb count=100 bs=1 2>/dev/null
      if [ $? -ne 0 ]; then
        /usr/bin/logger -t heartbeat "Potential problem with $hb: not reachable since $(($(date +%s) - $lastdate)) seconds"
      else
        lastdate=$(date +%s)
      fi
    else
      /usr/bin/logger -t heartbeat "Potential problem with heartbeat, mount not found for $dir"
      lastdate=$(date +%s)
      sed -i /${dir##/*/}/d $file
    fi
  done
done

/usr/bin/logger -t heartbeat "Problem with $hb: not reachable for $(($(date +%s) - $lastdate)) seconds, rebooting system!"
echo b > /proc/sysrq-trigger