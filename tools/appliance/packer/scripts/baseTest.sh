# Update the box

export DEBIAN_FRONTEND=noninteractive
export DEBIAN_PRIORITY=critical

apt-get -q -y --force-yes update
apt-get -q -y --force-yes install curl unzip less
apt-get clean


# Set up sudo, TODO: Check security concerns
#echo 'vagrant ALL=NOPASSWD:/bin/chmod, /bin/cp, /bin/mkdir, /bin/mount, /bin/umount','/bin/bash','/bin/build.sh setenv' > /etc/sudoers.d/vagrant
echo 'vagrant ALL=(ALL) ALL' > /etc/sudoers.d/vagrant
echo 'cloud ALL=NOPASSWD:/bin/chmod, /bin/cp, /bin/mkdir, /bin/mount, /bin/umount' > /etc/sudoers.d/cloud

# Tweak sshd to prevent DNS resolution (speed up logins)
echo 'UseDNS no' >> /etc/ssh/sshd_config

#build directories for configure script overlays

mkdir -p /opt/cloudstack/systemvm/patches/debian

# Remove 5s grub timeout to speed up booting
cat <<EOF > /etc/default/grub
# If you change this file, run 'update-grub' afterwards to update
# /boot/grub/grub.cfg.

GRUB_DEFAULT=0
GRUB_TIMEOUT=0
GRUB_DISTRIBUTOR=`lsb_release -i -s 2> /dev/null || echo Debian`
GRUB_CMDLINE_LINUX="debian-installer=en_US%template=domP%name=r-223-VM%vmpassword=password%eth2ip=192.168.30.10%eth2mask=255.255.255.0%gateway=192.168.20.2%eth0ip=10.1.1.10%eth0mask=255.255.255.0%redundant_router=0%guestgw=10.1.1.1%guestbrd=10.1.1.255%guestcidrsize=24%router_pr=100%domain=csa8cloud.internal%dhcprange=10.1.1.101%eth1ip=169.254.2.214%eth1mask=255.255.255.0%type=router%disable_rp_filter=true%dns1=8.8.8.8%dns2=8.8.4.4"
#GRUB_CMDLINE_LINUX_DEFUALT="debian-installer=en_US"
EOF

update-grub
