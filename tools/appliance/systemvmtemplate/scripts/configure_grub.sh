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

set -e
set -x

function configure_grub() {
  # Remove the old/unused kernel
  dpkg --list | grep linux-image | awk '{ print $2 }' | sort -V | sed -n '/'`uname -r`'/q;p' | xargs sudo apt-get remove -y --purge || true
  apt-get -y autoremove --purge
  echo "blacklist floppy" > /etc/modprobe.d/blacklist-floppy.conf
  rmmod floppy || true
  update-initramfs -u

  cat > /etc/default/grub <<EOF
# If you change this file, run 'update-grub' afterwards to update
# /boot/grub/grub.cfg.

GRUB_DEFAULT=0
GRUB_TIMEOUT=0
GRUB_DISTRIBUTOR=Debian
GRUB_CMDLINE_LINUX_DEFAULT="quiet fsck.mode=force fsck.repair=yes"
GRUB_CMDLINE_LINUX="console=tty0 console=ttyS0,115200n8 console=hvc0 earlyprintk=xen net.ifnames=0 biosdevname=0 debian-installer=en_US nomodeset"
GRUB_CMDLINE_XEN="com1=115200 console=com1"
GRUB_TERMINAL="console serial"
GRUB_SERIAL_COMMAND="serial --speed=115200 --unit=0 --word=8 --parity=no --stop=1"

EOF

  grub-mkconfig -o /boot/grub/grub.cfg
  update-grub
}

return 2>/dev/null || configure_grub
