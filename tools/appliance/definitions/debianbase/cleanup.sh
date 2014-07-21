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

function cleanup_apt() {
  #apt-get -y remove linux-headers-$(uname -r) build-essential
  apt-get -y remove dictionaries-common busybox
  apt-get -y autoremove
  apt-get autoclean
  apt-get clean
}

# Removing leftover leases and persistent rules
function cleanup_dhcp() {
  rm -f /var/lib/dhcp/*
}

# Make sure Udev doesn't block our network
function cleanup_dev() {
  echo "cleaning up udev rules"
  rm -f /etc/udev/rules.d/70-persistent-net.rules
  rm -rf /dev/.udev/
  rm -f /lib/udev/rules.d/75-persistent-net-generator.rules
}

function cleanup() {
  cleanup_apt
  cleanup_dhcp
  cleanup_dev
}

return 2>/dev/null || cleanup
