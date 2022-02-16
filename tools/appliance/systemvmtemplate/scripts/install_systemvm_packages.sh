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

function install_vhd_util() {
  [[ -f /bin/vhd-util ]] && return

  wget --no-check-certificate https://github.com/shapeblue/cloudstack-nonoss/raw/main/vhd-util -O /bin/vhd-util
  chmod a+x /bin/vhd-util
}

function debconf_packages() {
  echo 'sysstat sysstat/enable boolean true' | debconf-set-selections
  echo "strongwan strongwan/install_x509_certificate boolean false" | debconf-set-selections
  echo "strongwan strongwan/install_x509_certificate seen true" | debconf-set-selections
  echo "iptables-persistent iptables-persistent/autosave_v4 boolean true" | debconf-set-selections
  echo "iptables-persistent iptables-persistent/autosave_v6 boolean true" | debconf-set-selections
  echo "libc6 libraries/restart-without-asking boolean false" | debconf-set-selections
}

function apt_clean() {
  apt-get -y autoremove --purge
  apt-get clean
  apt-get autoclean
}

function install_packages() {
  export DEBIAN_FRONTEND=noninteractive
  export DEBIAN_PRIORITY=critical
  local arch=`dpkg --print-architecture`

  debconf_packages

  local apt_get="apt-get --no-install-recommends -q -y"

  ${apt_get} install grub-legacy \
    rsyslog logrotate cron net-tools ifupdown tmux vim-tiny htop netbase iptables nftables \
    openssh-server e2fsprogs tcpdump iftop socat wget coreutils systemd \
    python python3 python3-flask ieee-data \
    bzip2 sed gawk diffutils grep gzip less tar telnet ftp rsync traceroute psmisc lsof procps \
    inetutils-ping iputils-arping httping curl \
    dnsutils zip unzip ethtool uuid file iproute2 acpid sudo \
    sysstat \
    apache2 ssl-cert \
    dnsmasq dnsmasq-utils \
    nfs-common \
    samba-common cifs-utils \
    xl2tpd bcrelay ppp tdb-tools \
    xenstore-utils libxenstore3.0 \
    ipvsadm conntrackd libnetfilter-conntrack3 \
    keepalived irqbalance \
    openjdk-11-jre-headless \
    ipcalc ipset \
    iptables-persistent \
    libtcnative-1 libssl-dev libapr1-dev \
    haproxy \
    haveged \
    radvd \
    sharutils genisoimage \
    strongswan libcharon-extra-plugins libstrongswan-extra-plugins strongswan-charon strongswan-starter \
    virt-what open-vm-tools qemu-guest-agent hyperv-daemons cloud-guest-utils \
    conntrack apt-transport-https ca-certificates curl gnupg  gnupg-agent software-properties-common

  apt-get install -y python3-json-pointer python3-jsonschema cloud-init

  # python2-netaddr workaround
  wget https://github.com/shapeblue/cloudstack-nonoss/raw/main/python-netaddr_0.7.19-1_all.deb
  dpkg -i python-netaddr_0.7.19-1_all.deb

  apt_clean

  # 32 bit architecture support for vhd-util
  if [ "${arch}" != "i386" ]; then
    dpkg --add-architecture i386
    apt-get update
    ${apt_get} install libuuid1:i386 libc6:i386
  fi

  # Install docker and containerd for CKS
  curl -fsSL https://download.docker.com/linux/debian/gpg | sudo apt-key add -
  apt-key fingerprint 0EBFCD88
  add-apt-repository "deb [arch=amd64] https://download.docker.com/linux/debian $(lsb_release -cs) stable"
  apt-get update
  ${apt_get} install containerd.io

  apt_clean

  install_vhd_util
  # Install xenserver guest utilities as debian repos don't have it
  wget https://mirrors.kernel.org/ubuntu/pool/main/x/xe-guest-utilities/xe-guest-utilities_7.10.0-0ubuntu1_amd64.deb
  dpkg -i xe-guest-utilities_7.10.0-0ubuntu1_amd64.deb
  rm -f xe-guest-utilities_7.10.0-0ubuntu1_amd64.deb
}

return 2>/dev/null || install_packages
