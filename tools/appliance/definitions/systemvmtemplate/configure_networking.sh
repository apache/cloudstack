HOSTNAME=systemvm

echo "Adding a 2 sec delay to the interface up, to make the dhclient happy"
echo "pre-up sleep 2" >> /etc/network/interfaces

fix_nameserver() {
  # Replace /etc/resolv.conf also
  cat > /etc/resolv.conf << EOF
nameserver 8.8.8.8
nameserver 8.8.4.4
EOF
}

fix_hostname() {
  # Fix hostname in openssh-server generated keys
  sed -i "s/root@\(.*\)$/root@$HOSTNAME/g" /etc/ssh/ssh_host_*.pub
  # Fix hostname to override one provided by dhcp during vm build
  echo "$HOSTNAME" > /etc/hostname
  hostname $HOSTNAME
  # Delete entry in /etc/hosts derived from dhcp
  sed -i '/127.0.1.1/d' /etc/hosts
}

fix_hostname
fix_nameserver
