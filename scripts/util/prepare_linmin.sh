#!/bin/sh
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


DHCP_CONF="/etc/dhcpd.conf"

err_exit() {
	echo "$*"
	exit 1
}

[ ! -f $DHCP_CONF ] && err_exit "Cannot find $DHCP_CONF"

cat $DHCP_CONF | tr '\n' '~' > /tmp/dhcpd.tmp && sed -i 's/}/}\n/g' /tmp/dhcpd.tmp && sed -i 's/\(subnet.*netmask.*{\).*\(}\)/\1\2/g' /tmp/dhcpd.tmp && cat /tmp/dhcpd.tmp | tr '~' '\n' > $DHCP_CONF && rm /tmp/dhcpd.tmp -f
[ $? -ne 0 ] && err_exit "Configure dhcpd.conf failed"
service dhcpd restart
[ $? -ne 0 ] && err_exit "restart dhcpd failed"
exit 0
