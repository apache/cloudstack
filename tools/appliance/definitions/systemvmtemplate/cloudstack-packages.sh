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


ROOTPW=password
CLOUDSTACK_RELEASE=4.2.0


install_packages() {
  DEBIAN_FRONTEND=noninteractive
  DEBIAN_PRIORITY=critical

  #basic stuff
   apt-get --no-install-recommends -q -y --force-yes install rsyslog logrotate cron chkconfig insserv net-tools ifupdown vim-tiny netbase iptables openssh-server grub-legacy e2fsprogs dhcp3-client dnsmasq tcpdump socat wget  python bzip2 sed gawk diff grep gzip less tar telnet ftp rsync traceroute psmisc lsof procps monit inetutils-ping iputils-arping httping dnsutils zip unzip ethtool uuid file iproute acpid iptables-persistent virt-what sudo

  #sysstat
  echo 'sysstat sysstat/enable boolean true' | debconf-set-selections
  apt-get --no-install-recommends -q -y --force-yes install sysstat
  #apache
  apt-get --no-install-recommends -q -y --force-yes install apache2 ssl-cert
  #haproxy
  apt-get --no-install-recommends -q -y --force-yes install haproxy
  #dnsmasq
  apt-get --no-install-recommends -q -y --force-yes install dnsmasq
  #nfs client
  apt-get --no-install-recommends -q -y --force-yes install nfs-common
  #vpn stuff
  apt-get --no-install-recommends -q -y --force-yes install xl2tpd bcrelay ppp ipsec-tools tdb-tools
  echo "openswan openswan/install_x509_certificate boolean false" | debconf-set-selections
  echo "openswan openswan/install_x509_certificate seen true" | debconf-set-selections
  chroot .  apt-get --no-install-recommends -q -y --force-yes install openswan
  #vmware tools
  apt-get --no-install-recommends -q -y --force-yes install open-vm-tools
  #xenstore utils
  apt-get --no-install-recommends -q -y --force-yes install xenstore-utils libxenstore3.0
  #keepalived and conntrackd
  apt-get --no-install-recommends -q -y --force-yes install keepalived conntrackd ipvsadm libnetfilter-conntrack3 libnl1
  #ipcalc
  apt-get --no-install-recommends -q -y --force-yes install ipcalc
  #java
  apt-get --no-install-recommends -q -y --force-yes install  default-jre-headless

}

accounts() {
  # Setup sudo to allow no-password sudo for "admin"
  groupadd -r admin
  #create a 'cloud' user
  useradd -G admin cloud
  echo "root:password" | chpasswd
  echo "cloud:password" | chpasswd
  sed -i -e '/Defaults\s\+env_reset/a Defaults\texempt_group=admin' /etc/sudoers
  sed -i -e 's/%admin ALL=(ALL) ALL/%admin ALL=NOPASSWD:ALL/g' /etc/sudoers
  
  mkdir -p /home/cloud/.ssh
  chmod 700 /home/cloud/.ssh

}

do_fixes() {
  #fix hostname in openssh-server generated keys
  sed -i "s/root@\(.*\)$/root@systemvm/g" /etc/ssh/ssh_host_*.pub
}

signature() {
  mkdir -p /var/cache/cloud/
  touch /var/cache/cloud/cloud-scripts-signature
  echo "Cloudstack Release $CLOUDSTACK_RELEASE $(date)" > /etc/cloudstack-release
}

echo "*************INSTALLING PACKAGES********************"
install_packages
echo "*************DONE INSTALLING PACKAGES********************"
accounts
do_fixes
signature
