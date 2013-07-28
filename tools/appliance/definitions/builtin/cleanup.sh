# Clean up unneeded packages.
yum -y erase gtk2 libX11 hicolor-icon-theme avahi freetype bitstream-vera-fonts
yum -y clean all

#rm -rf /etc/yum.repos.d/{puppetlabs,epel}.repo
rm -rf VBoxGuestAdditions_*.iso
rm -rf xs-tools*.iso

# Ensure that udev doesn't screw us with network device naming.
ln -sf /dev/null /lib/udev/rules.d/75-persistent-net-generator.rules
rm -f /etc/udev/rules.d/70-persistent-net.rules

# On startup, remove HWADDR from the eth0 interface.
cp -f /etc/sysconfig/network-scripts/ifcfg-eth0 /tmp/eth0
sed "/^HWADDR/d" /tmp/eth0 > /etc/sysconfig/network-scripts/ifcfg-eth0
sed -e "s/dhcp/none/;s/eth0/eth1/" /etc/sysconfig/network-scripts/ifcfg-eth0 > /etc/sysconfig/network-scripts/ifcfg-eth1

# Prevent way too much CPU usage in VirtualBox by disabling APIC.
sed -e 's/\tkernel.*/& noapic/' /boot/grub/grub.conf > /tmp/new_grub.conf
mv /boot/grub/grub.conf /boot/grub/grub.conf.bak
mv /tmp/new_grub.conf /boot/grub/grub.conf
