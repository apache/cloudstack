#!/usr/bin/env bash
# Private downstream build dependencies (Ubuntu/Debian).
# Aligns loosely with Apache CloudStack GitHub Actions build workflow.

set -euo pipefail

if [[ "${EUID:-$(id -u)}" -ne 0 ]]; then
  SUDO="sudo"
else
  SUDO=""
fi

$SUDO apt-get update
$SUDO apt-get install -y \
  git \
  uuid-runtime \
  genisoimage \
  netcat-openbsd \
  ipmitool \
  build-essential \
  libgcrypt20 \
  libgpg-error-dev \
  libgpg-error0 \
  libopenipmi0 \
  libpython3-dev \
  libssl-dev \
  libffi-dev \
  python3-openssl \
  python3-dev \
  python3-setuptools \
  wget \
  unzip
