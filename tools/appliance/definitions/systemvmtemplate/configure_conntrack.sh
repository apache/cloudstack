#!/bin/bash

set -e
set -x

# This is actually a bug in the conntrackd package. The comment in the conf file says stats logging is off by default
# but the parameter is set to on.
# After a couple weeks logrotate will rotate the conntrackd-stats.log file ans start conntracking even if we don't want
# it to (on non-redundant routers for instance).
function disable_conntrack_logging() {
  grep "LogFile off" /etc/conntrackd/conntrackd.conf && return

  sed -i '/Stats {/,/}/ s/LogFile on/LogFile off/' /etc/conntrackd/conntrackd.conf
  rm -f /var/log/conntrackd-stats.log
}

function load_conntrack_modules() {
  grep nf_conntrack_ipv4 /etc/modules && return

  cat >> /etc/modules << EOF
nf_conntrack_ipv4
nf_conntrack
nf_conntrack_ftp
nf_nat_ftp
EOF
}

function configure_conntrack() {
  disable_conntrack_logging
  load_conntrack_modules
}

return 2>/dev/null || configure_conntrack
