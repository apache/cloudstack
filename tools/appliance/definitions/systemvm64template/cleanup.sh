# Clean up
#apt-get -y remove linux-headers-$(uname -r) build-essential
apt-get -y remove dictionaries-common busybox
apt-get -y autoremove
apt-get autoclean
apt-get clean

# Removing leftover leases and persistent rules
echo "cleaning up dhcp leases"
rm /var/lib/dhcp/*

# Make sure Udev doesn't block our network
echo "cleaning up udev rules"
rm /etc/udev/rules.d/70-persistent-net.rules
rm -rf /dev/.udev/
rm /lib/udev/rules.d/75-persistent-net-generator.rules
