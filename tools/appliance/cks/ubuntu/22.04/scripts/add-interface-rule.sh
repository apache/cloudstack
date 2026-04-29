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

# File and rule definition
RULE_FILE="/etc/udev/rules.d/90-new-interface.rules"
RULE='ACTION=="add|change|remove", SUBSYSTEM=="net", DRIVERS=="?*", RUN+="/bin/systemctl --no-block start update-netplan.service"'

# Ensure the file exists, or create it
if [[ ! -f $RULE_FILE ]]; then
    touch "$RULE_FILE"
    echo "Created $RULE_FILE."
fi

# Check if the rule already exists to prevent duplication
if grep -Fxq "$RULE" "$RULE_FILE"; then
    echo "Rule already exists in $RULE_FILE."
else
    # Add the rule to the file
    echo "$RULE" | tee -a "$RULE_FILE" > /dev/null
    echo "Rule added to $RULE_FILE."
fi

# Reload udev rules and apply the changes
udevadm control --reload-rules
udevadm trigger
echo "Udev rules reloaded and triggered."
