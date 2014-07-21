#!/bin/bash

set -e
set -x

function configure_acpid() {
  grep /usr/local/sbin/power.sh /etc/acpi/events/power && return

  mkdir -p /etc/acpi/events
  cat >> /etc/acpi/events/power << EOF
event=button/power.*
action=/usr/local/sbin/power.sh "%e"
EOF
  cat >> /usr/local/sbin/power.sh << EOF
#!/bin/bash
/sbin/poweroff
EOF
  chmod a+x /usr/local/sbin/power.sh
}

return 2>/dev/null || configure_acpid
