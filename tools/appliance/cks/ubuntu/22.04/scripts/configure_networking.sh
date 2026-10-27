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

set -e
set -x

HOSTNAME=cksnode

function configure_resolv_conf() {
  grep 8.8.8.8 /etc/resolv.conf && grep 8.8.4.4 /etc/resolv.conf && return

  cat > /etc/resolv.conf << EOF
nameserver 8.8.8.8
nameserver 8.8.4.4
EOF
}

# Delete entry in /etc/hosts derived from dhcp
function delete_dhcp_ip() {
  result=$(grep 127.0.1.1 /etc/hosts || true)
  [ "${result}" == "" ] && return

  sed -i '/127.0.1.1/d' /etc/hosts
}

function configure_hostname() {
  sed -i "s/root@\(.*\)$/root@$HOSTNAME/g" /etc/ssh/ssh_host_*.pub

  echo "$HOSTNAME" > /etc/hostname
  hostname $HOSTNAME
}

function configure_interfaces() {
  cat > /etc/network/interfaces << EOF
source /etc/network/interfaces.d/*

# The loopback network interface
auto lo
iface lo inet loopback

# The primary network interface
auto ens35
iface ens35 inet dhcp

EOF

echo "net.ipv4.ip_forward = 1" >> /etc/sysctl.conf
sysctl -p /etc/sysctl.conf
}

function configure_networking() {
  configure_interfaces
  configure_resolv_conf
  delete_dhcp_ip
  configure_hostname
}

return 2>/dev/null || configure_networking
