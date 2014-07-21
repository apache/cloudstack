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

install_packages
