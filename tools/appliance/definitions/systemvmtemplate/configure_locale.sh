#!/bin/bash

set -e
set -x

function configure_locale() {
  grep LANG=en_US.UTF-8 /etc/default/locale && \
      grep LC_ALL=en_US.UTF-8 /etc/default/locale && \
      grep "en_US.UTF-8 UTF-8" /etc/locale.gen &&
      return

  cat >> /etc/default/locale  << EOF
LANG=en_US.UTF-8
LC_ALL=en_US.UTF-8
EOF
  cat >> /etc/locale.gen  << EOF
en_US.UTF-8 UTF-8
EOF

  locale-gen en_US.UTF-8
}

return 2>/dev/null || configure_locale
