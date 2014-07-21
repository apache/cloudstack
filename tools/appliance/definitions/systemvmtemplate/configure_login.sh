setup_accounts() {
  # Setup sudo to allow no-password sudo for "admin"
  groupadd -r admin
  # Create a 'cloud' user if it's not there
  id cloud
  if [[ $? -ne 0 ]]
  then
    useradd -G admin cloud
  else
    usermod -a -G admin cloud
  fi
  echo "root:$ROOTPW" | chpasswd
  echo "cloud:`openssl rand -base64 32`" | chpasswd
  sed -i -e '/Defaults\s\+env_reset/a Defaults\texempt_group=admin' /etc/sudoers
  sed -i -e 's/%admin ALL=(ALL) ALL/%admin ALL=NOPASSWD:/bin/chmod, /bin/cp, /bin/mkdir, /bin/mount, /bin/umount/g' /etc/sudoers
  # Disable password based authentication via ssh, this will take effect on next reboot
  sed -i -e 's/^.*PasswordAuthentication .*$/PasswordAuthentication no/g' /etc/ssh/sshd_config
  # Secure ~/.ssh
  mkdir -p /home/cloud/.ssh
  chmod 700 /home/cloud/.ssh
}

fix_inittab() {
  # Fix inittab
  cat >> /etc/inittab << EOF

vc:2345:respawn:/sbin/getty 38400 hvc0
EOF
}

setup_accounts
fix_inittab

