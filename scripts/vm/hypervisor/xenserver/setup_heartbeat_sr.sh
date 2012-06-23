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
  echo "Usage: $(basename $0) [uuid of this host] [uuid of the sr to place the heartbeat]"

}

if [ -z $1 ]; then
  usage
  echo "#1#no host uuid"
  exit 0
fi

if [ -z $2 ]; then
  usage
  echo "#2#no sr uuid"
  exit 0
fi

if [ `xe host-list | grep $1 | wc -l` -ne 1 ]; then
  echo  "#3# Unable to find the host uuid: $1"
  exit 0
fi

if [ `xe sr-list uuid=$2 | wc -l`  -eq 0 ]; then 
  echo "#4# Unable to find SR with uuid: $2"
  exit 0
fi

if [ `xe pbd-list sr-uuid=$2 | grep -B 1 $1 | wc -l` -eq 0 ]; then
  echo "#5# Unable to find a pbd for the SR: $2"
  exit 0
fi

srtype=`xe sr-param-get param-name=type uuid=$2`

if [ "$srtype" == "nfs" ];then
  dir=/var/run/sr-mount/$2
  filename=$dir/hb-$1
  files=`ls $dir | grep "hb-$1"`
  if [ -z "$files" ]; then
    date=`date +%s`
    echo "$date" > $filename
  fi
else 
  dir=/dev/VG_XenStorage-$2
  link=$dir/hb-$1
  lv=`lvscan | grep $link`
  if [ -z "$lv" ]; then
    if [ -e $link ]; then
      devmapper=$(ls $link -l | awk '{print $NF}')
      if [ -e $devmapper ]; then
        dmsetup remove -f $devmapper
      fi
      rm $link -f
    fi 
    lvcreate VG_XenStorage-$2 -n hb-$1 --size 4M
    if [ $? -ne 0 ]; then
      echo "#6# Unable to create heartbeat volume hb-$1"
      exit 0
    fi
    lv=`lvscan | grep $link`
    if [ -z "$lv" ]; then
      echo "#7# volume hb-$1 is not created"
      exit 0
    fi
  fi

  if [ `echo $lv | awk '{print $1}'` == "inactive" ]; then
    lvchange -ay $link
    if [ $? -ne 0 ]; then
      echo "#8# Unable to make $link active"
      exit 0
    fi
  fi

  if [ ! -L $link ]; then
    echo "#9# Unable to find the soft link $link"
    exit 0
  fi
  dd if=/dev/zero of=$link bs=1 count=100
fi

echo "#0#DONE"

exit 0
