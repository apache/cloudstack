#!/bin/sh
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
