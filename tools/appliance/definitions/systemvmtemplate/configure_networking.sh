#!/bin/bash

set -e
set -x

HOSTNAME=systemvm

# Adding a 2 sec delay to the interface up, to make the dhclient happy
function set_interface_sleep() {
  grep "pre-up sleep 2" /etc/network/interfaces && return

  echo "pre-up sleep 2" >> /etc/network/interfaces
}

function configure_resolv_conf() {
  grep 8.8.8.8 /etc/resolv.conf && grep 8.8.4.4 /etc/resolv.conf && return

  cat > /etc/resolv.conf << EOF
nameserver 8.8.8.8
nameserver 8.8.4.4
EOF
}

# Delete entry in /etc/hosts derived from dhcp
function delete_dhcp_ip() {
  result=$(grep 127.0.1.1 /etc/hosts || true)
  [ "${result}" == "" ] && return

  sed -i '/127.0.1.1/d' /etc/hosts
}

function configure_hostname() {
  sed -i "s/root@\(.*\)$/root@$HOSTNAME/g" /etc/ssh/ssh_host_*.pub

  echo "$HOSTNAME" > /etc/hostname
  hostname $HOSTNAME
}

function configure_networking() {
  set_interface_sleep
  configure_resolv_conf
  delete_dhcp_ip
  configure_hostname
}

return 2>/dev/null || configure_networking
