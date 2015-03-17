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

  wget --no-check-certificate http://download.cloud.com.s3.amazonaws.com/tools/vhd-util -O /bin/vhd-util
  chmod a+x /bin/vhd-util
}

function debconf_packages() {
  echo 'sysstat sysstat/enable boolean true' | debconf-set-selections
  echo "openswan openswan/install_x509_certificate boolean false" | debconf-set-selections
  echo "openswan openswan/install_x509_certificate seen true" | debconf-set-selections
  echo "iptables-persistent iptables-persistent/autosave_v4 boolean true" | debconf-set-selections
  echo "iptables-persistent iptables-persistent/autosave_v6 boolean true" | debconf-set-selections
}

function install_packages() {
  DEBIAN_FRONTEND=noninteractive
  DEBIAN_PRIORITY=critical
  local arch=`dpkg --print-architecture`

  debconf_packages
  install_vhd_util

  local apt_install="apt-get --no-install-recommends -q -y --force-yes install"

  #32 bit architecture support:: not required for 32 bit template
  if [ "${arch}" != "i386" ]; then
    dpkg --add-architecture i386
    apt-get update
    ${apt_install} links:i386 libuuid1:i386
  fi

  ${apt_install} \
    rsyslog logrotate cron chkconfig insserv net-tools ifupdown vim-tiny netbase iptables \
    openssh-server e2fsprogs dhcp3-client tcpdump socat wget \
    python bzip2 sed gawk diffutils grep gzip less tar telnet ftp rsync traceroute psmisc lsof procps \
    inetutils-ping iputils-arping httping  curl \
    dnsutils zip unzip ethtool uuid file iproute acpid virt-what sudo \
    sysstat python-netaddr \
    apache2 ssl-cert \
    dnsmasq dnsmasq-utils \
    nfs-common irqbalance \
    samba-common cifs-utils \
    xl2tpd bcrelay ppp ipsec-tools tdb-tools \
    openswan=1:2.6.37-3 \
    xenstore-utils libxenstore3.0 \
    keepalived conntrackd ipvsadm libnetfilter-conntrack3 libnl1 \
    ipcalc \
    openjdk-7-jre-headless \
    iptables-persistent \
    libtcnative-1 libssl-dev libapr1-dev \
    open-vm-tools \
    python-flask \
    haproxy \
    radvd \
    sharutils

  # hold on installed openswan version, upgrade rest of the packages (if any)
  apt-mark hold openswan
  apt-get update
  apt-get -y --force-yes upgrade

  # commented out installation of vmware-tools as we are using the open source open-vm-tools:
  # ${apt_install} build-essential linux-headers-`uname -r`
  # df -h
  # PREVDIR=$PWD
  # cd /opt
  # wget http://people.apache.org/~bhaisaab/cloudstack/VMwareTools-9.2.1-818201.tar.gz
  # tar xzf VMwareTools-9.2.1-818201.tar.gz
  # rm VMwareTools-*.tar.gz
  # cd vmware-tools-distrib
  # ./vmware-install.pl -d
  # cd $PREV
  # rm -fr /opt/vmware-tools-distrib
  # apt-get -q -y --force-yes purge build-essential

  # Hyperv  kvp daemon - 64bit only
  if [ "${arch}" == "amd64" ]; then
    # Download the hv kvp daemon
    wget http://people.apache.org/~rajeshbattala/hv-kvp-daemon_3.1_amd64.deb
    dpkg -i hv-kvp-daemon_3.1_amd64.deb
  fi
}

return 2>/dev/null || install_packages
