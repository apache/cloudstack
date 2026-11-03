#!/bin/bash
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.

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
nf_conntrack
nf_conntrack_ftp
nf_conntrack_pptp
nf_conntrack_proto_gre
nf_nat_tftp
nf_nat_ftp
EOF
}

function configure_conntrack() {
  disable_conntrack_logging
  load_conntrack_modules
}

return 2>/dev/null || configure_conntrack
