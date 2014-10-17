#!/bin/bash
echo "changing sudoer.d for vagrant access"
echo "vagrant ALL=(ALL) ALL"
echo 'vagrant ALL=(ALL) ALL' > /etc/sudoers.d/vagrant
