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

function install_packages() {
    apt-get install -y qemu-guest-agent rsyslog logrotate cron net-tools ifupdown cloud-guest-utils conntrack apt-transport-https ca-certificates curl \
     gnupg gnupg-agent software-properties-common gnupg lsb-release
    apt-get install -y python3-json-pointer python3-jsonschema cloud-init resolvconf

    sudo mkdir -p /etc/apt/keyrings
    echo "Creating /opt/bin directory"
    sudo mkdir -p /opt/bin
    curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg
    echo \
      "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu \
      $(lsb_release -cs) stable" | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null
      apt update
      apt install containerd.io

      systemctl start containerd
      systemctl enable containerd
}

function configure_services() {
  install_packages

  systemctl daemon-reload
cat <<EOF > /etc/cloud/cloud.cfg.d/cloudstack.cfg
datasource_list: ['CloudStack']
datasource:
  CloudStack:
    max_wait: 120
    timeout: 50
EOF
}

configure_services
