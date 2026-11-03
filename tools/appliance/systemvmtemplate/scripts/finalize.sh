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

function configure_misc() {
  rm -fv /home/cloud/*.sh
  echo "cloud:`openssl rand -base64 32`" | chpasswd
}

function configure_rundisk_size() {
  # Debian's default is 10% of total RAM. This is too low when total is 256M.
  # See https://manpages.debian.org/stretch/initscripts/tmpfs.5.en.html
  echo "tmpfs /run tmpfs nodev,nosuid,size=20%,mode=755 0 0" >> /etc/fstab
}

function configure_sudoers() {
  cat >/etc/sudoers <<END
Defaults	env_reset
Defaults	exempt_group=admin
Defaults	mail_badpass
Defaults	secure_path="/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin"

root	  ALL=(ALL:ALL) ALL
%admin	ALL=NOPASSWD:/bin/chmod, /bin/cp, /bin/mkdir, /bin/mount, /bin/umount

#includedir /etc/sudoers.d
END
  echo 'cloud ALL=NOPASSWD:/bin/chmod, /bin/cp, /bin/mkdir, /bin/mount, /bin/umount, /sbin/halt' > /etc/sudoers.d/cloud
}

# clean up stuff copied in
function cleanup_final() {
  # this has to be here since it is the last file to run (and we remove ourselves)
  cd /home/cloud
  rm -fv *.iso
  rm -fv {apt_upgrade,authorized_keys,cleanup,install_systemvm_packages,zerodisk}.sh
  rm -fv configure_{acpid,conntrack,grub,locale,networking,persistent_config,systemvm_services}.sh /home/cloud/install_systemvm_packages.sh
  rm -fv .veewee_version .veewee_params .vbox_version
}

# Zero out the free space to save space in the final image:
function zero_disk() {
  df -h
  fstrim -av
  for path in / /boot /var/log /tmp
  do
    dd if=/dev/zero of=${path}/zero bs=1M || true
    sync
    rm -f ${path}/zero
  done
}

function finalize() {
  depmod -a
  configure_misc
  configure_rundisk_size
  configure_sudoers
  cleanup_final
  sync
  zero_disk
  sync
}

return 2>/dev/null || finalize
