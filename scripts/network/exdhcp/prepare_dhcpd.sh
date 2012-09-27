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


# usage prepare_ping.sh subnet

dhcpd_conf=
subnet=$1

exit_with_error() {
	echo $1
	exit 1
}

exit_if_fail() {
	[ $? -ne 0 ] && exit_with_error "$*"
}

config_dhcpd() {
	echo "$*" >> $dhcpd_conf
	[ $? -ne 0 ] && exit_with_error "echo $* failed"
}

[ $# -ne 1 ] && exit_with_error "Usage:prepare_ping.sh subnet"

if [ -f "/etc/dhcp/dhcpd.conf" ]; then
	dhcpd_conf="/etc/dhcp/dhcpd.conf"
fi

if [ x"$dhcpd_conf" == "x" ] && [ -f "/etc/dhcpd.conf" ]; then
	dhcpd_conf="/etc/dhcpd.conf"
fi

if [ x"$dhcpd_conf" == "x" ]; then
	exit_with_error "Cannot find dhcpd.conf"
fi

signature=`head -n 1 $dhcpd_conf`
if [ x"$signature" != x"# CloudStack" ]; then
	# prepare dhcpd
	cp $dhcpd_conf /etc/dhcpd.conf.bak -f
	exit_if_fail "Cannot back dhcpd.conf"
	echo "# CloudStack" > $dhcpd_conf
	echo "# This is produced by CloudStack" >> $dhcpd_conf
	config_dhcpd ddns-update-style interim\;
	config_dhcpd subnet $subnet netmask 255.255.255.0 {}
	config_dhcpd allow booting\;
	config_dhcpd allow bootp\;
fi

service dhcpd restart
exit_if_fail "service dhcpd restart failed"
exit 0
