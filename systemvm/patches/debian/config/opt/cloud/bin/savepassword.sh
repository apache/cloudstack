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

# Usage
#   save_password -v <user VM IP> -p <password>

while getopts 'v:p:' OPTION
do
  case $OPTION in
  v)    VM_IP="$OPTARG"
        ;;
  p)    PASSWORD="$OPTARG"
        ;;
  ?)    echo "Incorrect usage"
        ;;
  esac
done
SERVER_IP=$(/sbin/ifconfig eth0 | grep 'inet addr:' | cut -d: -f2 | awk '{ print $1}')
TOKEN_FILE="/tmp/passwdsrvrtoken"
TOKEN=""
if [ -f $TOKEN_FILE ]; then
    TOKEN=$(cat $TOKEN_FILE)
fi
ps aux | grep passwd_server_ip.py |grep -v grep 2>&1 > /dev/null
if [ $? -eq 0 ]
then
    curl --header "DomU_Request: save_password" "http://$SERVER_IP:8080/" -F "ip=$VM_IP" -F "password=$PASSWORD" -F "token=$TOKEN"
fi
