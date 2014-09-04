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
CLOUDSTACK_RELEASE=4.4.0

install_packages() {
  DEBIAN_FRONTEND=noninteractive
  DEBIAN_PRIORITY=critical
  local arch=`dpkg --print-architecture`

  # Basic packages
  apt-get --no-install-recommends -q -y --force-yes install rsyslog logrotate cron chkconfig insserv net-tools ifupdown vim-tiny netbase iptables
  apt-get --no-install-recommends -q -y --force-yes install openssh-server openssl e2fsprogs dhcp3-client tcpdump socat wget
  # apt-get --no-install-recommends -q -y --force-yes install grub-legacy
  apt-get --no-install-recommends -q -y --force-yes install python bzip2 sed gawk diffutils grep gzip less tar telnet ftp rsync traceroute psmisc lsof procps  inetutils-ping iputils-arping httping
  apt-get --no-install-recommends -q -y --force-yes install dnsutils zip unzip ethtool uuid file iproute acpid virt-what sudo

  # sysstat
  echo 'sysstat sysstat/enable boolean true' | debconf-set-selections
  apt-get --no-install-recommends -q -y --force-yes install sysstat
  # apache
  apt-get --no-install-recommends -q -y --force-yes install apache2 ssl-cert

  # dnsmasq
  apt-get --no-install-recommends -q -y --force-yes install dnsmasq dnsmasq-utils
  # nfs client
  apt-get --no-install-recommends -q -y --force-yes install nfs-common
  # nfs irqbalance
  apt-get --no-install-recommends -q -y --force-yes install irqbalance

  # cifs client
  apt-get --no-install-recommends -q -y --force-yes install samba-common
  apt-get --no-install-recommends -q -y --force-yes install cifs-utils

  # vpn stuff
  apt-get --no-install-recommends -q -y --force-yes install xl2tpd bcrelay ppp ipsec-tools tdb-tools
  echo "openswan openswan/install_x509_certificate boolean false" | debconf-set-selections
  echo "openswan openswan/install_x509_certificate seen true" | debconf-set-selections
  apt-get --no-install-recommends -q -y --force-yes install openswan=1:2.6.37-3

  # xenstore utils
  apt-get --no-install-recommends -q -y --force-yes install xenstore-utils libxenstore3.0
  # keepalived and conntrackd for redundant router
  apt-get --no-install-recommends -q -y --force-yes install keepalived conntrackd ipvsadm libnetfilter-conntrack3 libnl1
  # ipcalc
  apt-get --no-install-recommends -q -y --force-yes install ipcalc
  apt-get update
  # java
  apt-get --no-install-recommends -q -y --force-yes install  openjdk-7-jre-headless

  echo "iptables-persistent iptables-persistent/autosave_v4 boolean true" | debconf-set-selections
  echo "iptables-persistent iptables-persistent/autosave_v6 boolean true" | debconf-set-selections
  apt-get --no-install-recommends -q -y --force-yes install iptables-persistent
  
  # Hyperv  kvp daemon - 64bit only
  if [ "${arch}" == "amd64" ]; then
    # Download the hv kvp daemon
    wget http://people.apache.org/~rajeshbattala/hv-kvp-daemon_3.1_amd64.deb
    dpkg -i hv-kvp-daemon_3.1_amd64.deb
  fi

  #libraries required for rdp client (Hyper-V)
  apt-get --no-install-recommends -q -y --force-yes install libtcnative-1 libssl-dev libapr1-dev

  # vmware tools
  apt-get --no-install-recommends -q -y --force-yes install open-vm-tools
  # commented installaion of vmware-tools  as we are using the opensource open-vm-tools:
  # apt-get --no-install-recommends -q -y --force-yes install build-essential linux-headers-`uname -r`
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

  apt-get --no-install-recommends -q -y --force-yes install haproxy

  #32 bit architecture support:: not required for 32 bit template
  if [ "${arch}" != "i386" ]; then
    dpkg --add-architecture i386
    apt-get update
    apt-get --no-install-recommends -q -y --force-yes install links:i386 libuuid1:i386
  fi

  apt-get --no-install-recommends -q -y --force-yes install radvd
}

fix_nameserver() {
  # Replace /etc/resolv.conf also
  cat > /etc/resolv.conf << EOF
nameserver 8.8.8.8
nameserver 8.8.4.4
EOF
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

fix_vhdutil() {
  wget --no-check-certificate http://download.cloud.com.s3.amazonaws.com/tools/vhd-util -O /bin/vhd-util
  chmod a+x /bin/vhd-util
}

do_fixes() {
  fix_nameserver
  fix_hostname
  fix_vhdutil
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
  local arch=`dpkg --print-architecture`
  mkdir -p /var/www/html
  mkdir -p /opt/cloud/bin
  mkdir -p /var/cache/cloud
  mkdir -p /usr/share/cloud
  mkdir -p /usr/local/cloud
  mkdir -p /root/.ssh
  # Fix haproxy directory issue
  mkdir -p /var/lib/haproxy

  # Get config files from master
  snapshot_url="https://git-wip-us.apache.org/repos/asf?p=cloudstack.git;a=snapshot;h=HEAD;sf=tgz"
  snapshot_dir="/opt/cloudstack*"
  cd /opt
  wget --no-check-certificate $snapshot_url -O cloudstack.tar.gz
  tar -zxvf cloudstack.tar.gz --wildcards 'cloudstack-HEAD-???????/systemvm'
  cp -rv $snapshot_dir/systemvm/patches/debian/config/* /
  cp -rv $snapshot_dir/systemvm/patches/debian/vpn/* /
  mkdir -p /usr/share/cloud/
  cd $snapshot_dir/systemvm/patches/debian/config
  tar -cvf /usr/share/cloud/cloud-scripts.tar *
  cd $snapshot_dir/systemvm/patches/debian/vpn
  tar -rvf /usr/share/cloud/cloud-scripts.tar *
  cd /opt
  rm -fr $snapshot_dir cloudstack.tar.gz

  chkconfig --add cloud-early-config
  chkconfig cloud-early-config on
  chkconfig --add cloud-passwd-srvr
  chkconfig cloud-passwd-srvr off
  chkconfig --add cloud
  chkconfig cloud off
  chkconfig xl2tpd off
  # Hyperv  kvp daemon - 64bit only
  if [ "${arch}" == "amd64" ]; then
    chkconfig hv_kvp_daemon off
  fi
  chkconfig radvd off
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
configure_services
configure_apache2
echo "*************DONE SETTING UP SERVICES********************"
do_fixes
echo "*************DONE FIXING CONFIGURATION********************"
do_signature

fin=$(date +%s)
t=$((fin-begin))

echo "Signed systemvm build, finished building systemvm appliance in $t seconds"
