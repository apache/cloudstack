#!/bin/bash
set -x
echo "overlaying scripts test"
apt-get --no-install-recommends -q -y --force-yes install build-essential linux-headers-`uname -r`
wget http://download.virtualbox.org/virtualbox/$VBOXVERSION/VBoxGuestAdditions_$VBOXVERSION.iso
mkdir /media/VBoxGuestAdditions
mount -o loop,ro VBoxGuestAdditions_$VBOXVERSION.iso /media/VBoxGuestAdditions
sh /media/VBoxGuestAdditions/VBoxLinuxAdditions.run
rm VBoxGuestAdditions_$VBOXVERSION.iso
umount /media/VBoxGuestAdditions
rmdir /media/VBoxGuestAdditions
apt-get --no-install-recommends -q -y --force-yes remove build-essential linux-headers-`uname -r`

