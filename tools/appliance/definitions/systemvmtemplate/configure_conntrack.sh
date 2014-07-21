# This is actually a bug in the conntrackd package. The comment in the conf file says stats logging is off by default but the parameter is set to on.
# After a couple weeks logrotate will rotate the conntrackd-stats.log file ans start conntracking even if we don't want it to (on non-redundant routers for instance).
fix_conntrackd() {
  sed -i '/Stats {/,/}/ s/LogFile on/LogFile off/' /etc/conntrackd/conntrackd.conf
  rm -f /var/log/conntrackd-stats.log
}

# Preload these module otherwise the sysctl settings will not be set, and pasive ftp will not work.
fix_modules() {
  cat >> /etc/modules << EOF
nf_conntrack_ipv4
nf_conntrack
nf_conntrack_ftp
nf_nat_ftp
EOF
}

fix_conntrackd
fix_modules
