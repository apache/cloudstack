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
  printf "Usage: %s [uuid of this host] [interval in seconds]\n" $(basename $0)

}

if [ -z $1 ]; then
  usage
  exit 2
fi

if [ -z $2 ]; then
  usage
  exit 3
fi

if [ -z $3 ]; then
  usage
  exit 3
fi

if [ ! -f /opt/cloud/bin/xenheartbeat.sh ]; then
  printf "Error: Unable to find xenheartbeat.sh to launch\n"
  exit 4
fi

for psid in `ps -ef | grep xenheartbeat | grep -v grep | awk '{print $2}'`; do
  kill $psid
done

nohup /opt/cloud/bin/xenheartbeat.sh $1 $2 $3 >/dev/null 2>/dev/null &
echo "======> DONE <======"
