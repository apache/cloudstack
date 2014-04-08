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

source /root/func.sh

lock="biglock"
locked=$(getLockFile $lock)
if [ "$locked" != "1" ]
then
    exit 1
fi

#set -x

filename=/var/log/routerServiceMonitor.log #Monitor service log file
if [ -n "$1" -a -n "$2" ]
then
        reqdateval=$(date -d $1 +"%Y%m%d");
        reqtimeval=$(date -d $2 +"%H%M%S");
else
        reqdateval=0
        reqtimeval=0
fi
if [ -f $filename ]
then
        while read line
        do
        if [ -n "$line" ]; then
            dateval=`echo $line |awk '{print $1}'`
            timeval=`echo $line |awk '{print $2}'`

            todate=$(date -d "$dateval" +"%Y%m%d") > /dev/null
            totime=$(date -d "$timeval" +"%H%M%S") > /dev/null
            if [ "$todate" -gt "$reqdateval" ] > /dev/null
            then
                if [ -n "$alerts" ]; then  alerts="$alerts\n$line"; else alerts="$line"; fi #>> $outputfile
                elif [ "$todate" -eq "$reqdateval" ] > /dev/null
                then
                    if [ "$totime" -gt "$reqtimeval" ] > /dev/null
                    then
                        if [ -n "$alerts" ]; then  alerts="$alerts\n$line"; else alerts="$line"; fi #>> $outputfile
                    fi
                fi
            fi
        done < $filename
fi
if [ -n "$alerts" ]; then
       echo $alerts
else
       echo "No Alerts"
fi

unlock_exit 0 $lock $locked