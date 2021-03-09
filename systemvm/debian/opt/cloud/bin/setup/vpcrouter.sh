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

    cat > /etc/network/interfaces << EOF
auto lo eth0
iface lo inet loopback
EOF
  setup_interface "0" $ETH0_IP $ETH0_MASK $GW

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
  if [ -n "$MGMTNET"  -a -n "$LOCAL_GW" ]
  then
     if [ "$HYPERVISOR" == "vmware" ] || [ "$HYPERVISOR" == "hyperv" ];
     then
         ip route add $MGMTNET via $LOCAL_GW dev eth0
         # workaround to activate vSwitch under VMware
         timeout 3 ping -n -c 3 $LOCAL_GW || true
     fi
  fi

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
  setup_sshd $ETH0_IP "eth0"
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

  # Setup hourly lograte in systemd timer
  sed -i 's/OnCalendar=daily/OnCalendar=hourly/g' /usr/lib/systemd/system/logrotate.timer
  sed -i 's/AccuracySec=12h/AccuracySec=5m/g' /usr/lib/systemd/system/logrotate.timer

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
