fix_acpid() {
  # Fix acpid
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

fix_acpid
