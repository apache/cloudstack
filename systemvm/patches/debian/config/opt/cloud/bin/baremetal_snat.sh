#!/bin/bash

#Licensed to the Apache Software Foundation (ASF) under one
#or more contributor license agreements.  See the NOTICE file
#distributed with this work for additional information
#regarding copyright ownership.  The ASF licenses this file
#to you under the Apache License, Version 2.0 (the
#"License"); you may not use this file except in compliance
#with the License.  You may obtain a copy of the License at
#
#  http://www.apache.org/licenses/LICENSE-2.0
#
#Unless required by applicable law or agreed to in writing,
#software distributed under the License is distributed on an
#"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
#KIND, either express or implied.  See the License for the
#specific language governing permissions and limitations
#under the License.

set +u

mgmt_nic_ip=$1
internal_server_ip=$2
gateway_ip=$3

ip route | grep "$internal_server_ip" > /dev/null

if [ $? -ne 0 ]; then
    ip route add $internal_server_ip via $gateway_ip
fi

iptables-save | grep -- "-A POSTROUTING -d $internal_server_ip" > /dev/null

if [ $? -ne 0 ]; then
    iptables -t nat -A POSTROUTING -d $internal_server_ip -j SNAT --to-source $mgmt_nic_ip
fi


iptables-save | grep -- "-A INPUT -i eth0 -p udp -m udp --dport 69 -j ACCEPT" > /dev/null
if [ $? -ne 0 ]; then
    iptables -I INPUT -i eth0 -p udp -m udp --dport 69 -j ACCEPT
fi

iptables-save | grep -- "-A FORWARD -i eth1 -o eth0 -j ACCEPT" > /dev/null
if [ $? -ne 0 ]; then
    iptables -A FORWARD -i eth1 -o eth0 -j ACCEPT
fi

rule="-A FORWARD -d $internal_server_ip/32 -i eth0 -o eth1 -j ACCEPT"
iptables-save | grep -- "$rule" > /dev/null
if [ $? -ne 0 ]; then 
    iptables -I FORWARD -d $internal_server_ip/32 -i eth0 -o eth1 -j ACCEPT
fi

