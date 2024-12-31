#!/bin/bash
# Usage ./reset-network.sh interface ip netmask
#
#  Licensed to the Apache Software Foundation (ASF) under one
#  or more contributor license agreements.  See the NOTICE file
#  distributed with this work for additional information
#  regarding copyright ownership.  The ASF licenses this file
#  to you under the Apache License, Version 2.0 (the
#  "License"); you may not use this file except in compliance
#  with the License.  You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing,
#  software distributed under the License is distributed on an
#  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
#  KIND, either express or implied.  See the License for the
#  specific language governing permissions and limitations
#  under the License.
#

[ $# -lt 3 ] && { echo -e "Missing arguments\nUsage: ./reset-network interface ip netmask label"; exit 1; }

. /etc/xensource-inventory

PIF=$(/usr/bin/xe pif-introduce device=$1 host-uuid=${INSTALLATION_UUID})

NETWORKPIF=$(/usr/bin/xe pif-list uuid=${PIF} params="network-uuid" --minimal)
/usr/bin/xe network-param-set uuid=${NETWORKPIF} name-label=${4}

if [ ${4} == "MGMT" ]
then
	/usr/bin/xe pif-reconfigure-ip uuid=${PIF} mode=static ip=${2} netmask=${3}
	/usr/bin/xe host-management-reconfigure pif-uuid=${PIF}
else
	/usr/bin/xe pif-plug uuid=${PIF}
fi
