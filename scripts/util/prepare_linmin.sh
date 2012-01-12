#!/bin/sh
# Copyright (C) 2011 Citrix Systems, Inc.  All rights reserved
#     
# This software is licensed under the GNU General Public License v3 or later.
# 
# It is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation, either version 3 of the License, or any later version.
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
# 
# You should have received a copy of the GNU General Public License
# along with this program.  If not, see <http://www.gnu.org/licenses/>.
# 



 

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
