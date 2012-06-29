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


# prepare dnsmasq on external dhcp server
# Usage:
# 	sh prepare_dnsmasq gateway dns self_ip

gateway=$1
dns=$2
self_ip=$3

exit_with_error() {
	echo $1
	exit 1
}

config_dnsmasq() {
	echo "$*" >> /etc/dnsmasq.conf
	[ $? -ne 0 ] && exit_with_error "echo $* failed"
}

[ $# -ne 3 ] && exit_with_error "Usage: prepare_dnsmasq gateway dns self_ip"

[ -f /etc/dnsmasq.conf ] || exit_with_error "Can not found /etc/dnsmasq.conf"

touch /var/log/dnsmasq.log
[ $? -ne 0 ] && exit_with_error "touch /var/log/dnsmasq.log failed"
touch /etc/dnsmasq-resolv.conf
[ $? -ne 0 ] && exit_with_error "touch /etc/dnsmasq-resolv.conf failed"
echo "nameserver $dns">/etc/dnsmasq-resolv.conf
[ $? -ne 0 ] && exit_with_error "echo \"nameserver $dns\">/etc/dnsmasq-resolv.conf failed"
touch /var/lib/dnsmasq.trace
[ $? -ne 0 ] && exit_with_error "touch /var/lib/dnsmasq.trace failed"


#produce echoer.sh
cat > /usr/bin/echoer.sh<<'EOF'
#!/bin/sh

sed -i /"$*"/d /var/lib/dnsmasq.trace
echo "$*" >> /var/lib/dnsmasq.trace
EOF
[ $? -ne 0 ] && exit_with_error "can't produce /usr/bin/echoer.sh"

#produce lease_checker.sh
cat > /usr/bin/lease_checker.sh<<'EOF'
#!/bin/sh
# Usage: lease_checker dhcp_entry_state(add/old/del) mac ip
state=$1
mac=$2
ip=$3

exit_with_error() {
	echo $1
	exit $2
}

[ $# -ne 3 ] && exit_with_error "Wrong arguments.Usage: lease_checker dhcp_entry_state(add/old/del) mac ip" -3

[ -f /var/lib/dnsmasq.trace ] || exit_with_error "Cannot find /var/lib/dnsmasq" -1
pidof dnsmasq &>/dev/null
[ $? -ne 0 ] && exit_with_error "Dnsmasq is not running" -2

grep "$state $mac $ip" /var/lib/dnsmasq.trace
if [ $? -ne 0 ]; then
	exit $?
else
	sed -i /"$state $mac $ip"/d /var/lib/dnsmasq.trace
	exit 0
fi

EOF

chmod +x /usr/bin/echoer.sh
[ $? -ne 0 ] && exit_with_error "chmod +x /usr/bin/echoer.sh failed"

# Configure dnsmasq with comments
echo "# This is produced by CloudStack" > /etc/dnsmasq.conf
config_dnsmasq "# Never forward plain names (without a dot or domain part)"
config_dnsmasq domain-needed
config_dnsmasq "# Never forward addresses in the non-routed address spaces."
config_dnsmasq bogus-priv
config_dnsmasq "
# Change this line if you want dns to get its upstream servers from
# somewhere other that /etc/resolv.conf"
config_dnsmasq resolv-file=/etc/dnsmasq-resolv.conf
config_dnsmasq "
# Add local-only domains here, queries in these domains are answered
# from /etc/hosts or DHCP only."
config_dnsmasq local=/cloudnine.internal/
config_dnsmasq "
# On systems which support it, dnsmasq binds the wildcard address,
# even when it is listening on only some interfaces. It then discards
# requests that it shouldn't reply to. This has the advantage of
# working even when interfaces come and go and change address. If you
# want dnsmasq to really bind only the interfaces it is listening on,
# uncomment this option. About the only time you may need this is when
# running another nameserver on the same machine."
config_dnsmasq bind-interfaces
config_dnsmasq "
# Set this (and domain: see below) if you want to have a domain
# automatically added to simple names in a hosts-file."
config_dnsmasq expand-hosts
config_dnsmasq "
# does the following things.
# 1) Allows DHCP hosts to have fully qualified domain names, as long
#     as the domain part matches this setting.
# 2) Sets the \"domain\" DHCP option thereby potentially setting the
#    domain of all systems configured by DHCP
# 3) Provides the domain part for \"expand-hosts\"
"
config_dnsmasq domain=cloudnine.internal
config_dnsmasq "
# Send options to hosts which ask for a DHCP lease.
# See RFC 2132 for details of available options.
# Common options can be given to dnsmasq by name: 
# run \"dnsmasq --help dhcp\" to get a list.
# Note that all the common settings, such as netmask and
# broadcast address, DNS server and default route, are given
# sane defaults by dnsmasq. You very likely will not need 
# any dhcp-options. If you use Windows clients and Samba, there
# are some options which are recommended, they are detailed at the
# end of this section.

# Override the default route supplied by dnsmasq, which assumes the
# router is the same machine as the one running dnsmasq."
config_dnsmasq dhcp-option=option:router,$gateway
config_dnsmasq "
# Uncomment this to enable the integrated DHCP server, you need
# to supply the range of addresses available for lease and optionally
# a lease time. If you have more than one network, you will need to
# repeat this for each network on which you want to supply DHCP
# service."
config_dnsmasq dhcp-range=$self_ip,static
config_dnsmasq dhcp-hostsfile=/etc/dhcphosts.txt
config_dnsmasq "# Set the domain"
config_dnsmasq dhcp-option=15,"cloudnine.internal"
config_dnsmasq "
# Send microsoft-specific option to tell windows to release the DHCP lease
# when it shuts down. Note the \"i\" flag, to tell dnsmasq to send the
# value as a four-byte integer - that's what microsoft wants. See
# http://technet2.microsoft.com/WindowsServer/en/library/a70f1bb7-d2d4-49f0-96d6-4b7414ecfaae1033.mspx?mfr=true"
config_dnsmasq dhcp-option=vendor:MSFT,2,1i
config_dnsmasq "
# The DHCP server needs somewhere on disk to keep its lease database.
# This defaults to a sane location, but if you want to change it, use
# the line below.
#dhcp-leasefile=/var/lib/misc/dnsmasq.leases"
config_dnsmasq leasefile-ro
config_dnsmasq "
# For debugging purposes, log each DNS query as it passes through
# dnsmasq."
config_dnsmasq log-queries
config_dnsmasq log-facility=/var/log/dnsmasq.log
config_dnsmasq "
# Run an executable when a DHCP lease is created or destroyed.
# The arguments sent to the script are \"add\" or \"del\",
# then the MAC address, the IP address and finally the hostname
# if there is one."
config_dnsmasq dhcp-script=/usr/bin/echoer.sh
config_dnsmasq dhcp-scriptuser=root
config_dnsmasq dhcp-authoritative
config_dnsmasq "
# Ignore any bootp and pxe boot request
"
config_dnsmasq dhcp-ignore=bootp
config_dnsmasq dhcp-vendorclass=pxestuff,PXEClient
config_dnsmasq dhcp-ignore=pxestuff

[ -f /usr/sbin/setenforce ] && /usr/sbin/setenforce 0
[ $? -ne 0 ] && exit_with_error "Can not set seLinux to passive mode"

# Open DHCP ports in iptable
chkconfig --list iptables | grep "on"
if [ $? -eq 0 ]; then
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

# Set up upstream DNS
[ -f /etc/dnsmasq-resolv.conf ] || echo nameserver $dns > /etc/dnsmasq-resolv.conf
[ $? -ne 0 ] && exit_with_error "cannot create /etc/dnsmasq-resolv.conf"

service dnsmasq restart
[ $? -ne 0 ] && exit_with_error "service dnsmasq restart failed"

exit 0
