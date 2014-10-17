#!/usr/bin/env bash
set -x 
mkdir /tmp/virtualbox
VERSION=$(cat /root/.vbox_version)
mount -o loop /tmp/guestAdditions/VBoxGuestAdditions.iso /tmp/virtualbox
sh /tmp/virtualbox/VBoxLinuxAdditions.run
umount /tmp/virtualbox
rmdir /tmp/virtualbox
rm /tmp/guestAdditions/*.iso
