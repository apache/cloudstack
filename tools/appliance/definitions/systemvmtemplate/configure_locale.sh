fix_locale() {
  cat >> /etc/default/locale  << EOF
LANG=en_US.UTF-8
LC_ALL=en_US.UTF-8
EOF
  cat >> /etc/locale.gen  << EOF
en_US.UTF-8 UTF-8
EOF

  locale-gen en_US.UTF-8
}

fix_locale
