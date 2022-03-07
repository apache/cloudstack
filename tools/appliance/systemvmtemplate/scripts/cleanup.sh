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

function cleanup_apt() {
  export DEBIAN_FRONTEND=noninteractive
  apt-get -y remove --purge dictionaries-common busybox \
    task-english task-ssh-server tasksel tasksel-data laptop-detect wamerican sharutils \
    nano util-linux-locales krb5-locales

  apt-get -y autoremove --purge
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

function cleanup_misc() {
  # Scripts
  rm -fr /home/cloud/cloud_scripts*
  rm -f /usr/share/cloud/cloud-scripts.tar
  rm -f /root/.rnd
  rm -f /var/www/html/index.html
  # Logs
  rm -f /var/log/*.log
  rm -f /var/log/apache2/*
  rm -f /var/log/messages
  rm -f /var/log/syslog
  rm -f /var/log/messages
  rm -fr /var/log/apt
  rm -fr /var/log/installer
  # Docs and data files
  rm -fr /var/lib/apt/*
  rm -fr /var/cache/apt/*
  rm -fr /var/cache/debconf/*old
  rm -fr /usr/share/doc
  rm -fr /usr/share/man
  rm -fr /usr/share/info
  rm -fr /usr/share/lintian
  rm -fr /usr/share/apache2/icons
  find /usr/share/locale -type f | grep -v en_US | xargs rm -fr
  find /usr/share/zoneinfo -type f | grep -v UTC | xargs rm -fr
  rm -fr /tmp/*
}

function cleanup() {
  cleanup_apt
  cleanup_dhcp
  cleanup_dev
  cleanup_misc
}

return 2>/dev/null || cleanup
