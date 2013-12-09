#!/bin/bash
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

#set -x
 
usage() {
  printf "Usage: \n" $(basename $0)

}

if [ ! -f "/etc/modprobe.d/disable-ipv6" ] ; then
    # disable IPv6 until reboot
    if [ -d "/proc/sys/net/ipv6/conf/all" ] ; then
        /sbin/sysctl -w net.ipv6.conf.all.forwarding=0
        /sbin/sysctl -w net.ipv6.conf.all.accept_ra=0
        /sbin/sysctl -w net.ipv6.conf.all.accept_redirects=0
        /sbin/sysctl -w net.ipv6.conf.all.autoconf=0
        /sbin/sysctl -w net.ipv6.conf.all.disable_ipv6=1
    fi

    # reinstate the disable-ipv6 file
    echo "alias ipv6 no" > /etc/modprobe.d/disable-ipv6
    echo "alias net-pf-10 off" >> /etc/modprobe.d/disable-ipv6
fi

#removing iptables entry for vnc ports
iptables -D RH-Firewall-1-INPUT -p tcp -m tcp --dport 5900:6099 -j ACCEPT 2>&1

# remove listening vnc on all interface
sed -i 's/0\.0\.0\.0/127\.0\.0\.1/' /opt/xensource/libexec/vncterm-wrapper 2>&1
sed -i 's/0\.0\.0\.0/127\.0\.0\.1/' /opt/xensource/libexec/qemu-dm-wrapper 2>&1

# disable the default link local on xenserver
sed -i /NOZEROCONF/d /etc/sysconfig/network
echo "NOZEROCONF=yes" >> /etc/sysconfig/network

mv -n /etc/cron.daily/logrotate /etc/cron.hourly 2>&1

# more aio thread
echo 1048576 >/proc/sys/fs/aio-max-nr

# empty heartbeat
cat /dev/null > /opt/cloud/bin/heartbeat
# empty knownhost
cat /dev/null > /root/.ssh/known_hosts

rm -f /opt/xensource/packages/iso/systemvm-premium.iso

echo "success"

