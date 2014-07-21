#!/bin/bash

set -e
set -x

# clean up stuff copied in by veewee
function cleanup_veewee() {
  # this has to be here since it is the last file to run (and we remove ourselves)
  rm -fv /root/*.iso
  rm -fv /root/{apt_upgrade,build_time,cleanup,install_systemvm_packages,zerodisk}.sh
  rm -fv /root/configure_{acpid,conntrack,grub,locale,login,networking,systemvm_services}.sh
  rm -fv .veewee_version .veewee_params .vbox_version
}

# Zero out the free space to save space in the final image:
function zero_disk() {
  cleanup_veewee

  for path in / /boot /usr /var /opt /tmp /home
  do
    dd if=/dev/zero of=${path}/zero bs=1M || true
    sync
    rm -f ${path}/zero
  done
}

return 2>/dev/null || zero_disk
