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

# Perform fsck check on every 3rd boot
function fix_tune2fs() {
  for partition in $(blkid -o list | grep ext | awk '{print $1}')
  do
    tune2fs -m 0 $partition
    tune2fs -c 4 $partition
  done
  fdisk -l
  df -h
  uname -a
}

function add_backports() {
  sed -i '/cdrom/d' /etc/apt/sources.list
  sed -i '/deb-src/d' /etc/apt/sources.list
  sed -i '/backports/d' /etc/apt/sources.list
  sed -i '/security/d' /etc/apt/sources.list
  echo 'deb http://http.debian.net/debian bullseye-backports main' >> /etc/apt/sources.list
  echo 'deb http://security.debian.org/debian-security bullseye-security main' >> /etc/apt/sources.list
}

function apt_upgrade() {
  DEBIAN_FRONTEND=noninteractive
  DEBIAN_PRIORITY=critical

  fix_tune2fs

  # Setup sudo
  echo 'cloud ALL=(ALL) NOPASSWD: ALL' > /etc/sudoers.d/cloud

  add_backports

  rm -fv /root/*.iso
  apt-get -q -y update

  apt-get -q -y upgrade
  apt-get -q -y dist-upgrade

  apt-get -y autoremove --purge
  apt-get autoclean
  apt-get clean
}

return 2>/dev/null || apt_upgrade
