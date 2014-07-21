#!/bin/bash

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

  # Basic packages
  ${apt_install} rsyslog logrotate cron chkconfig insserv net-tools ifupdown vim-tiny netbase iptables
  ${apt_install} openssh-server openssl e2fsprogs dhcp3-client tcpdump socat wget
  # ${apt_install} grub-legacy
  ${apt_install} python bzip2 sed gawk diffutils grep gzip less tar telnet ftp rsync traceroute psmisc lsof procps  inetutils-ping iputils-arping httping
  ${apt_install} dnsutils zip unzip ethtool uuid file iproute acpid virt-what sudo

  # sysstat
  ${apt_install} sysstat
  # apache
  ${apt_install} apache2 ssl-cert

  # dnsmasq
  ${apt_install} dnsmasq dnsmasq-utils
  # nfs client
  ${apt_install} nfs-common
  # nfs irqbalance
  ${apt_install} irqbalance

  # cifs client
  ${apt_install} samba-common
  ${apt_install} cifs-utils

  # vpn stuff
  ${apt_install} xl2tpd bcrelay ppp ipsec-tools tdb-tools
  ${apt_install} openswan=1:2.6.37-3

  # xenstore utils
  ${apt_install} xenstore-utils libxenstore3.0
  # keepalived and conntrackd for redundant router
  ${apt_install} keepalived conntrackd ipvsadm libnetfilter-conntrack3 libnl1
  # ipcalc
  ${apt_install} ipcalc
  apt-get update
  # java
  ${apt_install}  openjdk-7-jre-headless

  ${apt_install} iptables-persistent

  #libraries required for rdp client (Hyper-V)
  ${apt_install} libtcnative-1 libssl-dev libapr1-dev

  # vmware tools
  ${apt_install} open-vm-tools
  # commented installaion of vmware-tools  as we are using the opensource open-vm-tools:
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

  ${apt_install} haproxy

  # Hyperv  kvp daemon - 64bit only
  if [ "${arch}" == "amd64" ]; then
    # Download the hv kvp daemon
    wget http://people.apache.org/~rajeshbattala/hv-kvp-daemon_3.1_amd64.deb
    dpkg -i hv-kvp-daemon_3.1_amd64.deb
  fi

  #32 bit architecture support:: not required for 32 bit template
  if [ "${arch}" != "i386" ]; then
    dpkg --add-architecture i386
    apt-get update
    ${apt_install} links:i386 libuuid1:i386
  fi

  ${apt_install} radvd
}

return 2>/dev/null || install_packages
