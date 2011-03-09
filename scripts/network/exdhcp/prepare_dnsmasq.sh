#!/bin/sh

# prepare dnsmasq on external dhcp server
# Usage:
# 	sh prepare_dnsmasq gateway dns
#

gateway=$1
dns=$2

exit_with_error() {
	echo $1
	exit 1
}

[ $# -ne 2 ] && exit_with_error "Usage: prepare_dnsmasq gateway dns"

[ -f /etc/dnsmasq.conf ] || exit_with_error "Can not found /etc/dnsmasq.conf"
sed -i -e '/^[#]*dhcp-option=option:router.*$/d' /etc/dnsmasq.conf
[ $? -ne 0 ] && exit_with_error "sed -i -e '/^[#]*dhcp-option=option:router.*$/d' /etc/dnsmasq.conf failed"
echo dhcp-option=option:router,$gateway >> /etc/dnsmasq.conf
[ $? -ne 0 ] && exit_with_error "echo \"sed -i -e '/^[#]*dhcp-option=option:router.*$/d' /etc/dnsmasq.conf failed\" "


# Open DHCP ports in iptable
chkconfig --list iptables | grep "on"
if [ $? -ne 0 ]; then
	iptables-save | grep 'A INPUT -p udp -m udp --dport 67 -j ACCEPT' >/dev/null
	if [ $? -ne 0 ]; then
		iptables -I INPUT 1 -p udp --dport 67 -j ACCEPT
		if [ $? -ne 0 ]; then
			exit_with_error "iptables -I INPUT 1 -p udp --dport 67 -j ACCEPT failed"
		fi
		echo "iptables:Open udp port 67 for DHCP"
	fi

	iptables-save | grep 'A INPUT -p tcp -m tcp --dport 67 -j ACCEPT' >/dev/null
	if [ $? -ne 0 ]; then
		iptables -I INPUT 1 -p tcp --dport 67 -j ACCEPT
		if [ $? -ne 0 ]; then
			exit_with_error "iptables -I INPUT 1 -p tcp --dport 67 -j ACCEPT failed"
		fi
		echo "iptables:Open tcp port 67 for DHCP"
	fi

	iptables-save | grep 'A INPUT -p udp -m udp --dport 53 -j ACCEPT' >/dev/null
	if [ $? -ne 0 ]; then
		iptables -I INPUT 1 -p udp --dport 53 -j ACCEPT
		if [ $? -ne 0 ]; then
			exit_with_error "iptables -I INPUT 1 -p udp --dport 53 -j ACCEPT failed"
		fi
		echo "iptables:Open udp port 53 for DHCP"
	fi

	iptables-save | grep 'A INPUT -p tcp -m tcp --dport 53 -j ACCEPT' >/dev/null
	if [ $? -ne 0 ]; then
		iptables -I INPUT 1 -p tcp --dport 53 -j ACCEPT
		if [ $? -ne 0 ]; then
			exit_with_error "iptables -I INPUT 1 -p tcp --dport 53 -j ACCEPT failed"
		fi
		echo "iptables:Open tcp port 53 for DHCP"
	fi

	service iptables save
	if [ $? -ne 0 ]; then
		exit_with_error "service iptables save failed"
	fi
fi

[ -f /etc/dnsmasq-resolv.conf ] || echo nameserver $dns > /etc/dnsmasq-resolv.conf
[ $? -ne 0 ] && exit_with_error "cannot create /etc/dnsmasq-resolv.conf"

touch /var/log/dnsmasq.log && service dnsmasq restart
[ $? -ne 0 ] && exit_with_error "touch /var/log/dnsmasq.log && service dnsmasq restart failed"

exit 0
