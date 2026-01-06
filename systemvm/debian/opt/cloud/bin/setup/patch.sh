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
PATH="/usr/local/sbin:/usr/local/bin:/sbin:/bin:/usr/sbin:/usr/bin"

log_it() {
  echo "$(date) $@" >> /var/log/cloud.log
}

patch_sshd_config() {
  if `! ssh -Q PubkeyAcceptedAlgorithms >/dev/null 2>&1` && `grep ^PubkeyAcceptedAlgorithms /etc/ssh/sshd_config >/dev/null`; then
      # "PubkeyAcceptedAlgorithms=+ssh-rsa" is added to /etc/ssh/sshd_config in 4.20.0 systemvm template
      # However, it is not supported in old systemvm templates
      # If the system vm is created from an old systemvm template, remove it from /etc/ssh/sshd_config
      # No need to restart ssh if it is running well
      log_it "Removing PubkeyAcceptedAlgorithms=+ssh-rsa from /etc/ssh/sshd_config as it is not supported"
      sed -i "/PubkeyAcceptedAlgorithms=+ssh-rsa/d" /etc/ssh/sshd_config
      if ! systemctl is-active ssh > /dev/null; then
        systemctl restart ssh
      fi
  elif `ssh -Q PubkeyAcceptedAlgorithms >/dev/null 2>&1` && `! grep ^PubkeyAcceptedAlgorithms /etc/ssh/sshd_config >/dev/null`; then
      log_it "Adding PubkeyAcceptedAlgorithms=+ssh-rsa to sshd_config"
      sed -i "/PubkeyAuthentication yes/aPubkeyAcceptedAlgorithms=+ssh-rsa" /etc/ssh/sshd_config
      systemctl restart ssh
  fi
}

patch_router() {
  local patchfile="/var/cache/cloud/agent.zip"
  local logfile="/var/log/patchrouter.log"
  rm /usr/local/cloud/systemvm -rf
  mkdir -p /usr/local/cloud/systemvm
  ls -lrt $patchfile

  log_it "Unziping $patchfile"
  echo "All" | unzip $patchfile -d /usr/local/cloud/systemvm >>$logfile 2>&1

  find /usr/local/cloud/systemvm/ -name \*.sh | xargs chmod 555

  patch_sshd_config
  install_packages
}

patch_system_vm() {
  patch_sshd_config
  install_packages
}

install_packages() {
  PACKAGES_FOLDER="/usr/local/cloud/systemvm/packages"
  PACKAGES_INI="$PACKAGES_FOLDER/packages.ini"
  declare -A package_properties
  if [ -d $PACKAGES_FOLDER ] && [ -f $PACKAGES_INI ]; then
    while read -r line; do
      if [[ "$line" =~ ^(\[)(.*)(\])$ ]]; then
        install_package
        package_properties=
      else
        key=$(echo $line | cut -d '=' -f1)
        value=$(echo $line | cut -d '=' -f2)
        if [ "$key" != "" ]; then
          package_properties[$key]=$value
        fi
      fi
    done <$PACKAGES_INI
  fi
  export DEBIAN_FRONTEND=noninteractive
  install_package
}

install_package() {
  local os=${package_properties["debian_os"]}
  if [ "$os" == "" ]; then
    return
  fi

  local package=${package_properties["package_name"]}
  local file=${package_properties["file_name"]}

  local DEBIAN_RELEASE=$(lsb_release -rs)
  if [ "$os" != "$DEBIAN_RELEASE" ]; then
    log_it "Skipped the installation of package $package on Debian $DEBIAN_RELEASE as it can only be installed on Debian $os."
    return
  fi

  if [ -z "$package" ] || [ -z "$file" ]; then
    log_it "Skipped the installation due to empty package of file name (package name: $package, file name: $file)."
    return
  fi

  dpkg-query -s $package >/dev/null 2>&1
  if [ $? -eq 0 ]; then
    log_it "Skipped the installation as package $package has already been installed."
    return
  fi

  local conflicts=${package_properties["conflicted_packages"]}
  if [ "$conflicts" != "" ]; then
    log_it "Removing conflicted packages \"$conflicts\" before installing package $package"
    apt remove -y "$conflicts"
    if [ $? -eq 0 ]; then
      log_it "Removed conflicted package(s) \"$conflicts\" before installing package $package"
    else
      log_it "Failed to remove conflicted package(s) \"$conflicts\" before installing package $package"
    fi
  fi

  PACKAGES_FOLDER="/usr/local/cloud/systemvm/packages"
  log_it "Installing package $package from file $PACKAGES_FOLDER/$file"
  dpkg -i $PACKAGES_FOLDER/$file
  if [ $? -eq 0 ]; then
    log_it "Installed package $package from file $PACKAGES_FOLDER/$file"
  else
    log_it "Failed to install package $package from file $PACKAGES_FOLDER/$file"
  fi
}
