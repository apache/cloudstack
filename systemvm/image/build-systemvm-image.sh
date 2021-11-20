#!/bin/bash
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.

# For faster builds run: apt-get install apt-cacher-ng
# Requirements: apt-get install debootstrap debian-keyring debian-archive-keyring qemu-utils nbdkit

set -x

ARCH="amd64"
BOOT_PKG="linux-image-$ARCH grub-pc"
FILE=systemvm-kvm.qcow2

clean_debian() {
	[ "$MNT_DIR" != "" ] && chroot $MNT_DIR umount /proc/ /sys/ /dev/ /boot/
	sleep 1s
	[ "$MNT_DIR" != "" ] && umount $MNT_DIR
	sleep 1s
	[ "$DISK" != "" ] && qemu-nbd -d $DISK
	sleep 1s
	[ "$MNT_DIR" != "" ] && rm -r $MNT_DIR
}

fail() {
	clean_debian
	echo ""
	echo "FAILED: $1"
	exit 1
}

cancel() {
	fail "CTRL-C detected"
}

trap cancel INT

# Create qcow2 disk
rm -f $FILE
qemu-img create -f qcow2 $FILE 4G

MNT_DIR=$(mktemp -d)
DISK=

echo "Looking for nbd device..."
modprobe nbd max_part=16 || fail "failed to load nbd module into kernel"
for i in /dev/nbd*
do
	if qemu-nbd -c $i $FILE
	then
		DISK=$i
		break
	fi
done
[ "$DISK" == "" ] && fail "no nbd device available"
echo "Connected $FILE to $DISK"

# Create partitions
echo "Partitioning $DISK..."
sfdisk $DISK -q << EOF || fail "cannot partition $FILE"
,256000,83,*
,640000,S
;
EOF

# Format partitions
mkfs.ext2 -q ${DISK}p1 || fail "cannot create /boot ext2"
mkswap ${DISK}p2 || fail "cannot create swap"
mkfs.ext4 -q ${DISK}p3 || fail "cannot create / ext4"

echo "Mounting root partition..."
mount ${DISK}p3 $MNT_DIR || fail "cannot mount /"

echo "Creating debian base..."
debootstrap --arch $ARCH --include="locales,sudo,openssh-server,acpid" stable $MNT_DIR http://ftp.debian.org/debian/ || fail "cannot install 'stable' into $DISK"

echo "Configuring fstab..."
UUID_BOOT=$(lsblk -f ${DISK}p1 | tail -1 | awk '{print $4}')
UUID_SWAP=$(lsblk -f ${DISK}p2 | tail -1 | awk '{print $4}')
UUID_ROOT=$(lsblk -f ${DISK}p3 | tail -1 | awk '{print $4}')
cat <<EOF > $MNT_DIR/etc/fstab
UUID=$UUID_ROOT  /     ext4  errors=remount-ro  0  1
UUID=$UUID_BOOT  /boot ext2  defaults           0  2
UUID=$UUID_SWAP  none  swap  sw                 0  0
EOF

echo "Binding /dev, /proc, /sys, mounting /boot"
mount --bind /dev/ $MNT_DIR/dev || fail "cannot bind /dev"
chroot $MNT_DIR mount -t ext4 ${DISK}p1 /boot || fail "cannot mount /boot"
chroot $MNT_DIR mount -t proc none /proc || fail "cannot mount /proc"
chroot $MNT_DIR mount -t sysfs none /sys || fail "cannot mount /sys"

echo "Installing $BOOT_PKG..."
LANG=C DEBIAN_FRONTEND=noninteractive chroot $MNT_DIR apt-get install -y --force-yes -q $BOOT_PKG || fail "cannot install $BOOT_PKG"
chroot $MNT_DIR grub-install $DISK --target=i386-pc --modules="biosdisk part_msdos" || fail "cannot install grub"

bash -x shar_cloud_scripts.sh
cp -vr ./scripts $MNT_DIR/
chroot $MNT_DIR bash /scripts/apt_upgrade.sh || fail "apt_upgrade.sh"
chroot $MNT_DIR bash /scripts/configure_grub.sh || fail "configure_grub.sh"
chroot $MNT_DIR bash /scripts/configure_locale.sh || fail "configure_locale.sh"
chroot $MNT_DIR bash /scripts/configure_networking.sh || fail "configure_networking.sh"
chroot $MNT_DIR bash /scripts/configure_acpid.sh || fail "configure_acpid.sh"
chroot $MNT_DIR bash /scripts/install_systemvm_packages.sh || fail "install_systemvm_packages.sh"
chroot $MNT_DIR bash /scripts/configure_conntrack.sh || fail "configure_conntrack.sh"
chroot $MNT_DIR bash /scripts/configure_login.sh || fail "configure_login.sh"
chroot $MNT_DIR bash /scripts/cloud_scripts_shar_archive.sh || fail "shar script"
chroot $MNT_DIR bash /scripts/configure_systemvm_services.sh || fail "configure_systemvm_services.sh"
chroot $MNT_DIR bash /scripts/cleanup.sh || fail "cleanup.sh"
chroot $MNT_DIR bash /scripts/finalize.sh || fail "finalize.sh"
rm -fr $MNT_DIR/scripts $MNT_DIR/cloud_scripts

# Install grub
#grub-install $DISK --target=i386-pc --root-directory=$MNT_DIR --modules="biosdisk part_msdos" || fail "cannot reinstall grub"

clean_debian
