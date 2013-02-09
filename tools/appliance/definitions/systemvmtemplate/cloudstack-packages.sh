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
HOSTNAME=systemvm
CLOUDSTACK_RELEASE=4.2.0


install_packages() {
  DEBIAN_FRONTEND=noninteractive
  DEBIAN_PRIORITY=critical

  #basic stuff
   apt-get --no-install-recommends -q -y --force-yes install rsyslog logrotate cron chkconfig insserv net-tools ifupdown vim-tiny netbase iptables 
   apt-get --no-install-recommends -q -y --force-yes install openssh-server grub-legacy e2fsprogs dhcp3-client dnsmasq tcpdump socat wget  
   apt-get --no-install-recommends -q -y --force-yes install python bzip2 sed gawk diffutils grep gzip less tar telnet ftp rsync traceroute psmisc lsof procps monit inetutils-ping iputils-arping httping 
   apt-get --no-install-recommends -q -y --force-yes install dnsutils zip unzip ethtool uuid file iproute acpid virt-what sudo 

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
  apt-get --no-install-recommends -q -y --force-yes install openswan

  #vmware tools
  apt-get --no-install-recommends -q -y --force-yes install open-vm-tools
  #xenstore utils
  apt-get --no-install-recommends -q -y --force-yes install xenstore-utils libxenstore3.0
  #keepalived and conntrackd for redundant router
  apt-get --no-install-recommends -q -y --force-yes install keepalived conntrackd ipvsadm libnetfilter-conntrack3 libnl1
  #ipcalc
  apt-get --no-install-recommends -q -y --force-yes install ipcalc
  #java
  apt-get --no-install-recommends -q -y --force-yes install  default-jre-headless

  echo "iptables-persistent iptables-persistent/autosave_v4 boolean true" | debconf-set-selections
  echo "iptables-persistent iptables-persistent/autosave_v6 boolean true" | debconf-set-selections
  apt-get --no-install-recommends -q -y --force-yes install iptables-persistent
}

accounts() {
  # Setup sudo to allow no-password sudo for "admin"
  groupadd -r admin
  #create a 'cloud' user
  useradd -G admin cloud
  echo "root:$PASSWORD" | chpasswd
  #FIXME: create random password for cloud
  #FIXME: disable password auth in sshd (final step, after veewee is done) 
  #echo "cloud:password" | chpasswd
  sed -i -e '/Defaults\s\+env_reset/a Defaults\texempt_group=admin' /etc/sudoers
  sed -i -e 's/%admin ALL=(ALL) ALL/%admin ALL=NOPASSWD:ALL/g' /etc/sudoers
  
  mkdir -p /home/cloud/.ssh
  chmod 700 /home/cloud/.ssh

}

fix_nameserver() {
  #replace /etc/resolv.conf also
  cat > /etc/resolv.conf << EOF
nameserver 8.8.8.8
nameserver 4.4.4.4
EOF

}

do_fixes() {
  #fix hostname in openssh-server generated keys
  sed -i "s/root@\(.*\)$/root@$HOSTNAME/g" /etc/ssh/ssh_host_*.pub
  #fix hostname to override one provided by dhcp during vm build
  echo "$HOSTNAME" > /etc/hostname
  hostname $HOSTNAME
  #delete entry in /etc/hosts derived from dhcp
  sed -i '/127.0.1.1/d' /etc/hosts 

  #fix_nameserver FIXME needed after veewee finishes
}

configure_apache2() {
   #enable ssl, rewrite and auth
   a2enmod ssl rewrite auth_basic auth_digest
   a2ensite default-ssl
   #backup stock apache configuration since we may modify it in Secondary Storage VM
   cp /etc/apache2/sites-available/default /etc/apache2/sites-available/default.orig
   cp /etc/apache2/sites-available/default-ssl /etc/apache2/sites-available/default-ssl.orig
}

services() {
  mkdir -p /var/www/html
  mkdir -p /opt/cloud/bin
  mkdir -p /var/cache/cloud
  mkdir -p /usr/share/cloud
  mkdir -p /usr/local/cloud
  mkdir -p /root/.ssh
  #Fix haproxy directory issue
  mkdir -p /var/lib/haproxy
  
  #FIXME: need a way to copy from git repo (perhaps wget from git-wip-us.apache.org?)
  #/bin/cp -r ${scriptdir}/config/* ./
  chkconfig xl2tpd off
  #chkconfig --add cloud-early-config
  #chkconfig cloud-early-config on
  #chkconfig --add cloud-passwd-srvr 
  #chkconfig cloud-passwd-srvr off
  #chkconfig --add cloud
  #chkconfig cloud off
  chkconfig monit off
}

signature() {
  mkdir -p /var/cache/cloud/
  touch /var/cache/cloud/cloud-scripts-signature
  #FIXME: signature should be generated from scripts package that can get updated
  echo "Cloudstack Release $CLOUDSTACK_RELEASE $(date)" > /etc/cloudstack-release
}

echo "*************INSTALLING PACKAGES********************"
install_packages
echo "*************DONE INSTALLING PACKAGES********************"
accounts
do_fixes
configure_apache2
services
signature
