#!/bin/bash

set -e
set -x

function cleanup_apt() {
  #apt-get -y remove linux-headers-$(uname -r) build-essential
  apt-get -y remove dictionaries-common busybox
  apt-get -y autoremove
  apt-get autoclean
  apt-get clean
}

# Removing leftover leases and persistent rules
function cleanup_dhcp() {
  rm -f /var/lib/dhcp/*
}

# Make sure Udev doesn't block our network
function cleanup_dev() {
  echo "cleaning up udev rules"
  rm -f /etc/udev/rules.d/70-persistent-net.rules
  rm -rf /dev/.udev/
  rm -f /lib/udev/rules.d/75-persistent-net-generator.rules
}

function cleanup() {
  cleanup_apt
  cleanup_dhcp
  cleanup_dev
}

return 2>/dev/null || cleanup
