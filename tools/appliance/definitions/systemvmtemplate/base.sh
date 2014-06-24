# Update the box

export DEBIAN_FRONTEND=noninteractive
export DEBIAN_PRIORITY=critical

apt-get -q -y --force-yes update
apt-get -q -y --force-yes install curl unzip
apt-get clean

# Set up sudo, TODO: Check security concerns
echo 'vagrant ALL=NOPASSWD:/bin/chmod, /bin/cp, /bin/mkdir, /bin/mount, /bin/umount' > /etc/sudoers.d/vagrant
echo 'cloud ALL=NOPASSWD:/bin/chmod, /bin/cp, /bin/mkdir, /bin/mount, /bin/umount' > /etc/sudoers.d/cloud

# Tweak sshd to prevent DNS resolution (speed up logins)
echo 'UseDNS no' >> /etc/ssh/sshd_config

# Remove 5s grub timeout to speed up booting
cat <<EOF > /etc/default/grub
# If you change this file, run 'update-grub' afterwards to update
# /boot/grub/grub.cfg.

GRUB_DEFAULT=0
GRUB_TIMEOUT=0
GRUB_DISTRIBUTOR=`lsb_release -i -s 2> /dev/null || echo Debian`
GRUB_CMDLINE_LINUX_DEFAULT="quiet"
GRUB_CMDLINE_LINUX="debian-installer=en_US"
EOF

update-grub
