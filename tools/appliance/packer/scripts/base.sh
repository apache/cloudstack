# Update the box

export DEBIAN_FRONTEND=noninteractive
export DEBIAN_PRIORITY=critical

apt-get -q -y --force-yes update
apt-get -q -y --force-yes install curl unzip less ssh-utils
apt-get clean

# Set up sudo, TODO: Check security concerns
echo 'vagrant ALL=NOPASSWD:/bin/chmod, /bin/cp, /bin/mkdir, /bin/mount, /bin/umount' > /etc/sudoers.d/vagrant
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
GRUB_CMDLINE_LINUX_DEFAULT="quiet"
#GRUB_CMDLINE_LINUX="debian-installer=en_US"
GRUB_CMDLINE_LINUX="debian-installer=en_US quiet -- quiet console=hvc0%template=domP%name=r-224-VM%vmpassword=3mxSGAZQ%eth2ip=207.19.99.29%eth2mask=255.255.255.224%gateway=07.19.99.1%eth0ip=10.1.1.235%eth0mask=255.255.255.0%redundant_router=1%guestgw=10.1.1.1%guestbrd=10.1.1.255%guestcidrsize=24%router_pr=99%domain=csa8cloud.internal%dhcprange=10.1.1.1%eth1ip=169.254.2.50%eth1mask=255.255.0.0%type=router%disable_rp_filter=true%dns1=8.8.8.8%dns2=8.8.4.4
EOF

update-grub
