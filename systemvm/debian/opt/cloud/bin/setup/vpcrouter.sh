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

. /opt/cloud/bin/setup/common.sh

setup_vpcrouter() {
  log_it "Setting up VPC virtual router system vm"

  if [ -f /etc/hosts ]; then
    grep -q $NAME /etc/hosts || echo "127.0.0.1 $NAME" >> /etc/hosts;
  fi

  echo $NAME > /etc/hostname
  echo 'AVAHI_DAEMON_DETECT_LOCAL=0' > /etc/default/avahi-daemon
  hostnamectl set-hostname $NAME

  #Nameserver
  sed -i -e "/^nameserver.*$/d" /etc/resolv.conf # remove previous entries
  sed -i -e "/^nameserver.*$/d" /etc/dnsmasq-resolv.conf # remove previous entries
  if [ -n "$internalNS1" ]
  then
    echo "nameserver $internalNS1" > /etc/dnsmasq-resolv.conf
    echo "nameserver $internalNS1" > /etc/resolv.conf
  fi

  if [ -n "$internalNS2" ]
  then
    echo "nameserver $internalNS2" >> /etc/dnsmasq-resolv.conf
    echo "nameserver $internalNS2" >> /etc/resolv.conf
  fi
  if [ -n "$NS1" ]
  then
    echo "nameserver $NS1" >> /etc/dnsmasq-resolv.conf
    echo "nameserver $NS1" >> /etc/resolv.conf
  fi

  if [ -n "$NS2" ]
  then
    echo "nameserver $NS2" >> /etc/dnsmasq-resolv.conf
    echo "nameserver $NS2" >> /etc/resolv.conf
  fi

  if [ -n "$IP6_NS1" ]
  then
    echo "nameserver $IP6_NS1" >> /etc/dnsmasq-resolv.conf
    echo "nameserver $IP6_NS1" >> /etc/resolv.conf
  fi
  if [ -n "$IP6_NS2" ]
  then
    echo "nameserver $IP6_NS2" >> /etc/dnsmasq-resolv.conf
    echo "nameserver $IP6_NS2" >> /etc/resolv.conf
  fi

  setup_vpc_mgmt_route "0"

  ip route delete default
  # create route table for static route

  sudo echo "252 static_route" >> /etc/iproute2/rt_tables 2>/dev/null
  sudo echo "251 static_route_back" >> /etc/iproute2/rt_tables 2>/dev/null
  sudo ip rule add from $VPCCIDR table static_route 2>/dev/null
  sudo ip rule add from $VPCCIDR table static_route_back 2>/dev/null

  setup_vpc_apache2

  enable_irqbalance 1
  enable_vpc_rpsrfs 1
  disable_rpfilter
  enable_fwding 1
  enable_passive_ftp 1
  cp /etc/iptables/iptables-vpcrouter /etc/iptables/rules.v4
  cp /etc/vpcdnsmasq.conf /etc/dnsmasq.conf
  cp /etc/cloud-nic.rules /etc/udev/rules.d/cloud-nic.rules
  echo "" > /etc/dnsmasq.d/dhcphosts.txt
  echo "dhcp-hostsfile=/etc/dhcphosts.txt" > /etc/dnsmasq.d/cloud.conf

  [ -z $DOMAIN ] && DOMAIN="cloudnine.internal"
  #DNS server will append $DOMAIN to local queries
  sed -r -i s/^[#]?domain=.*$/domain=$DOMAIN/ /etc/dnsmasq.conf
  #answer all local domain queries
  sed  -i -e "s/^[#]*local=.*$/local=\/$DOMAIN\//" /etc/dnsmasq.conf

  command -v dhcp_release > /dev/null 2>&1
  no_dhcp_release=$?
  if [ $no_dhcp_release -eq 0 ]
  then
      echo 1 > /var/cache/cloud/dnsmasq_managed_lease
      sed -i -e "/^leasefile-ro/d" /etc/dnsmasq.conf
  else
      echo 0 > /var/cache/cloud/dnsmasq_managed_lease
  fi

  # Setup hourly logrotate
  if [ -f /etc/cron.daily/logrotate ]; then
    mv -n /etc/cron.daily/logrotate /etc/cron.hourly 2>&1
  fi

  # As ACS is changing the file, the description will also change to make it clear that ACS is handling this.
  sed -i "s#^Description=.*#Description=Cloudstack configuration time for rotation of log files#g" /usr/lib/systemd/system/logrotate.timer
  sed -i "s#^OnCalendar=.*#OnCalendar=$LOGROTATE_FREQUENCY#g" /usr/lib/systemd/system/logrotate.timer
  sed -i 's#^AccuracySec=.*#AccuracySec=5m#g' /usr/lib/systemd/system/logrotate.timer

  # reload daemon
  /usr/bin/systemctl daemon-reload

  # Load modules to support NAT traversal in VR
  modprobe nf_nat_pptp
}

routing_svcs
if [ $? -gt 0 ]
then
  log_it "Failed to execute routing_svcs"
  exit 1
fi
setup_vpcrouter
. /opt/cloud/bin/setup/patch.sh && patch_router
