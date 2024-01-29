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

ilbvm_svcs() {
  echo "haproxy" > /var/cache/cloud/enabled_svcs
  echo "cloud dnsmasq conntrackd keepalived apache2 nfs-common portmap" > /var/cache/cloud/disabled_svcs
}

setup_ilbvm() {
  log_it "Setting up Internal Load Balancer system vm"
  #eth0 = guest network, eth1=control network

  sed -i  /$NAME/d /etc/hosts
  echo "$ETH0_IP $NAME" >> /etc/hosts

  cp /etc/iptables/iptables-ilbvm /etc/iptables/rules.v4
  setup_sshd $ETH1_IP "eth1"

  enable_fwding 0
  enable_irqbalance 1
}

ilbvm_svcs
if [ $? -gt 0 ]
then
  log_it "Failed to execute ilbvm svcs"
  exit 1
fi
setup_ilbvm
