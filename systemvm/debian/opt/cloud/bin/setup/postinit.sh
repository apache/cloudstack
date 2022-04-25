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
#
# This scripts before ssh.service but after cloud-early-config

log_it() {
  echo "$(date) $@" >> /var/log/cloud.log
  log_action_msg "$@"
}

# Restart journald for setting changes to apply
systemctl restart systemd-journald

CMDLINE=/var/cache/cloud/cmdline
TYPE=$(grep -Po 'type=\K[a-zA-Z]*' $CMDLINE)
if [ "$TYPE" == "router" ] || [ "$TYPE" == "vpcrouter" ] || [ "$TYPE" == "dhcpsrvr" ]
then
  if [ -x /opt/cloud/bin/update_config.py ]
  then
    /opt/cloud/bin/update_config.py cmd_line.json || true
  fi
fi

if [ "$TYPE" == "cksnode" ]; then
  pkill -9 dhclient
fi

[ ! -f /var/cache/cloud/enabled_svcs ] && touch /var/cache/cloud/enabled_svcs
for svc in $(cat /var/cache/cloud/enabled_svcs)
do
  systemctl enable --now --no-block $svc
done

[ ! -f /var/cache/cloud/disabled_svcs ] && touch /var/cache/cloud/disabled_svcs
for svc in $(cat /var/cache/cloud/disabled_svcs)
do
  systemctl disable --now --no-block $svc
done

# Restore the persistent iptables nat, rules and filters for IPv4 and IPv6 if they exist
ipv4="/etc/iptables/rules.v4"
if [ -e $ipv4 ]
then
  iptables-restore < $ipv4
fi

ipv6="/etc/iptables/rules.v6"
if [ -e $ipv6 ]
then
  ip6tables-restore < $ipv6
fi

date > /var/cache/cloud/boot_up_done
