#!/bin/sh
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
