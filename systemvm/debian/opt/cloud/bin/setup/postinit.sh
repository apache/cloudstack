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

. /lib/lsb/init-functions

log_it() {
  echo "$(date) $@" >> /var/log/cloud.log
  log_action_msg "$@"
}

# Restart journald for setting changes to apply
systemctl restart systemd-journald

# Restore the persistent iptables nat, rules and filters for IPv4 and IPv6 if they exist
nftables="/etc/iptables/rules.nftables"
if [ -e $nftables ]
then
  nft -f $nftables
fi

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

CMDLINE=/var/cache/cloud/cmdline
TYPE=$(grep -Po 'type=\K[a-zA-Z]*' $CMDLINE)

# Execute cloud-init if user data is present
run_cloud_init() {
  if [ ! -f "$CMDLINE" ]; then
    log_it "No cmdline file found, skipping cloud-init execution"
    return 0
  fi

  local encoded_userdata=$(grep -Po 'userdata=\K[^[:space:]]*' "$CMDLINE" || true)
  if [ -z "$encoded_userdata" ]; then
    log_it "No user data found in cmdline, skipping cloud-init execution"
    return 0
  fi

  log_it "User data detected, setting up and running cloud-init"

  # Update cloud-init config to use NoCloud datasource
  cat <<EOF > /etc/cloud/cloud.cfg.d/cloudstack.cfg
#cloud-config
datasource_list: ['NoCloud']
network:
  config: disabled
manage_etc_hosts: false
manage_resolv_conf: false
users: []
disable_root: false
ssh_pwauth: false
cloud_init_modules:
  - migrator
  - seed_random
  - bootcmd
  - write-files
  - growpart
  - resizefs
  - disk_setup
  - mounts
  - rsyslog
cloud_config_modules:
  - locale
  - timezone
  - runcmd
cloud_final_modules:
  - scripts-per-once
  - scripts-per-boot
  - scripts-per-instance
  - scripts-user
  - final-message
  - power-state-change
EOF

  # Set up user data files (reuse the function from init.sh)
  mkdir -p /var/lib/cloud/seed/nocloud

  # Decode and decompress user data
  local decoded_userdata
  decoded_userdata=$(echo "$encoded_userdata" | base64 -d 2>/dev/null | gunzip 2>/dev/null)
  if [ $? -ne 0 ] || [ -z "$decoded_userdata" ]; then
    log_it "ERROR: Failed to decode or decompress user data"
    return 1
  fi

  # Write user data
  echo "$decoded_userdata" > /var/lib/cloud/seed/nocloud/user-data
  chmod 600 /var/lib/cloud/seed/nocloud/user-data

  # Create meta-data
  local instance_name=$(grep -Po 'name=\K[^[:space:]]*' "$CMDLINE" || hostname)
  cat > /var/lib/cloud/seed/nocloud/meta-data << EOF
instance-id: $instance_name
local-hostname: $instance_name
EOF
  chmod 644 /var/lib/cloud/seed/nocloud/meta-data

  log_it "User data files created, executing cloud-init..."

  # Run cloud-init stages manually
  cloud-init init --local && \
  cloud-init init && \
  cloud-init modules --mode=config && \
  cloud-init modules --mode=final

  local cloud_init_result=$?
  if [ $cloud_init_result -eq 0 ]; then
    log_it "Cloud-init executed successfully"
  else
    log_it "ERROR: Cloud-init execution failed with exit code: $cloud_init_result"
  fi

  return $cloud_init_result
}

if [ "$TYPE" == "router" ] || [ "$TYPE" == "vpcrouter" ] || [ "$TYPE" == "dhcpsrvr" ]
then
  if [ -x /opt/cloud/bin/update_config.py ]
  then
    /opt/cloud/bin/update_config.py cmd_line.json || true
  fi
fi

if [ "$TYPE" == "cksnode" ] || [ "$TYPE" == "sharedfsvm" ]; then
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

run_cloud_init

date > /var/cache/cloud/boot_up_done
