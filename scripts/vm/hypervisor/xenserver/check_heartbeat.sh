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
  printf "Usage: %s [uuid of this host] [interval in seconds]\n" $(basename $0) >&2

}

if [ -z $1 ]; then
  usage
  exit 2
fi

if [ -z $2 ]; then
  usage
  exit 3
fi


date=`date +%s`
hbs=`lvscan | grep hb-$1 | awk '{print $2}'`
for hb in $hbs
do
  hb=${hb:1:`expr ${#hb} - 2`}
  active=`lvscan | grep $hb | awk '{print $1}'`
  if [ "$active" == "inactive" ]; then
    lvchange -ay $hb
    if [ ! -L $hb ]; then
      continue;
    fi
  fi
  ping=`dd if=$hb bs=1 count=100`
  if [ $? -ne 0 ]; then
    continue;
  fi
  diff=`expr $date - $ping`
  if [ $diff -lt $2 ]; then
    echo "=====> ALIVE <====="
    exit 0;    
  fi
done

hbs=`ls -l /var/run/sr-mount/*/hb-$1 | awk '{print $9}'`
for hb in $hbs
do
  ping=`cat $hb`
  if [ $? -ne 0 ]; then
    continue;
  fi
  diff=`expr $date - $ping`
  if [ $diff -lt $2 ]; then
    echo "=====> ALIVE <====="
    exit 0;    
  fi
done

echo "=====> DEAD <======"
exit 1
