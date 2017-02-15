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

set -x

usage() { echo "Usage: $0  -d <all|agent> -t <duration in seconds for downing all network interfaces>"; exit 1; }

Interval=
Down=
while getopts 'd:t:' OPTION
do
  case $OPTION in
  d)
     Down="$OPTARG"
     ;;
  t)
     Interval="$OPTARG"
     ;;
  *)
     usage
     ;;
  esac
done


if [ -z $Interval ]; then
   usage
fi


if [ "$Down" != 'all' ]; then
 if [ "$Down" != 'agent' ]; then
   usage
 fi
fi

case $Interval in
    ''|*[!0-9]*) echo "The parameter should be an integer"; exit ;;
    *) echo $1 ;;
esac

if [ $Interval -lt 1 ]; then
   echo "Down time should be at least 1 second"
   exit 1
elif [ $Interval -gt 5000 ]; then
   echo "Down time should be less than 5000 second"
   exit 1
fi


for i in `ifconfig -a | sed 's/[ \t].*//;/^\(lo\|\)$/d' | grep "^eth.$"`
do
    ifconfig $i down
done


service cloudstack-agent stop
update-rc.d -f cloudstack-agent remove

sleep 1

if [ "$Down" = 'agent' ]; then
    for i in `ifconfig -a | sed 's/[ \t].*//;/^\(lo\|\)$/d' | grep "^eth.$"`
    do
       ifconfig $i up
    done
fi

counter=$Interval
while [ $counter -gt 0 ]
do
   sleep 1
   counter=$(( $counter - 1 ))
done

if [ "$Down" = 'all' ]; then
    for i in `ifconfig -a | sed 's/[ \t].*//;/^\(lo\|\)$/d' | grep eth`
    do
       ifconfig $i up
    done
fi


update-rc.d -f cloudstack-agent defaults
service cloudstack-agent start
