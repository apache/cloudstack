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

ip=$1
netmask=$2
mtu=$3
timeout=$4

i=0

get_interface() {
  for i in `seq 1 $(($timeout))`
  do
    #inf=$(ip route list ${1}/${2} | awk '{print $3}')
    inf=$(ip addr show|egrep '^ *inet'|grep ${1}/${2} |grep brd|awk -- '{ print $NF; }')
    if [ ! -z $inf ]; then
      echo $inf
      break
    fi
  sleep 0.1
  done
}


interfaceName=$(get_interface $ip $netmask)
echo $interfaceName
if [ ! -z $interfaceName ]; then
  state=$(cat /sys/class/net/${interfaceName}/operstate)
  if [[ "$state" == "up" ]]; then
	  ifconfig $interfaceName mtu $mtu up
  else
    ifconfig $interfaceName mtu $mtu
  fi
  if grep "dhcp-option=$interfaceName,26" /etc/dnsmasq.d/cloud.conf; then
    sed -i "/dhcp-option=$interfaceName,26/c\dhcp-option=$interfaceName,26,$mtu" /etc/dnsmasq.d/cloud.conf
  else
    echo "dhcp-option=$interfaceName,26,$mtu" >> /etc/dnsmasq.d/cloud.conf
  fi
  systemctl restart dnsmasq
  exit $?
fi

echo "Interface with IP ${ip} not found"
exit 1
