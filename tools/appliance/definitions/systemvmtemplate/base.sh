# Update the box
apt-get -y update
#below are needed for ruby perhaps
#apt-get -y install linux-headers-$(uname -r) build-essential
#apt-get -y install zlib1g-dev libssl-dev libreadline-gplv2-dev
apt-get -y install curl unzip
apt-get clean

# Set up sudo, TODO: Check security concerns
echo 'cloud ALL=NOPASSWD:ALL' > /etc/sudoers.d/cloud

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
