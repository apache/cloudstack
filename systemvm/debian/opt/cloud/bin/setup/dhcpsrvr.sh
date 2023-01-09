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

dhcpsrvr_svcs() {
  echo "dnsmasq apache2" > /var/cache/cloud/enabled_svcs
  echo "cloud nfs-common conntrackd keepalived haproxy portmap" > /var/cache/cloud/disabled_svcs
}

setup_dhcpsrvr() {
  log_it "Setting up dhcp server system vm"
  setup_dnsmasq
  setup_apache2 $ETH0_IP

  sed -i  /$NAME/d /etc/hosts
  [ $ETH0_IP ] && echo "$ETH0_IP $NAME" >> /etc/hosts
  [ $ETH0_IP6 ] && echo "$ETH0_IP6 $NAME" >> /etc/hosts

  enable_irqbalance 0
  enable_fwding 0

  #Only allow DNS service for current network
  sed -i "s/-A INPUT -i eth0 -p udp -m udp --dport 53 -j ACCEPT/-A INPUT -i eth0 -p udp -m udp --dport 53 -s $DHCP_RANGE\/$CIDR_SIZE -j ACCEPT/g" /etc/iptables/rules.v4
  sed -i "s/-A INPUT -i eth0 -p tcp -m tcp --dport 53 -j ACCEPT/-A INPUT -i eth0 -p tcp -m tcp --dport 53 -s $DHCP_RANGE\/$CIDR_SIZE -j ACCEPT/g" /etc/iptables/rules.v4

  log_it "Disable radvd for dhcp server system vm"
  rm -rf /etc/radvd.conf
  systemctl stop radvd
  systemctl disable radvd
}

dhcpsrvr_svcs
if [ $? -gt 0 ]
then
  log_it "Failed to execute dhcpsrvr_svcs"
  exit 1
fi
setup_dhcpsrvr
