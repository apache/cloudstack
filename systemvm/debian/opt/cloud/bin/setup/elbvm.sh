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

elbvm_svcs() {
  echo "haproxy" > /var/cache/cloud/enabled_svcs
  echo "cloud dnsmasq conntrackd keepalived apache2 nfs-common portmap" > /var/cache/cloud/disabled_svcs
}

setup_elbvm() {
  log_it "Setting up Elastic Load Balancer system vm"
  sed -i  /$NAME/d /etc/hosts
  public_ip=$ETH2_IP
  [ "$ETH2_IP" == "0.0.0.0" ] || [ "$ETH2_IP" == "" ] && public_ip=$ETH0_IP
  echo "$public_ip $NAME" >> /etc/hosts

  enable_fwding 0
  enable_irqbalance 0
}

elbvm_svcs
if [ $? -gt 0 ]
then
  log_it "Failed to execute elbvm svcs"
  exit 1
fi
setup_elbvm
. /opt/cloud/bin/setup/patch.sh && patch_router
