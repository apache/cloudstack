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



 

#_run.sh runs the agent client.

# set -x
readonly PROGNAME=$(basename "$0")
readonly LOCKDIR=/tmp
readonly LOCKFD=500

CLOUDSTACK_HOME="/usr/local/cloud"
. $CLOUDSTACK_HOME/systemvm/utils.sh

LOCKFILE=$LOCKDIR/$PROGNAME.xlock
lock $LOCKFILE $LOCKFD
if [ $? -eq 1 ];then
  exit 1
fi

while true
do
  pid=$(get_pids)
  action=`cat /usr/local/cloud/systemvm/user_request`
  if [ "$pid" == "" ] && [ "$action" == "start" ] ; then
    ./_run.sh "$@" &
    wait
    ex=$?
    if [ $ex -eq 0 ] || [ $ex -eq 1 ] || [ $ex -eq 66 ] || [ $ex -gt 128 ]; then
        # permanent errors
        sleep 5
    fi
  fi

  # user stop agent by service cloud stop
  grep 'stop' /usr/local/cloud/systemvm/user_request &>/dev/null
  if [ $? -eq 0 ]; then
      timestamp=$(date)
      echo "$timestamp User stops cloud.com service" >> /var/log/cloud.log
      exit 0
  fi
  sleep 5
done
