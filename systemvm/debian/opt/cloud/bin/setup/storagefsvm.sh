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

setup_storagefsvm() {
    log_it "Starting cloud-init services"
    log_it "Setting up storagefsvm"

    update-alternatives --set iptables /usr/sbin/iptables-legacy
    update-alternatives --set ip6tables /usr/sbin/ip6tables-legacy
    update-alternatives --set arptables /usr/sbin/arptables-legacy
    update-alternatives --set ebtables /usr/sbin/ebtables-legacy

    swapoff -a
    sudo sed -i '/ swap / s/^/#/' /etc/fstab
    log_it "Swap disabled"

    log_it "Setting up interfaces"
    setup_system_rfc1918_internal

    log_it "Setting up entry in hosts"
    sed -i  /$NAME/d /etc/hosts
    echo "$ETH0_IP $NAME" >> /etc/hosts

    echo "export PATH='$PATH:/opt/bin/'">> ~/.bashrc

    disable_rpfilter
    enable_fwding 1
    enable_irqbalance 0
    setup_ntp
    dhclient -1

    rm -f /etc/logrotate.d/cloud

    if [ -f /home/cloud/success ]; then
      systemctl stop cloud-init cloud-config cloud-final
      systemctl disable cloud-init cloud-config cloud-final
    else
      systemctl start --no-block cloud-init
      systemctl start --no-block cloud-config
      systemctl start --no-block cloud-final
    fi
}

setup_storagefsvm
