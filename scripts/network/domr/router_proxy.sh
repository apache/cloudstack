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

# used as a proxy to call script inside virtual router 

#set -x

check_gw() {
  ping -c 1 -n -q $1 > /dev/null
  if [ $? -gt 0 ]
  then
    sleep 1
    ping -c 1 -n -q $1 > /dev/null
  fi
  if [ $? -gt 0 ]
  then
    exit 1
  fi
}

cert="/root/.ssh/id_rsa.cloud"

script="$1"
domRIp="$2"

check_gw "$domRIp"

ssh -p 3922 -q -o StrictHostKeyChecking=no -i $cert root@$domRIp "/opt/cloud/bin/$script ${@:3}"

exit $?
