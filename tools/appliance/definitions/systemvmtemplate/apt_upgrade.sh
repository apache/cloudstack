#!/bin/bash

set -e
set -x

function add_backports() {
  sed -i '/backports/d' /etc/apt/sources.list
  echo 'deb http://http.us.debian.org/debian wheezy-backports main' >> /etc/apt/sources.list
}

function apt_upgrade() {
  DEBIAN_FRONTEND=noninteractive
  DEBIAN_PRIORITY=critical

  add_backports

  apt-get -q -y --force-yes update
  apt-get -q -y --force-yes upgrade
}

return 2>/dev/null || apt_upgrade
