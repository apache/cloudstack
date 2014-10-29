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



 

#run.sh runs the console proxy.

# make sure we delete the old files from the original template 
rm console-proxy.jar
rm console-common.jar
rm conf/cloud.properties

set -x

CP=./:./conf
for file in *.jar
do
  CP=${CP}:$file
done
keyvalues=
LOGHOME=/var/log/cloud/

CMDLINE=$(cat /var/cache/cloud/cmdline)

#CMDLINE="graphical utf8 eth0ip=0.0.0.0 eth0mask=255.255.255.0 eth1ip=192.168.140.40 eth1mask=255.255.255.0 eth2ip=172.24.0.50 eth2mask=255.255.0.0 gateway=172.24.0.1 dns1=72.52.126.11 template=domP dns2=72.52.126.12 host=192.168.1.142 port=8250 mgmtcidr=192.168.1.0/24 localgw=192.168.140.1 zone=5 pod=5"
for i in $CMDLINE
  do
     KEY=$(echo $i | cut -s -d= -f1)
     VALUE=$(echo $i | cut -s -d= -f2)
     [ "$KEY" == "" ] && continue
     case $KEY in
        *)
          keyvalues="${keyvalues} $KEY=$VALUE"
     esac
  done
   
tot_mem_k=$(cat /proc/meminfo | grep MemTotal | awk '{print $2}')
let "tot_mem_m=tot_mem_k>>10"
let "eightypcnt=$tot_mem_m*8/10"
let "maxmem=$tot_mem_m-80"

if [ $maxmem -gt $eightypcnt ]
then
  maxmem=$eightypcnt
fi

if [ "$(uname -m | grep '64')" == "" ]; then
  let "maxmem32bit=2600"
  if [ $maxmem -gt $maxmem32bit ]; then
    maxmem=$maxmem32bit
  fi
fi

java -Djavax.net.ssl.trustStore=./certs/realhostip.keystore -Djsse.enableSNIExtension=false -Dlog.home=$LOGHOME -mx${maxmem}m -cp $CP com.cloud.agent.AgentShell $keyvalues $@
