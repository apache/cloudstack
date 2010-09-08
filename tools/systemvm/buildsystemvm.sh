#!/bin/bash

IMAGENAME=systemvm
LOCATION=/var/lib/images/systemvm2
PASSWORD=password
APT_PROXY=
HOSTNAME=systemvm
SIZE=2000
DEBIAN_MIRROR=ftp.us.debian.org/debian

baseimage() {
  mkdir -p $LOCATION
  dd if=/dev/zero of=$IMAGELOC bs=1M seek=$((SIZE - 1)) count=1
  loopdev=$(losetup -f)
  losetup $loopdev $IMAGELOC
  parted $loopdev -s 'mklabel msdos'
  parted $loopdev -s 'mkpart primary ext3 512B 2097151000B'
  losetup -d $loopdev
  loopdev=$(losetup --show -o 512 -f $IMAGELOC )
  mkfs.ext3  -L ROOT $loopdev
  mkdir -p $MOUNTPOINT
  tune2fs -c 100 -i 0 $loopdev
  losetup -d $loopdev
  
  mount -o loop,offset=512 $IMAGELOC  $MOUNTPOINT
  
  #debootstrap --variant=minbase --keyring=/usr/share/keyrings/debian-archive-keyring.gpg squeeze $MOUNTPOINT http://${APT_PROXY}${DEBIAN_MIRROR}
  debootstrap --variant=minbase --arch=i386 squeeze $MOUNTPOINT http://${APT_PROXY}${DEBIAN_MIRROR}
}


fixapt() {
  if [ "$APT_PROXY" != "" ]; then
  cat >> etc/apt/apt.conf.d/01proxy << EOF
Acquire::http::Proxy "http://${APT_PROXY}";
EOF
  fi

  cat > etc/apt/sources.list << EOF
deb http://ftp.us.debian.org/debian/ squeeze main non-free
deb-src http://ftp.us.debian.org/debian/ squeeze main non-free

deb http://security.debian.org/ squeeze/updates main
deb-src http://security.debian.org/ squeeze/updates main

deb http://volatile.debian.org/debian-volatile squeeze/volatile main
deb-src http://volatile.debian.org/debian-volatile squeeze/volatile main

deb http://ftp.us.debian.org/debian testing main contrib non-free
EOF

  cat >> etc/apt/apt.conf << EOF
APT::Default-Release "stable"; 
EOF

  cat >> etc/apt/preferences << EOF
Package: *
Pin: release o=Debian,a=stable
Pin-Priority: 900

Package: *
Pin: release o=Debian,a=testing
Pin-Priority: 400
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
auto lo
iface lo inet loopback

# The primary network interface
allow-hotplug eth0
iface eth0 inet dhcp

EOF
}

install_kernel() {
  DEBIAN_FRONTEND=noninteractive
  DEBIAN_PRIORITY=critical
  export DEBIAN_FRONTEND DEBIAN_PRIORITY

  chroot . apt-get -qq -y --force-yes install grub &&
  cp -av usr/lib/grub/i386-pc boot/grub
  #for some reason apt-get install grub does not install grub/stage1 etc
  loopd=$(losetup -f --show $1)
  grub-install $loopd --root-directory=$MOUNTPOINT
  losetup -d $loopd
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
  chroot . apt-get install -qq -y --force-yes linux-image-686-bigmem
  cat >> etc/kernel-img.conf << EOF
postinst_hook = /usr/sbin/update-grub
postrm_hook   = /usr/sbin/update-grub
EOF
}


fixgrub() {
  cat > boot/grub/menu.lst << EOF
default 0
timeout 2
color cyan/blue white/blue

### BEGIN AUTOMAGIC KERNELS LIST
# kopt=root=LABEL=ROOT ro

## ## End Default Options ##
title		Debian GNU/Linux, kernel 2.6.32-5-686-bigmem 
root		(hd0,0)
kernel		/boot/vmlinuz-2.6.32-5-686-bigmem root=LABEL=ROOT ro console=tty0 xencons=ttyS0,115200 console=hvc0 quiet
initrd		/boot/initrd.img-2.6.32-5-686-bigmem

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
LABEL=ROOT      /               ext3    errors=remount-ro 0       1
EOF
}

fixacpid() {
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

packages() {
  DEBIAN_FRONTEND=noninteractive
  DEBIAN_PRIORITY=critical
  DEBCONF_DB_OVERRIDE=’File{/root/config.dat}’
  export DEBIAN_FRONTEND DEBIAN_PRIORITY DEBCONF_DB_OVERRIDE

  chroot .  apt-get --no-install-recommends -q -y --force-yes install rsyslog chkconfig insserv net-tools ifupdown vim netbase iptables openssh-server grub e2fsprogs dhcp3-client dnsmasq tcpdump socat wget apache2 python2.5 bzip2 sed gawk diff grep gzip less tar telnet xl2tpd traceroute openswan psmisc 

  chroot . apt-get --no-install-recommends -q -y --force-yes -t backports install haproxy nfs-common

  echo "***** getting additional modules *********"
  chroot .  apt-get --no-install-recommends -q -y --force-yes  install iproute acpid iptables-persistent

  echo "***** getting sun jre 6*********"
  DEBIAN_FRONTEND=readline
  export DEBIAN_FRONTEND 
  chroot . echo sun-java6-jdk shared/accepted-sun-dlj-v1-1 boolean true | debconf-set-selections
  chroot .  apt-get --no-install-recommends -q -y install  sun-java6-jre 

}

password() {
  chroot . echo "root:$PASSWORD" | chroot . chpasswd
}

cleanup() {
  rm -f usr/sbin/policy-rc.d
  rm -f etc/apt/apt.conf.d/01proxy 
}

mkdir -p $IMAGENAME
mkdir -p $LOCATION
MOUNTPOINT=/mnt/$IMAGENAME/
IMAGELOC=$LOCATION/$IMAGENAME.img
scriptdir=$(dirname $PWD/$0)

rm -f $IMAGELOC

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

#cp etc/inittab etc/inittab.hvm
#cp $scriptdir/inittab.xen etc/inittab.xen
#cp $scriptdir/inittab.xen etc/inittab
#cp $scriptdir/fstab.xen etc/fstab.xen
#cp $scriptdir/fstab.xen etc/fstab
#cp $scriptdir/fstab etc/fstab

echo "*************INSTALLING PACKAGES********************"
packages
echo "*************DONE INSTALLING PACKAGES********************"

echo "*************CONFIGURING PASSWORD********************"
password

echo "*************CLEANING UP********************"
cleanup 

cd $scriptdir

umount $MOUNTPOINT/proc
umount $MOUNTPOINT/dev
umount $MOUNTPOINT

