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

# Create the script in the /opt/bin directory
SCRIPT_PATH="/usr/local/bin/update-netplan.sh"

cat <<'EOF' > $SCRIPT_PATH
#!/bin/bash

echo "New interface detected: $INTERFACE" >> /var/log/interface-events.log
CONFIG_FILE="/etc/netplan/config.yaml"

# Generate a new netplan configuration
echo "network:" > $CONFIG_FILE
echo "  ethernets:" >> $CONFIG_FILE

# Loop through all available interfaces
for iface in $(ls /sys/class/net | grep -vE '^lo$'); do
cat <<EOL >> $CONFIG_FILE
   $iface:
       dhcp4: true
EOL
done

chmod 600 $CONFIG_FILE

netplan apply
EOF

tee /etc/systemd/system/update-netplan.service <<EOF
[Unit]
Description=Update netplan configuration on boot
After=network.target

[Service]
Type=oneshot
ExecStart=/usr/local/bin/update-netplan.sh

[Install]
WantedBy=multi-user.target
EOF

chmod 600 /etc/netplan/config.yaml
chmod 777 $SCRIPT_PATH

systemctl daemon-reload || true
systemctl enable update-netplan.service || true
systemctl start update-netplan.service || true
