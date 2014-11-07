#!/bin/bash
set -x
echo "overlaying scripts test"
wget http://download.virtualbox.org/virtualbox/$VBOXVERSION/VBoxGuestAdditions_$VBOXVERSION.iso
mkdir /media/VBoxGuestAdditions
mount -o loop,ro VBoxGuestAdditions_$VBOXVERSION.iso /media/VBoxGuestAdditions
sh /media/VBoxGuestAdditions/VBoxLinuxAdditions.run
rm VBoxGuestAdditions_$VBOXVERSION.iso
umount /media/VBoxGuestAdditions
rmdir /media/VBoxGuestAdditions
