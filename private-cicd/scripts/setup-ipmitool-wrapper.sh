#!/usr/bin/env bash
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

# Optional CloudStack-style ipmitool wrapper (matches upstream CI expectations).
set -euo pipefail

if [[ "${EUID:-$(id -u)}" -ne 0 ]]; then
  SUDO="sudo"
else
  SUDO=""
fi

$SUDO mkdir -p /usr/share/cloudstack-common
if [[ ! -f /usr/share/cloudstack-common/ipmitool ]]; then
  $SUDO cp /usr/bin/ipmitool /usr/share/cloudstack-common/ipmitool
  $SUDO chmod 755 /usr/share/cloudstack-common/ipmitool
fi

$SUDO tee /usr/bin/ipmitool > /dev/null << 'EOF'
#!/bin/bash
/usr/share/cloudstack-common/ipmitool -C3 "$@"
EOF
$SUDO chmod 755 /usr/bin/ipmitool
