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

# Eject cdrom if any
eject || true

# Setup router
CMDLINE=/var/cache/cloud/cmdline
for str in $(cat $CMDLINE)
  do
    KEY=$(echo $str | cut -d= -f1)
    VALUE=$(echo $str | cut -d= -f2)
    case $KEY in
      type)
        export TYPE=$VALUE
        ;;
      *)
        ;;
    esac
done

if [ "$TYPE" == "router" ] || [ "$TYPE" == "vpcrouter" ] || [ "$TYPE" == "dhcpsrvr" ]
then
  if [ -x /opt/cloud/bin/update_config.py ]
  then
      /opt/cloud/bin/update_config.py cmd_line.json || true
      logger -t cloud "postinit: Updated config cmd_line.json"
  fi
fi

if [ "$TYPE" == "router" ]
then
    python /opt/cloud/bin/baremetal-vr.py &
    logger -t cloud "Started baremetal-vr service"
fi

[ ! -f /var/cache/cloud/enabled_svcs ] && touch /var/cache/cloud/enabled_svcs
for svc in $(cat /var/cache/cloud/enabled_svcs)
do
   logger -t cloud "Starting $svc"
   systemctl enable --no-block --now $svc
done

[ ! -f /var/cache/cloud/disabled_svcs ] && touch /var/cache/cloud/disabled_svcs
for svc in $(cat /var/cache/cloud/disabled_svcs)
do
   logger -t cloud "Stopping $svc"
   systemctl disable --no-block --now $svc
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

# Enable SSH by default
systemctl enable --no-block --now ssh

date > /var/cache/cloud/boot_up_done
logger -t cloud "Boot up process done"
