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

# @VERSION@

getEthByIp (){
  local ip=$1
  for dev in `ls -1 /sys/class/net | grep eth`
  do
    sudo ip addr show dev $dev | grep $ip\/ > /dev/null
    if [ $? -eq 0 ]
    then
      echo $dev
      return 0
    fi
  done
  return 1
}    

getVPCcidr () {
  CMDLINE=$(cat /var/cache/cloud/cmdline)
  for i in $CMDLINE
  do
    # search for foo=bar pattern and cut out foo
    KEY=$(echo $i | cut -d= -f1)
    VALUE=$(echo $i | cut -d= -f2)
    if [ "$KEY" == "vpccidr" ]
    then
      echo "$VALUE"
      return 0
    fi
  done
  return 1
}

removeRulesForIp() {
  local ip=$1
  iptables-save -t mangle | grep $ip | grep "\-A"  | while read rule
  do
    rule=$(echo $rule | sed 's/\-A/\-D/')
    sudo iptables -t mangle $rule
  done
  iptables-save -t nat | grep $ip | grep "\-A"  | while read rule
  do
    rule=$(echo $rule | sed 's/\-A/\-D/')
    sudo iptables -t nat $rule
  done
  iptables-save -t filter | grep $ip | grep "\-A"  | while read rule
  do
    rule=$(echo $rule | sed 's/\-A/\-D/')
    sudo iptables -t filter $rule
  done
}
