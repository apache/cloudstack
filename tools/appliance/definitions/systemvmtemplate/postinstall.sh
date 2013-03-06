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

set -x

ROOTPW=password
HOSTNAME=systemvm
CLOUDSTACK_RELEASE=4.2.0

install_packages() {
  DEBIAN_FRONTEND=noninteractive
  DEBIAN_PRIORITY=critical

  # Basic packages
  apt-get --no-install-recommends -q -y --force-yes install rsyslog logrotate cron chkconfig insserv net-tools ifupdown vim-tiny netbase iptables
  apt-get --no-install-recommends -q -y --force-yes install openssh-server openssl e2fsprogs dhcp3-client tcpdump socat wget
  # apt-get --no-install-recommends -q -y --force-yes install grub-legacy
  apt-get --no-install-recommends -q -y --force-yes install python bzip2 sed gawk diffutils grep gzip less tar telnet ftp rsync traceroute psmisc lsof procps monit inetutils-ping iputils-arping httping
  apt-get --no-install-recommends -q -y --force-yes install dnsutils zip unzip ethtool uuid file iproute acpid virt-what sudo

  # sysstat
  echo 'sysstat sysstat/enable boolean true' | debconf-set-selections
  apt-get --no-install-recommends -q -y --force-yes install sysstat
  # apache
  apt-get --no-install-recommends -q -y --force-yes install apache2 ssl-cert
  # haproxy
  apt-get --no-install-recommends -q -y --force-yes install haproxy
  # dnsmasq
  apt-get --no-install-recommends -q -y --force-yes install dnsmasq
  # nfs client
  apt-get --no-install-recommends -q -y --force-yes install nfs-common

  # vpn stuff
  apt-get --no-install-recommends -q -y --force-yes install xl2tpd bcrelay ppp ipsec-tools tdb-tools
  echo "openswan openswan/install_x509_certificate boolean false" | debconf-set-selections
  echo "openswan openswan/install_x509_certificate seen true" | debconf-set-selections
  apt-get --no-install-recommends -q -y --force-yes install openswan

  # vmware tools
  apt-get --no-install-recommends -q -y --force-yes install open-vm-tools
  # xenstore utils
  apt-get --no-install-recommends -q -y --force-yes install xenstore-utils libxenstore3.0
  # keepalived and conntrackd for redundant router
  apt-get --no-install-recommends -q -y --force-yes install keepalived conntrackd ipvsadm libnetfilter-conntrack3 libnl1
  # ipcalc
  apt-get --no-install-recommends -q -y --force-yes install ipcalc
  # java
  apt-get --no-install-recommends -q -y --force-yes install  default-jre-headless

  echo "iptables-persistent iptables-persistent/autosave_v4 boolean true" | debconf-set-selections
  echo "iptables-persistent iptables-persistent/autosave_v6 boolean true" | debconf-set-selections
  apt-get --no-install-recommends -q -y --force-yes install iptables-persistent
}

setup_accounts() {
  # Setup sudo to allow no-password sudo for "admin"
  groupadd -r admin
  # Create a 'cloud' user if it's not there
  id cloud
  if [[ $? -ne 0 ]]
  then
    useradd -G admin cloud
  else
    usermod -a -G admin cloud
  fi
  echo "root:$ROOTPW" | chpasswd
  echo "cloud:`openssl rand -base64 32`" | chpasswd
  sed -i -e '/Defaults\s\+env_reset/a Defaults\texempt_group=admin' /etc/sudoers
  sed -i -e 's/%admin ALL=(ALL) ALL/%admin ALL=NOPASSWD:ALL/g' /etc/sudoers
  # Disable password based authentication via ssh, this will take effect on next reboot
  sed -i -e 's/^.*PasswordAuthentication .*$/PasswordAuthentication no/g' /etc/ssh/sshd_config
  # Secure ~/.ssh
  mkdir -p /home/cloud/.ssh
  chmod 700 /home/cloud/.ssh
}

fix_nameserver() {
  # Replace /etc/resolv.conf also
  cat > /etc/resolv.conf << EOF
nameserver 8.8.8.8
nameserver 4.4.4.4
EOF
}

fix_inittab() {
  # Fix inittab
  cat >> /etc/inittab << EOF

vc:2345:respawn:/sbin/getty 38400 hvc0
EOF
}

fix_acpid() {
  # Fix acpid
  mkdir -p /etc/acpi/events
  cat >> /etc/acpi/events/power << EOF
event=button/power.*
action=/usr/local/sbin/power.sh "%e"
EOF
  cat >> /usr/local/sbin/power.sh << EOF
#!/bin/bash
/sbin/poweroff
EOF
  chmod a+x /usr/local/sbin/power.sh
}

fix_hostname() {
  # Fix hostname in openssh-server generated keys
  sed -i "s/root@\(.*\)$/root@$HOSTNAME/g" /etc/ssh/ssh_host_*.pub
  # Fix hostname to override one provided by dhcp during vm build
  echo "$HOSTNAME" > /etc/hostname
  hostname $HOSTNAME
  # Delete entry in /etc/hosts derived from dhcp
  sed -i '/127.0.1.1/d' /etc/hosts
}

fix_locale() {
  cat >> /etc/default/locale  << EOF
LANG=en_US.UTF-8
LC_ALL=en_US.UTF-8
EOF
  cat >> /etc/locale.gen  << EOF
en_US.UTF-8 UTF-8
EOF

  locale-gen en_US.UTF-8
}

do_fixes() {
  fix_nameserver
  fix_inittab
  fix_acpid
  fix_hostname
  fix_locale
}

configure_apache2() {
   # Enable ssl, rewrite and auth
   a2enmod ssl rewrite auth_basic auth_digest
   a2ensite default-ssl
   # Backup stock apache configuration since we may modify it in Secondary Storage VM
   cp /etc/apache2/sites-available/default /etc/apache2/sites-available/default.orig
   cp /etc/apache2/sites-available/default-ssl /etc/apache2/sites-available/default-ssl.orig
}

configure_services() {
  mkdir -p /var/www/html
  mkdir -p /opt/cloud/bin
  mkdir -p /var/cache/cloud
  mkdir -p /usr/share/cloud
  mkdir -p /usr/local/cloud
  mkdir -p /root/.ssh
  # Fix haproxy directory issue
  mkdir -p /var/lib/haproxy

  # Get config files from master
  snapshot_url="https://git-wip-us.apache.org/repos/asf?p=incubator-cloudstack.git;a=snapshot;h=HEAD;sf=tgz"
  snapshot_dir="/opt/incubator-cloudstack*"
  cd /opt
  wget $snapshot_url -O cloudstack.tar.gz
  tar -zxvf cloudstack.tar.gz
  cp -rv $snapshot_dir/patches/systemvm/debian/config/* /
  cp -rv $snapshot_dir/patches/systemvm/debian/vpn/* /
  mkdir -p /usr/share/cloud/
  cd $snapshot_dir/patches/systemvm/debian/config
  tar -cvf /usr/share/cloud/cloud-scripts.tar *
  cd $snapshot_dir/patches/systemvm/debian/vpn
  tar -rvf /usr/share/cloud/cloud-scripts.tar *
  cd /opt
  rm -fr $snapshot_dir cloudstack.tar.gz

  chkconfig --add cloud-early-config
  chkconfig cloud-early-config on
  chkconfig --add cloud-passwd-srvr
  chkconfig cloud-passwd-srvr off
  chkconfig --add cloud
  chkconfig cloud off
  chkconfig monit off
  chkconfig xl2tpd off
}

do_signature() {
  mkdir -p /var/cache/cloud/
  gzip -c /usr/share/cloud/cloud-scripts.tar > /usr/share/cloud/cloud-scripts.tgz
  md5sum /usr/share/cloud/cloud-scripts.tgz | awk '{print $1}' > /var/cache/cloud/cloud-scripts-signature
  echo "Cloudstack Release $CLOUDSTACK_RELEASE $(date)" > /etc/cloudstack-release
}

begin=$(date +%s)

echo "*************INSTALLING PACKAGES********************"
install_packages
echo "*************DONE INSTALLING PACKAGES********************"
setup_accounts
echo "*************DONE ACCOUNT SETUP********************"
configure_services
configure_apache2
echo "*************DONE SETTING UP SERVICES********************"
do_fixes
echo "*************DONE FIXING CONFIGURATION********************"
do_signature

fin=$(date +%s)
t=$((fin-begin))

echo "Signed systemvm build, finished building systemvm appliance in $t seconds"
