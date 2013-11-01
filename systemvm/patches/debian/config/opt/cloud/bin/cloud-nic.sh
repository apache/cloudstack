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

plug_nic() {
  sudo echo "$tableNo $tableName" >> /etc/iproute2/rt_tables 2>/dev/null
  sudo ip rule add fwmark $tableNo table $tableName 2>/dev/null
  sudo ip route flush table $tableName
  sudo ip route flush cache
}


unplug_nic() {
  sudo iptables -t mangle -D PREROUTING -i $dev -m state --state NEW -j CONNMARK --set-mark $tableNo 2>/dev/null

  sudo ip rule del fwmark $tableNo 2>/dev/null
  sudo ip route flush table $tableName
  sudo sed -i /"$tableNo $tableName"/d /etc/iproute2/rt_tables 2>/dev/null
  sudo ip route flush cache
  # remove network usage rules
  sudo iptables -F NETWORK_STATS_$dev 2>/dev/null
  iptables-save | grep NETWORK_STATS_$dev | grep "\-A"  | while read rule
  do
    rule=$(echo $rule | sed 's/\-A/\-D/')
    sudo iptables $rule
  done
  sudo iptables -X NETWORK_STATS_$dev 2>/dev/null
  # remove vpn network usage rules
  sudo iptables -t mangle -F VPN_STATS_$dev 2>/dev/null
  iptables-save -t mangle | grep VPN_STATS_$dev | grep "\-A"  | while read rule
  do
    rule=$(echo $rule | sed 's/\-A/\-D/')
    sudo iptables -t mangle $rule
  done
  sudo iptables -t mangle -X VPN_STATS_$dev 2>/dev/null  
  # remove rules on this dev
  iptables-save -t mangle | grep $dev | grep "\-A"  | while read rule
  do
    rule=$(echo $rule | sed 's/\-A/\-D/')
    sudo iptables -t mangle $rule
  done
  iptables-save -t nat | grep $dev | grep "\-A"  | while read rule
  do
    rule=$(echo $rule | sed 's/\-A/\-D/')
    sudo iptables -t nat $rule
  done
  iptables-save | grep $dev | grep "\-A"  | while read rule
  do
    rule=$(echo $rule | sed 's/\-A/\-D/')
    sudo iptables $rule
  done
  # remove apache config for this eth
  rm -f /etc/apache2/conf.d/vhost$dev.conf
}

action=$1
dev=$2
tableNo=${dev:3}
tableName="Table_$dev"

if [ $action == 'add' ]
then
  plug_nic
else
  unplug_nic
fi
