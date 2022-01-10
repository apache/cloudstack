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

setup_console_proxy() {
  log_it "Setting up console proxy system vm"

  echo "cloud" > /var/cache/cloud/enabled_svcs
  echo "haproxy dnsmasq apache2 nfs-common portmap" > /var/cache/cloud/disabled_svcs
  mkdir -p /var/log/cloud

  setup_system_rfc1918_internal

  log_it "Setting up entry in hosts"
  sed -i /$NAME/d /etc/hosts
  public_ip=`getPublicIp`
  echo "$public_ip $NAME" >> /etc/hosts

  disable_rpfilter
  enable_fwding 0
  enable_irqbalance 0
  rm -f /etc/logrotate.d/cloud

}

setup_console_proxy
