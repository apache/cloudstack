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

set -x

echo "Running k8s node setup script"

. /opt/cloud/bin/setup/common.sh

setup_k8s_node() {
    log_it "Setting up k8s master vm"

    # set default ssh port and restart sshd service
    sed -i 's/3922/22/g' /etc/ssh/sshd_config

    swapoff -a
    sudo sed -ri '/\swap\s/s/^#?/#/' /etc/fstab
    log_it "Swap disabled"

    log_it "Setting up interfaces"
    setup_common eth0
    setup_system_rfc1918_internal

    log_it "Setting up entry in hosts"
    sed -i  /$NAME/d /etc/hosts
    echo "$ETH0_IP $NAME" >> /etc/hosts

    public_ip=`getPublicIp`
    echo "$public_ip $NAME" >> /etc/hosts

    echo "export PATH='$PATH:/opt/bin/'">> ~/.bashrc

    disable_rpfilter
    enable_fwding 1
    enable_irqbalance 0
    setup_ntp

    rm -f /etc/logrotate.d/cloud

    log_it "Starting cloud-init services"
    systemctl enable --now --no-block docker
    systemctl enable --now --no-block cloud-init
    systemctl enable --now --no-block cloud-config
    systemctl enable --now --no-block cloud-final
}

setup_k8s_node