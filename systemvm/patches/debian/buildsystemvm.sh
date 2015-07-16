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

echo "####################################################"
echo " Note there is a new systemvm build script based on "
echo " Veewee(Vagrant) under tools/appliance."
echo "####################################################"

set -e
set -x

IMAGENAME=systemvm
LOCATION=/var/lib/images/systemvm
PASSWORD=password
#APT_PROXY=192.168.1.115:3142/
APT_PROXY=
HOSTNAME=systemvm
SIZE=2000
DEBIAN_MIRROR=ftp.us.debian.org/debian
MINIMIZE=true
CLOUDSTACK_RELEASE=4.0
offset=4096
baseimage() {
  mkdir -p $LOCATION
  #dd if=/dev/zero of=$IMAGELOC bs=1M  count=$SIZE
  dd if=/dev/zero of=$IMAGELOC bs=1M seek=$((SIZE - 1)) count=1
  loopdev=$(losetup -f)
  losetup $loopdev $IMAGELOC
  parted $loopdev -s 'mklabel msdos'
  parted $loopdev -s 'mkpart primary ext3 4096B -1'
  sleep 2 
  losetup -d $loopdev
  loopdev=$(losetup --show -o $offset -f $IMAGELOC )
  mkfs.ext3  -L ROOT $loopdev
  mkdir -p $MOUNTPOINT
  tune2fs -c 100 -i 0 $loopdev
  sleep 2 
  losetup -d $loopdev
  
  mount -o loop,offset=$offset $IMAGELOC  $MOUNTPOINT
  
  #debootstrap --variant=minbase --keyring=/usr/share/keyrings/debian-archive-keyring.gpg wheezy $MOUNTPOINT http://${APT_PROXY}${DEBIAN_MIRROR}
  debootstrap --variant=minbase --arch=i386 wheezy $MOUNTPOINT http://${APT_PROXY}${DEBIAN_MIRROR}
}


fixapt() {
  if [ "$APT_PROXY" != "" ]; then
  cat >> etc/apt/apt.conf.d/01proxy << EOF
Acquire::http::Proxy "http://${APT_PROXY}";
EOF
  fi

  cat > etc/apt/sources.list << EOF
deb http://http.debian.net/debian/ wheezy main contrib non-free
deb-src http://http.debian.net/debian/ wheezy main contrib non-free

deb http://security.debian.org/ wheezy/updates main
deb-src http://security.debian.org/ wheezy/updates main

deb http://http.debian.net/debian/ wheezy-backports main
deb-src http://http.debian.net/debian/ wheezy-backports main
EOF

  cat >> etc/apt/apt.conf << EOF
APT::Default-Release "stable"; 
EOF

  cat >> etc/apt/preferences << EOF
Package: *
Pin: release o=Debian,a=stable
Pin-Priority: 900
EOF

  #apt-key exportall | chroot . apt-key add - &&
  chroot . apt-get update &&
  echo "Apt::Install-Recommends 0;" > etc/apt/apt.conf.d/local-recommends

  cat >> usr/sbin/policy-rc.d  << EOF
#!/bin/sh
exit 101
EOF
  chmod a+x usr/sbin/policy-rc.d

  cat >> etc/default/locale  << EOF
LANG=en_US.UTF-8
LC_ALL=en_US.UTF-8
EOF

  cat >> etc/locale.gen  << EOF
en_US.UTF-8 UTF-8
EOF

  DEBIAN_FRONTEND=noninteractive
  DEBIAN_PRIORITY=critical
  export DEBIAN_FRONTEND DEBIAN_PRIORITY 
  chroot . dpkg-reconfigure debconf --frontend=noninteractive
  chroot . apt-get -q -y install locales
}

network() {

  echo "$HOSTNAME" > etc/hostname &&
  cat > etc/hosts << EOF 
127.0.0.1       localhost
# The following lines are desirable for IPv6 capable hosts
::1     localhost ip6-localhost ip6-loopback
fe00::0 ip6-localnet
ff00::0 ip6-mcastprefix
ff02::1 ip6-allnodes
ff02::2 ip6-allrouters
ff02::3 ip6-allhosts
EOF

  cat >> etc/network/interfaces << EOF
auto lo eth0
iface lo inet loopback

# The primary network interface
iface eth0 inet static

EOF
}

install_kernel() {
  DEBIAN_FRONTEND=noninteractive
  DEBIAN_PRIORITY=critical
  export DEBIAN_FRONTEND DEBIAN_PRIORITY

  chroot . apt-get -qq -y --force-yes install grub-legacy &&
  cp -av usr/lib/grub/i386-pc boot/grub
  #for some reason apt-get install grub does not install grub/stage1 etc
  #loopd=$(losetup -f --show $1)
  #grub-install $loopd --root-directory=$MOUNTPOINT
  #losetup -d $loopd
  grub  << EOF &&
device (hd0) $1
root (hd0,0)
setup (hd0)
quit
EOF
   # install a kernel image
   cat > etc/kernel-img.conf << EOF &&
do_symlinks = yes
link_in_boot = yes
do_initrd = yes
EOF
  touch /mnt/systemvm/boot/grub/default
  chroot . apt-get install -qq -y --force-yes linux-image-686-bigmem
  cat >> etc/kernel-img.conf << EOF
postinst_hook = /usr/sbin/update-grub
postrm_hook   = /usr/sbin/update-grub
EOF
}


fixgrub() {
  kern=$(basename $(ls  boot/vmlinuz-*))
  ver=${kern#vmlinuz-}
  cat > boot/grub/menu.lst << EOF
default 0
timeout 2
color cyan/blue white/blue

### BEGIN AUTOMAGIC KERNELS LIST
# kopt=root=LABEL=ROOT ro

## ## End Default Options ##
title		Debian GNU/Linux, kernel $ver
root		(hd0,0)
kernel		/boot/$kern root=LABEL=ROOT ro console=tty0 xencons=ttyS0,115200 console=hvc0 quiet
initrd		/boot/initrd.img-$ver

### END DEBIAN AUTOMAGIC KERNELS LIST
EOF
  (cd boot/grub; ln -s menu.lst grub.conf)
}

fixinittab() {
  cat >> etc/inittab << EOF

vc:2345:respawn:/sbin/getty 38400 hvc0
EOF
}

fixfstab() {
  cat > etc/fstab << EOF
# <file system> <mount point>   <type>  <options>       <dump>  <pass>
proc            /proc           proc    defaults        0       0
LABEL=ROOT      /               ext3    errors=remount-ro,sync,noatime 0       1
EOF
}

fixacpid() {
  mkdir -p etc/acpi/events
  cat >> etc/acpi/events/power << EOF
event=button/power.*
action=/usr/local/sbin/power.sh "%e"
EOF
  cat >> usr/local/sbin/power.sh << EOF
#!/bin/bash
/sbin/poweroff
EOF
  chmod a+x usr/local/sbin/power.sh
}

fixiptables() {
cat >> etc/modules << EOF
nf_conntrack
nf_conntrack_ipv4
EOF
cat > etc/init.d/iptables-persistent << EOF
#!/bin/sh
### BEGIN INIT INFO
# Provides:          iptables
# Required-Start:    mountkernfs $local_fs
# Required-Stop:     $local_fs
# Should-Start:      cloud-early-config
# Default-Start:     S
# Default-Stop:     
# Short-Description: Set up iptables rules
### END INIT INFO

PATH="/sbin:/bin:/usr/sbin:/usr/bin"

# Include config file for iptables-persistent
. /etc/iptables/iptables.conf

case "\$1" in
start)
    if [ -e /var/run/iptables ]; then
        echo "iptables is already started!"
        exit 1
    else
        touch /var/run/iptables
    fi

    if [ \$ENABLE_ROUTING -ne 0 ]; then
        # Enable Routing
        echo 1 > /proc/sys/net/ipv4/ip_forward
    fi

    # Load Modules
    modprobe -a \$MODULES

    # Load saved rules
    if [ -f /etc/iptables/rules ]; then
        iptables-restore </etc/iptables/rules
    fi
    ;;
stop|force-stop)
    if [ ! -e /var/run/iptables ]; then
        echo "iptables is already stopped!"
        exit 1
    else
        rm /var/run/iptables
    fi

    if [ \$SAVE_NEW_RULES -ne 0 ]; then
        # Backup old rules
        cp /etc/iptables/rules /etc/iptables/rules.bak
        # Save new rules
        iptables-save >/etc/iptables/rules
    fi

    # Restore Default Policies
    iptables -P INPUT ACCEPT
    iptables -P FORWARD ACCEPT
    iptables -P OUTPUT ACCEPT

    # Flush rules on default tables
    iptables -F
    iptables -t nat -F
    iptables -t mangle -F

    # Unload previously loaded modules
    modprobe -r \$MODULES

    # Disable Routing if enabled
    if [ \$ENABLE_ROUTING -ne 0 ]; then
        # Disable Routing
        echo 0 > /proc/sys/net/ipv4/ip_forward
    fi

    ;;
restart|force-reload)
    \$0 stop
    \$0 start
    ;;
status)
    echo "Filter Rules:"
    echo "--------------"
    iptables -L -v
    echo ""
    echo "NAT Rules:"
    echo "-------------"
    iptables -t nat -L -v
    echo ""
    echo "Mangle Rules:"
    echo "----------------"
    iptables -t mangle -L -v
    ;;
*)
    echo "Usage: \$0 {start|stop|force-stop|restart|force-reload|status}" >&2
    exit 1
    ;;
esac

exit 0
EOF
  chmod a+x etc/init.d/iptables-persistent


  touch etc/iptables/iptables.conf 
  cat > etc/iptables/iptables.conf << EOF
# A basic config file for the /etc/init.d/iptable-persistent script

# Should new manually added rules from command line be saved on reboot? Assign to a value different that 0 if you want this enabled.
SAVE_NEW_RULES=0

# Modules to load:
MODULES="nf_nat_ftp nf_conntrack_ftp"

# Enable Routing?
ENABLE_ROUTING=1
EOF
  chmod a+x etc/iptables/iptables.conf

}

vpn_config() {
  cp -r ${scriptdir}/vpn/* ./
}

#
# IMPORTANT REMARK
# Package intallation is no longer done via this script. We are not removing the code yet, but we want to 
# make sure that everybody willing to install/update packages should refer to the file:
#   ==> cloud-tools/appliance/definitions/systemvmtemplate/install_systemvm_packages.sh
#
packages() {
  DEBIAN_FRONTEND=noninteractive
  DEBIAN_PRIORITY=critical
  DEBCONF_DB_OVERRIDE=’File{/root/config.dat}’
  export DEBIAN_FRONTEND DEBIAN_PRIORITY DEBCONF_DB_OVERRIDE

  #basic stuff
  chroot .  apt-get --no-install-recommends -q -y --force-yes install rsyslog logrotate cron chkconfig insserv net-tools ifupdown vim-tiny netbase iptables openssh-server grub-legacy e2fsprogs dhcp3-client dnsmasq tcpdump socat wget  python bzip2 sed gawk diffutils grep gzip less tar telnet ftp rsync traceroute psmisc lsof procps monit inetutils-ping iputils-arping httping dnsutils zip unzip ethtool uuid file iproute acpid iptables-persistent virt-what sudo
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
  #keepalived - install version 1.2.13 from wheezy backports
  chroot . apt-get --no-install-recommends -q -y --force-yes -t wheezy-backports install keepalived
  #conntrackd
  chroot . apt-get --no-install-recommends -q -y --force-yes install conntrackd ipvsadm libnetfilter-conntrack3 libnl1
  #ipcalc
  chroot . apt-get --no-install-recommends -q -y --force-yes install ipcalc
  #irqbalance from wheezy-backports
  chroot . apt-get --no-install-recommends -q -y --force-yes -t wheezy-backports install irqbalance

  echo "***** getting jre 7 *********"
  chroot .  apt-get --no-install-recommends -q -y install openjdk-7-jre-headless
}


password() {
  chroot . echo "root:$PASSWORD" | chroot . chpasswd
}

apache2() {
   chroot . a2enmod ssl rewrite auth_basic auth_digest
   chroot . a2ensite default-ssl
   cp etc/apache2/sites-available/default etc/apache2/sites-available/default.orig
   cp etc/apache2/sites-available/default-ssl etc/apache2/sites-available/default-ssl.orig
}

services() {
  mkdir -p ./var/www/html
  mkdir -p ./opt/cloud/bin
  mkdir -p ./var/cache/cloud
  mkdir -p ./usr/share/cloud
  mkdir -p ./usr/local/cloud
  mkdir -p ./root/.ssh
  #Fix haproxy directory issue
  mkdir -p ./var/lib/haproxy
  
  /bin/cp -r ${scriptdir}/config/* ./
  chroot . chkconfig xl2tpd off
  chroot . chkconfig --add cloud-early-config
  chroot . chkconfig cloud-early-config on
  chroot . chkconfig --add iptables-persistent
  chroot . chkconfig iptables-persistent off
  chroot . chkconfig --force --add cloud-passwd-srvr
  chroot . chkconfig cloud-passwd-srvr off
  chroot . chkconfig --add cloud
  chroot . chkconfig cloud off
  chroot . chkconfig monit off
}

dhcp_fix() {
  #deal with virtio DHCP issue, copy and install customized kernel module and iptables
  mkdir -p tmp
  cp /tmp/systemvm/xt_CHECKSUM.ko lib/modules/2.6.32-5-686-bigmem/kernel/net/netfilter
  chroot . depmod -a 2.6.32-5-686-bigmem
  cp /tmp/systemvm/iptables_1.4.8-3local1checksum1_i386.deb tmp/
  chroot . dpkg -i tmp/iptables_1.4.8-3local1checksum1_i386.deb
  rm tmp/iptables_1.4.8-3local1checksum1_i386.deb
}

install_xs_tool() {
  #deal with virtio DHCP issue, copy and install customized kernel module and iptables
  mkdir -p tmp
  cp /tmp/systemvm/xe-guest-utilities_5.6.0-595_i386.deb tmp/
  chroot . dpkg -i tmp/xe-guest-utilities_5.6.0-595_i386.deb
  rm tmp/xe-guest-utilities_5.6.0-595_i386.deb
}

cleanup() {
  rm -f usr/sbin/policy-rc.d
  rm -f root/config.dat
  rm -f etc/apt/apt.conf.d/01proxy 

  if [ "$MINIMIZE" == "true" ]
  then
    rm -rf var/cache/apt/*
    rm -rf var/lib/apt/*
    rm -rf usr/share/locale/[a-d]*
    rm -rf usr/share/locale/[f-z]*
    rm -rf usr/share/doc/*
    size=$(df   $MOUNTPOINT | awk '{print $4}' | grep -v Available)
    dd if=/dev/zero of=$MOUNTPOINT/zeros.img bs=1M count=$((((size-150000)) / 1000))
    rm -f $MOUNTPOINT/zeros.img
  fi
}

signature() {
  (cd ${scriptdir}/config;  tar cvf ${MOUNTPOINT}/usr/share/cloud/cloud-scripts.tar *)
  (cd ${scriptdir}/vpn;  tar rvf ${MOUNTPOINT}/usr/share/cloud/cloud-scripts.tar *)
  gzip -c ${MOUNTPOINT}/usr/share/cloud/cloud-scripts.tar  > ${MOUNTPOINT}/usr/share/cloud/cloud-scripts.tgz
  md5sum ${MOUNTPOINT}/usr/share/cloud/cloud-scripts.tgz |awk '{print $1}'  > ${MOUNTPOINT}/var/cache/cloud/cloud-scripts-signature
  echo "Cloudstack Release $CLOUDSTACK_RELEASE $(date)" > ${MOUNTPOINT}/etc/cloudstack-release
}

#check grub version

grub --version | grep "0.9" > /dev/null
if [ $? -ne 0 ]
then
    echo You need grub 0.9x\(grub-legacy\) to use this script!
    exit 1
fi

mkdir -p $IMAGENAME
mkdir -p $LOCATION
MOUNTPOINT=/mnt/$IMAGENAME/
IMAGELOC=$LOCATION/$IMAGENAME.img
scriptdir=$(dirname $PWD/$0)

rm -rf /tmp/systemvm
mkdir -p /tmp/systemvm
#cp ./xt_CHECKSUM.ko /tmp/systemvm
#cp ./iptables_1.4.8-3local1checksum1_i386.deb /tmp/systemvm
#cp ./xe-guest-utilities_5.6.0-595_i386.deb /tmp/systemvm

rm -f $IMAGELOC
begin=$(date +%s)
echo "*************INSTALLING BASEIMAGE********************"
baseimage

cp $scriptdir/config.dat $MOUNTPOINT/root/
cd $MOUNTPOINT

mount -o bind /proc $MOUNTPOINT/proc
mount -o bind /dev $MOUNTPOINT/dev

echo "*************CONFIGURING APT********************"
fixapt  
echo "*************DONE CONFIGURING APT********************"

echo "*************CONFIGURING NETWORK********************"
network
echo "*************DONE CONFIGURING NETWORK********************"

echo "*************INSTALLING KERNEL********************"
install_kernel $IMAGELOC
echo "*************DONE INSTALLING KERNEL********************"

echo "*************CONFIGURING GRUB********************"
fixgrub $IMAGELOC
echo "*************DONE CONFIGURING GRUB********************"


echo "*************CONFIGURING INITTAB********************"
fixinittab
echo "*************DONE CONFIGURING INITTAB********************"

echo "*************CONFIGURING FSTAB********************"
fixfstab
echo "*************DONE CONFIGURING FSTAB********************"

echo "*************CONFIGURING ACPID********************"
fixacpid
echo "*************DONE CONFIGURING ACPID********************"

echo "*************INSTALLING PACKAGES********************"
packages
echo "*************DONE INSTALLING PACKAGES********************"

echo "*************CONFIGURING IPTABLES********************"
fixiptables
echo "*************DONE CONFIGURING IPTABLES********************"

echo "*************CONFIGURING PASSWORD********************"
password

echo "*************CONFIGURING SERVICES********************"
services

echo "*************CONFIGURING APACHE********************"
apache2

echo "*************CONFIGURING VPN********************"
vpn_config

echo "*************FIX DHCP ISSUE********************"
#dhcp_fix

echo "*************INSTALL XS TOOLS********************"
#install_xs_tool

echo "*************CLEANING UP********************"
cleanup 

echo "*************GENERATING SIGNATURE********************"
signature

cd $scriptdir

umount $MOUNTPOINT/proc
umount $MOUNTPOINT/dev
umount $MOUNTPOINT
fin=$(date +%s)
t=$((fin-begin))
echo "Finished building image $IMAGELOC in $t seconds"

