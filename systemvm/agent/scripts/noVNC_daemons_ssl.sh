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

isNumpyInstalled=$(dpkg -l |grep -w python-numpy)
if [ "$isNumpyInstalled" == "" ]; then 
    apt-get update
    apt-get install -y python-numpy --force-yes
fi

python /root/noVNC/utils/websockify/websockify/websocketproxy.py --ssl-only --web=/root/noVNC --daemon --cert=/usr/local/cloud/systemvm/certs/customssl.crt --key=/usr/local/cloud/systemvm/certs/customssl.key 8080

DATA="echo \'<cross-domain-policy><allow-access-from domain=\\\"*\\\" to-ports=\\\"*\\\" /></cross-domain-policy>\'"
/usr/bin/nohup /usr/bin/socat -T 1 TCP-L:843,reuseaddr,fork,crlf SYSTEM:"$DATA" 2> /var/log/socat.err &
