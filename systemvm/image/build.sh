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
# Requirements: apt-get install debootstrap debian-keyring debian-archive-keyring qemu-utils libguestfs-tools apt-cacher-ng
# Reference: examples from https://gist.github.com/spectra/10301941 and https://diogogomes.com/2012/07/13/debootstrap-kvm-image/
# Usage: sudo bash -x <this script>
set -x

clean_env() {
  [ "$MNT_DIR" != "" ] && chroot $MNT_DIR umount /proc /sys /dev /boot
  [ "$MNT_DIR" != "" ] && umount $MNT_DIR && sync
  [ "$DISK" != "" ] && qemu-nbd -d $DISK && sync
  [ "$MNT_DIR" != "" ] && rm -fr $MNT_DIR
}

fail() {
  clean_env
  echo "Failed: $1"
  exit 1
}

cancel() {
  fail "Ctrl-C detected"
}

trap cancel INT

FILE=image.qcow2
rm -f $FILE && qemu-img create -f qcow2 -o compat=0.10 $FILE 5G
DISK=
modprobe nbd max_part=16
for i in /dev/nbd*; do
  if qemu-nbd -c $i $FILE; then
    DISK=$i
    break
  fi
done
[ "$DISK" == "" ] && fail "no nbd device available"

# Create partitions
bash -x ./scripts/part.sh $DISK || fail "cannot partition $FILE"
mkfs.ext2 -q ${DISK}p1 || fail "cannot create /boot ext2"
mkswap ${DISK}p2 || fail "cannot create swap"
mkfs.ext4 -q ${DISK}p3 || fail "cannot create / ext4"

# Create Debian base
MNT_DIR=$(mktemp -d)
mount ${DISK}p3 $MNT_DIR || fail "cannot mount /"
debootstrap --arch amd64 --include="sudo,bzip2,locales,openssh-server,openssl,acpid" bullseye $MNT_DIR http://ftp.debian.org/debian/ || fail "cannot install 'bullseye' into $DISK"

# Configure fstab and mounts
UUID_BOOT=$(lsblk -f ${DISK}p1 | tail -1 | awk '{print $4}')
UUID_SWAP=$(lsblk -f ${DISK}p2 | tail -1 | awk '{print $4}')
UUID_ROOT=$(lsblk -f ${DISK}p3 | tail -1 | awk '{print $4}')
cat <<EOF > $MNT_DIR/etc/fstab
UUID=$UUID_ROOT  /     ext4  errors=remount-ro  0  1
UUID=$UUID_BOOT  /boot ext2  defaults           0  2
UUID=$UUID_SWAP  none  swap  sw                 0  0
EOF
mount --bind /dev/ $MNT_DIR/dev || fail "cannot bind /dev"
chroot $MNT_DIR mount -t ext4 ${DISK}p1 /boot || fail "cannot mount /boot"
chroot $MNT_DIR mount -t proc none /proc || fail "cannot mount /proc"
chroot $MNT_DIR mount -t sysfs none /sys || fail "cannot mount /sys"

# Install Linux Kernel and Grub
DEBIAN_FRONTEND=noninteractive chroot $MNT_DIR apt-get install -y --force-yes -q linux-image-amd64 grub-pc || fail "failed to install Linux Kernel and Grub"
chroot $MNT_DIR grub-install $DISK --target=i386-pc --modules="biosdisk part_msdos" || fail "cannot install grub"

# Setup SystemVM
bash -x shar_cloud_scripts.sh
cp -vr ./scripts $MNT_DIR/
for script in apt_upgrade.sh configure_grub.sh configure_locale.sh \
              configure_networking.sh configure_acpid.sh install_systemvm_packages.sh \
              configure_conntrack.sh configure_login.sh cloud_scripts_shar_archive.sh \
              configure_systemvm_services.sh cleanup.sh finalize.sh; do
    chroot $MNT_DIR bash -x /scripts/$script || fail "$script failed"
done
rm -fr $MNT_DIR/scripts $MNT_DIR/cloud_scripts scripts/cloud_scripts_shar_archive.sh
clean_env
# this works well on Ubuntu but not on CentOS:
# virt-sparsify $FILE --compress systemvmtemplate-kvm.qcow2 && rm -f $FILE
qemu-img convert -f qcow2 -O qcow2 -c $FILE systemvmtemplate-kvm.qcow2 && rm -f $FILE
