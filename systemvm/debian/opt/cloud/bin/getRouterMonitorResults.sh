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

# getRouterMonitorResults.sh  --- Send the monitor results to Management Server

if [ "$1" == "true" ]
then
    python /root/monitorServices.py > /dev/null
fi

printf "FAILING CHECKS:\n"

if [ -f /root/basic_failing_health_checks ]
then
    echo `cat /root/basic_failing_health_checks`
fi

if [ -f /root/advanced_failing_health_checks ]
then
    echo `cat /root/advanced_failing_health_checks`
fi

printf "MONITOR RESULTS:\n"

echo "{\"basic\":"
if [ -f /root/basic_monitor_results.json ]
then
    echo `cat /root/basic_monitor_results.json`
else
    echo "{}"
fi
echo ",\"advanced\":"
if [ -f /root/advanced_monitor_results.json ]
then
    echo `cat /root/advanced_monitor_results.json`
else
    echo "{}"
fi

echo "}"
