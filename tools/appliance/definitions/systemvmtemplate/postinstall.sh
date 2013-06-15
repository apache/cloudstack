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

IMAGENAME=systemvm
LOCATION=/var/lib/images/systemvm
PASSWORD=password
HOSTNAME=systemvm
SIZE=2048
DEBIAN_MIRROR=ftp.us.debian.org/debian
MINIMIZE=true
CLOUDSTACK_RELEASE=4.1.1

init() {
    # Update the box
    apt-get -y update
    apt-get -y install linux-headers-$(uname -r) build-essential
    apt-get -y install zlib1g-dev libssl-dev libreadline-gplv2-dev
    apt-get -y install curl unzip
    apt-get clean

    # Set up sudo
    echo 'vagrant ALL=NOPASSWD:ALL' > /etc/sudoers.d/vagrant

    # Tweak sshd to prevent DNS resolution (speed up logins)
    echo 'UseDNS no' >> /etc/ssh/sshd_config

    # Remove 5s grub timeout to speed up booting
    echo <<EOF > /etc/default/grub
# If you change this file, run 'update-grub' afterwards to update
# /boot/grub/grub.cfg.

GRUB_DEFAULT=0
GRUB_TIMEOUT=0
GRUB_DISTRIBUTOR=`lsb_release -i -s 2> /dev/null || echo Debian`
GRUB_CMDLINE_LINUX_DEFAULT="quiet"
GRUB_CMDLINE_LINUX="debian-installer=en_US"
EOF

    update-grub
}

install_packages() {
  DEBIAN_FRONTEND=noninteractive
  DEBIAN_PRIORITY=critical
  DEBCONF_DB_OVERRIDE=’File{/root/config.dat}’
  export DEBIAN_FRONTEND DEBIAN_PRIORITY DEBCONF_DB_OVERRIDE

  #basic stuff
  chroot .  apt-get --no-install-recommends -q -y --force-yes install rsyslog logrotate cron chkconfig insserv net-tools ifupdown vim-tiny netbase iptables openssh-server grub-legacy e2fsprogs dhcp3-client dnsmasq tcpdump socat wget  python bzip2 sed gawk diff grep gzip less tar telnet ftp rsync traceroute psmisc lsof procps monit inetutils-ping iputils-arping httping dnsutils zip unzip ethtool uuid file iproute acpid iptables-persistent virt-what sudo
  #fix hostname in openssh-server generated keys
  sed -i "s/root@\(.*\)$/root@systemvm/g" etc/ssh/ssh_host_*.pub

  #sysstat
  chroot . echo 'sysstat sysstat/enable boolean true' | chroot . debconf-set-selections
  chroot .  apt-get --no-install-recommends -q -y --force-yes install sysstat
  #apache
  chroot .  apt-get --no-install-recommends -q -y --force-yes install apache2 ssl-cert
  #haproxy
  chroot . apt-get --no-install-recommends -q -y --force-yes install haproxy
  #dnsmasq
  chroot . apt-get --no-install-recommends -q -y --force-yes install dnsmasq
  #nfs client
  chroot . apt-get --no-install-recommends -q -y --force-yes install nfs-common
  #vpn stuff
  chroot .  apt-get --no-install-recommends -q -y --force-yes install xl2tpd openswan bcrelay ppp ipsec-tools tdb-tools
  #vmware tools
  chroot . apt-get --no-install-recommends -q -y --force-yes install open-vm-tools
  #xenstore utils
  chroot . apt-get --no-install-recommends -q -y --force-yes install xenstore-utils libxenstore3.0
  #keepalived and conntrackd
  chroot . apt-get --no-install-recommends -q -y --force-yes install keepalived conntrackd ipvsadm libnetfilter-conntrack3 libnl1
  #ipcalc
  chroot . apt-get --no-install-recommends -q -y --force-yes install ipcalc

  echo "***** getting sun jre 6*********"
  chroot . echo 'sun-java6-bin shared/accepted-sun-dlj-v1-1 boolean true
    sun-java6-jre shared/accepted-sun-dlj-v1-1 boolean true
    sun-java6-jre sun-java6-jre/stopthread boolean true
    sun-java6-jre sun-java6-jre/jcepolicy note
    sun-java6-bin shared/present-sun-dlj-v1-1 note
    sun-java6-jre shared/present-sun-dlj-v1-1 note ' | chroot . debconf-set-selections
  chroot .  apt-get --no-install-recommends -q -y install  sun-java6-jre
}

cleanup() {
    # Clean up
    apt-get -y remove linux-headers-$(uname -r) build-essential
    apt-get -y autoremove

    # Removing leftover leases and persistent rules
    echo "cleaning up dhcp leases"
    rm /var/lib/dhcp/*

    # Make sure Udev doesn't block our network
    echo "cleaning up udev rules"
    rm /etc/udev/rules.d/70-persistent-net.rules
    mkdir /etc/udev/rules.d/70-persistent-net.rules
    rm -rf /dev/.udev/
    rm /lib/udev/rules.d/75-persistent-net-generator.rules

    echo "Adding a 2 sec delay to the interface up, to make the dhclient happy"
    echo "pre-up sleep 2" >> /etc/network/interfaces
}

finalize() {
    # Zero out the free space to save space in the final image:
    dd if=/dev/zero of=/EMPTY bs=1M
    rm -f /EMPTY
}


echo "*************STARTING POSTINST SCRIPT********************"
begin=$(date +%s)

echo "*************INITIALIZING BASE SYSTEM********************"
init

echo "*************INSTALLING PACKAGES********************"
install_packages

echo "*************CLEANING UP********************"
cleanup

echo "*************FINALIZING IMAGE********************"
finalize

fin=$(date +%s)
t=$((fin-begin))

echo "Finished building systemvm appliance in $t seconds"
