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

setup_k8s_node() {
    log_it "Setting up k8s node"

    update-alternatives --set iptables /usr/sbin/iptables-legacy
    update-alternatives --set ip6tables /usr/sbin/ip6tables-legacy
    update-alternatives --set arptables /usr/sbin/arptables-legacy
    update-alternatives --set ebtables /usr/sbin/ebtables-legacy

    # set default ssh port and restart sshd service
    sed -i 's/3922/22/g' /etc/ssh/sshd_config
    systemctl restart ssh

    # Prevent root login
    > /root/.ssh/authorized_keys
    passwd -l root
    #sed -i 's#root:x:0:0:root:/root:/bin/bash#root:x:0:0:root:/root:/sbin/nologin#' /etc/passwd

    swapoff -a
    sudo sed -i '/ swap / s/^/#/' /etc/fstab
    log_it "Swap disabled"

    log_it "Setting up interfaces"
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
    dhclient -1

    rm -f /etc/logrotate.d/cloud

    # Enable cloud-init without any aid from ds-identify
    echo "policy: enabled" >  /etc/cloud/ds-identify.cfg

    # Add ConfigDrive to datasource_list
    sed -i "s/datasource_list: .*/datasource_list: ['ConfigDrive', 'CloudStack']/g" /etc/cloud/cloud.cfg.d/cloudstack.cfg

    log_it "Starting cloud-init services"
    systemctl enable --now --no-block containerd
    if [ -f /home/cloud/success ]; then
      systemctl stop cloud-init cloud-config cloud-final
      systemctl disable cloud-init cloud-config cloud-final
    else
      systemctl start --no-block cloud-init
      systemctl start --no-block cloud-config
      systemctl start --no-block cloud-final
    fi
}

setup_k8s_node
. /opt/cloud/bin/setup/patch.sh && patch_sshd_config
