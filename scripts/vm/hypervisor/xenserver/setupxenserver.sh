#!/bin/bash
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
# Version @VERSION@

#set -x
 
usage() {
  printf "Usage: \n" $(basename $0)

}

#set iptables
iptables -D RH-Firewall-1-INPUT -p tcp -m tcp --dport 5900:6099 -j ACCEPT 2>/dev/null
iptables -I RH-Firewall-1-INPUT -p tcp -m tcp --dport 5900:6099 -j ACCEPT 2>&1

# listen vnc on all interface
sed -i 's/127\.0\.0\.1/0\.0\.0\.0/' /opt/xensource/libexec/vncterm-wrapper 2>&1
sed -i 's/127\.0\.0\.1/0\.0\.0\.0/' /opt/xensource/libexec/qemu-dm-wrapper 2>&1

# disable the default link local on xenserver
sed -i /NOZEROCONF/d /etc/sysconfig/network
echo "NOZEROCONF=yes" >> /etc/sysconfig/network

mv /etc/cron.daily/logrotate /etc/cron.hourly 2>&1

# more aio thread
echo 1048576 >/proc/sys/fs/aio-max-nr

# empty heartbeat
cat /dev/null > /opt/xensource/bin/heartbeat
# empty knownhost
cat /dev/null > /root/.ssh/known_hosts

rm -f /opt/xensource/packages/iso/systemvm-premium.iso

echo "success"

