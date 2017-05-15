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

  wget --no-check-certificate http://download.cloudstack.org/tools/vhd-util -O /bin/vhd-util
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

function install_packages() {
  export DEBIAN_FRONTEND=noninteractive
  export DEBIAN_PRIORITY=critical
  local arch=`dpkg --print-architecture`

  debconf_packages
  install_vhd_util

  local apt_get="apt-get --no-install-recommends -q -y --force-yes"

  #32 bit architecture support:: not required for 32 bit template
  if [ "${arch}" != "i386" ]; then
    dpkg --add-architecture i386
    apt-get update
    ${apt_get} install links:i386 libuuid1:i386 libc6:i386
  fi

  ${apt_get} install \
    rsyslog logrotate cron chkconfig insserv net-tools ifupdown vim-tiny netbase iptables \
    openssh-server e2fsprogs dhcp3-client tcpdump socat wget \
    python bzip2 sed gawk diffutils grep gzip less tar telnet ftp rsync traceroute psmisc lsof procps \
    inetutils-ping iputils-arping httping  curl \
    dnsutils zip unzip ethtool uuid file iproute acpid virt-what sudo \
    sysstat python-netaddr \
    apache2 ssl-cert \
    dnsmasq dnsmasq-utils \
    nfs-common \
    samba-common cifs-utils \
    xl2tpd bcrelay ppp ipsec-tools tdb-tools \
    xenstore-utils libxenstore3.0 \
    conntrackd ipvsadm libnetfilter-conntrack3 libnl-3-200 libnl-genl-3-200 \
    ipcalc \
    ipset \
    iptables-persistent \
    libtcnative-1 libssl-dev libapr1-dev \
    python-flask \
    haproxy \
    radvd \
    sharutils

  ${apt_get} -t wheezy-backports install keepalived irqbalance open-vm-tools qemu-guest-agent
  ${apt_get} -t wheezy-backports install strongswan libcharon-extra-plugins libstrongswan-extra-plugins

  apt-get update
  apt-get -y --force-yes upgrade

  if [ "${arch}" == "amd64" ]; then
    # Hyperv  kvp daemon - 64bit only
    # Download the hv kvp daemon
    wget http://people.apache.org/~rajeshbattala/hv-kvp-daemon_3.1_amd64.deb
    dpkg -i hv-kvp-daemon_3.1_amd64.deb
    rm -f hv-kvp-daemon_3.1_amd64.deb
    # XS tools
    wget --no-check-certificate https://raw.githubusercontent.com/rhtyd/cloudstack-nonoss/master/xe-guest-utilities_6.5.0_amd64.deb
    md5sum xe-guest-utilities_6.5.0_amd64.deb
    dpkg -i xe-guest-utilities_6.5.0_amd64.deb
    rm -f xe-guest-utilities_6.5.0_amd64.deb
  fi

  # Install OpenJDK8 pkgs maintained by Azul
  apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys 0x219BD9C9
  echo 'deb http://repos.azulsystems.com/debian stable main' > /etc/apt/sources.list.d/zulu.list
  apt-get -y autoremove
  apt-get autoclean
  apt-get clean
  apt-get update
  ${apt_get} install zulu-8
  java -version
}

return 2>/dev/null || install_packages
