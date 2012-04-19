#removing iptables entry for vnc ports
iptables -D RH-Firewall-1-INPUT -p tcp -m tcp --dport 5900:6099 -j ACCEPT 2>&1

# remove listening vnc on all interface
sed -i 's/0\.0\.0\.0/127\.0\.0\.1/' /opt/xensource/libexec/vncterm-wrapper 2>&1
sed -i 's/0\.0\.0\.0/127\.0\.0\.1/' /opt/xensource/libexec/qemu-dm-wrapper 2>&1

