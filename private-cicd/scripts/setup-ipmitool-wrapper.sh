#!/usr/bin/env bash
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
