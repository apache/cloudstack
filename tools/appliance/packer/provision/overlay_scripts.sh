#!/bin/bash
set -x
echo "overlaying scripts test"
version=$1
wget http://download.virtualbox.org/virtualbox/$version/VBoxGuestAdditions_$version.iso
sudo mkdir /media/VBoxGuestAdditions
sudo mount -o loop,ro VBoxGuestAdditions_$version.iso /media/VBoxGuestAdditions
sudo sh /media/VBoxGuestAdditions/VBoxLinuxAdditions.run
rm VBoxGuestAdditions_$version.iso
sudo umount /media/VBoxGuestAdditions
sudo rmdir /media/VBoxGuestAdditions
