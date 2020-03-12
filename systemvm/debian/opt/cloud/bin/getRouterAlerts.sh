#!/usr/bin/env bash
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

# getRouterAlerts.sh  --- Send the alerts from routerServiceMonitor.log to Management Server

#set -x

filename=/var/log/routerServiceMonitor.log #Monitor service log file
if [ -n "$1" -a -n "$2" ]
then
        reqDateVal=$(date -d "$1 $2" "+%s");
else
        reqDateVal=0
fi
if [ -f $filename ]
then
        while read line
        do
            if [ -n "$line" ]
            then
                dateval=`echo $line |awk '{print $1, $2}'`
                IFS=',' read -a array <<< "$dateval"
                dateval=${array[0]}

                toDateVal=$(date -d "$dateval" "+%s")

                if [ "$toDateVal" -gt "$reqDateVal" ]
                then
                    alerts="$line\n$alerts"
                else
                    break
                fi
            fi
        done < <(tac $filename)
fi
if [ -n "$alerts" ]; then
       echo $alerts
else
       echo "No Alerts"
fi