#!/bin/bash

set -e
set -x

function add_admin_group() {
  groupadd -f -r admin
}

function configure_cloud_user() {
  usermod -a -G admin cloud
  mkdir -p /home/cloud/.ssh
  chmod 700 /home/cloud/.ssh
  echo "cloud:`openssl rand -base64 32`" | chpasswd
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
  echo 'cloud ALL=NOPASSWD:/bin/chmod, /bin/cp, /bin/mkdir, /bin/mount, /bin/umount' > /etc/sudoers.d/cloud
}

# sshd_config is overwritten from cloud_scripts
#function configure_sshd() {
#  grep "UseDNS no" /etc/ssh/sshd_config && \
#      grep "PasswordAuthentication no" /etc/ssh/sshd_config && \
#      return
#  # Tweak sshd to prevent DNS resolution (speed up logins)
#  echo 'UseDNS no' >> /etc/ssh/sshd_config
#
#  # Require ssh keys for login
#  sed -i -e 's/^.*PasswordAuthentication .*$/PasswordAuthentication no/g' /etc/ssh/sshd_config
#}

function configure_inittab() {
  grep "vc:2345:respawn:/sbin/getty" /etc/inittab && return

  # Fix inittab
  cat >> /etc/inittab << EOF

vc:2345:respawn:/sbin/getty 38400 hvc0
EOF
}

function configure_login() {
  add_admin_group
  configure_cloud_user
  configure_sudoers
  # configure_sshd
  configure_inittab
}

return 2>/dev/null || configure_login
