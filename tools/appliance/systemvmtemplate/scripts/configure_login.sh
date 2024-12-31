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

function add_admin_group() {
  groupadd -f -r admin
}

function configure_user() {
  usermod -a -G admin cloud
  mkdir -p /home/cloud/.ssh
  chmod 700 /home/cloud/.ssh
  echo "root:password" | chpasswd
}

function configure_inittab() {
  # Fix inittab
  cat >> /etc/inittab << EOF

0:2345:respawn:/sbin/getty -L 115200 ttyS0 vt102
vc:2345:respawn:/sbin/getty 38400 hvc0
EOF
}

function configure_login() {
  configure_inittab
  add_admin_group
  configure_user
}

return 2>/dev/null || configure_login
