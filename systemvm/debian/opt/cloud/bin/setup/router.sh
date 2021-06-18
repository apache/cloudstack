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

check_reboot_vmware() {
  if [ "$HYPERVISOR" != "vmware" ]; then
    return
  fi

  if [ -n "$MGMTNET" ]; then
    MGMT_GW=$(echo $MGMTNET | awk -F "." '{print $1"."$2"."$3".1"}')
    if ping -n -c 1 -W 3 $MGMT_GW &> /dev/null; then
      log_it "Management gateway pingable, skipping VR reboot"
      return
    fi
  fi

  log_it "Management gateway not pingable, rebooting VR"
  sync
  reboot
}

setup_router() {
  # To save router public interface and gw ip information
  touch /var/cache/cloud/ifaceGwIp

  oldmd5=
  [ -f "/etc/udev/rules.d/70-persistent-net.rules" ] && oldmd5=$(md5sum "/etc/udev/rules.d/70-persistent-net.rules" | awk '{print $1}')

  if [ -n "$ETH2_IP" ]; then
    setup_common eth0 eth1 eth2

    if [ -n "$EXTRA_PUBNICS" ]; then
      for ((i = 3; i < 3 + $EXTRA_PUBNICS; i++)); do
        setup_interface "$i" "0.0.0.0" "255.255.255.255" $GW "force"
      done
    fi
  else
    setup_common eth0 eth1
    if [ -n "$EXTRA_PUBNICS" ]; then
      for ((i = 2; i < 2 + $EXTRA_PUBNICS; i++)); do
        setup_interface "$i" "0.0.0.0" "255.255.255.255" $GW "force"
      done
    fi
  fi

  log_it "Checking udev NIC assignment order changes"
  if [ "$NIC_MACS" != "" ]
  then
    init_interfaces_orderby_macs "$NIC_MACS" "/tmp/interfaces" "/tmp/udev-rules"
    newmd5=$(md5sum "/tmp/udev-rules" | awk '{print $1}')
    rm /tmp/interfaces
    rm /tmp/udev-rules

    if [ "$oldmd5" != "$newmd5" ]
    then
      log_it "Reloading udev for new udev NIC assignment"
      udevadm control --reload-rules && udevadm trigger
      check_reboot_vmware
    fi
  fi

  setup_aesni
  setup_dnsmasq
  setup_apache2 $ETH0_IP

  sed -i /$NAME/d /etc/hosts
  echo "$ETH0_IP $NAME" >> /etc/hosts

  enable_irqbalance 1
  disable_rpfilter_domR
  enable_fwding 1
  enable_rpsrfs 1
  enable_passive_ftp 1
  cp /etc/iptables/iptables-router /etc/iptables/rules.v4
  setup_sshd $ETH1_IP "eth1"

  # Only allow DNS service for current network
  sed -i "s/-A INPUT -i eth0 -p udp -m udp --dport 53 -j ACCEPT/-A INPUT -i eth0 -p udp -m udp --dport 53 -s $DHCP_RANGE\/$CIDR_SIZE -j ACCEPT/g" /etc/iptables/rules.v4
  sed -i "s/-A INPUT -i eth0 -p tcp -m tcp --dport 53 -j ACCEPT/-A INPUT -i eth0 -p tcp -m tcp --dport 53 -s $DHCP_RANGE\/$CIDR_SIZE -j ACCEPT/g" /etc/iptables/rules.v4

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
setup_router
