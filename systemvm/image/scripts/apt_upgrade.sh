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

function add_backports() {
  sed -i '/cdrom/d' /etc/apt/sources.list
  sed -i '/deb-src/d' /etc/apt/sources.list
  sed -i '/backports/d' /etc/apt/sources.list
  sed -i '/security/d' /etc/apt/sources.list
  echo 'deb http://ftp.debian.org/debian/ bullseye-backports main' >> /etc/apt/sources.list
  echo 'deb http://security.debian.org/debian-security bullseye-security main' >> /etc/apt/sources.list
}

function apt_upgrade() {
  DEBIAN_FRONTEND=noninteractive
  DEBIAN_PRIORITY=critical

  add_backports

  rm -fv /root/*.iso
  apt-get -q -y update
  apt-get -q -y upgrade
  apt-get -q -y dist-upgrade
}

return 2>/dev/null || apt_upgrade
