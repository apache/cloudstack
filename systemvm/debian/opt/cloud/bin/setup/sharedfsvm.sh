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

setup_sharedfsvm() {
    log_it "Setting up sharedfsvm"

    update-alternatives --set iptables /usr/sbin/iptables-legacy
    update-alternatives --set ip6tables /usr/sbin/ip6tables-legacy
    update-alternatives --set arptables /usr/sbin/arptables-legacy
    update-alternatives --set ebtables /usr/sbin/ebtables-legacy

    log_it "Setting up entry in hosts"
    sed -i  /$NAME/d /etc/hosts
    echo "$ETH0_IP $NAME" >> /etc/hosts

    # set default ssh port and restart sshd service
    sed -i 's/3922/22/g' /etc/ssh/sshd_config
    systemctl restart ssh

    > /root/.ssh/authorized_keys
    swapoff -a
    sudo sed -i '/ swap / s/^/#/' /etc/fstab
    log_it "Swap disabled"

    echo "export PATH='$PATH:/opt/bin/'">> ~/.bashrc

    disable_rpfilter
    enable_fwding 0
    enable_irqbalance 0
    setup_ntp
    dhclient -1

    rm -f /etc/logrotate.d/cloud

    log_it "Starting cloud-init services"
    if [ -f /home/cloud/success ]; then
      systemctl stop cloud-init cloud-config cloud-final
      systemctl disable cloud-init cloud-config cloud-final
    else
      systemctl start --no-block cloud-init
      systemctl start --no-block cloud-config
      systemctl start --no-block cloud-final
    fi
}

setup_sharedfsvm
. /opt/cloud/bin/setup/patch.sh && patch_sshd_config
