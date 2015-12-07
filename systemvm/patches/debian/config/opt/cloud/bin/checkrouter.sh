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

STATUS=UNKNOWN
INTERFACE=eth1
ROUTER_TYPE=$(cat /etc/cloudstack/cmdline.json | grep type | awk '{print $2;}' | sed -e 's/[,\"]//g')
if [ $ROUTER_TYPE = "router" ]
then
	ROUTER_STATE=$(ip addr | grep eth0 | grep inet | wc -l | xargs bash -c  'if [ $0 == 2 ]; then echo "MASTER"; else echo "BACKUP"; fi')
	STATUS=$ROUTER_STATE
else
	ROUTER_STATE=$(ip addr | grep $INTERFACE | grep state | awk '{print $9;}')
	if [ $ROUTER_STATE = "UP" ]
	then
	    STATUS=MASTER
	elif [ $ROUTER_STATE = "DOWN" ]
	then
	    STATUS=BACKUP
	fi
fi

echo "Status: ${STATUS}"