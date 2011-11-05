#!/bin/bash
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

