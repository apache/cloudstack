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

set -e
set -x

CLOUDSTACK_RELEASE=4.6.0

function configure_apache2() {
   # Enable ssl, rewrite and auth
   a2enmod ssl rewrite auth_basic auth_digest
   a2ensite default-ssl
   # Backup stock apache configuration since we may modify it in Secondary Storage VM
   cp /etc/apache2/sites-available/default /etc/apache2/sites-available/default.orig
   cp /etc/apache2/sites-available/default-ssl /etc/apache2/sites-available/default-ssl.orig
   sed -i 's/SSLProtocol all -SSLv2$/SSLProtocol all -SSLv2 -SSLv3/g' /etc/apache2/mods-available/ssl.conf
}

function install_cloud_scripts() {
  # ./cloud_scripts/ has been put there by ../../cloud_scripts_shar_archive.sh
  rsync -av ./cloud_scripts/ /
  chmod +x /opt/cloud/bin/* \
    /root/{clearUsageRules.sh,reconfigLB.sh,monitorServices.py} \
    /etc/init.d/{cloud,cloud-early-config,cloud-passwd-srvr,postinit} \
    /etc/cron.daily/cloud-cleanup \
    /etc/profile.d/cloud.sh

  chkconfig --add cloud-early-config
  chkconfig cloud-early-config on
  chkconfig --add cloud-passwd-srvr
  chkconfig cloud-passwd-srvr off
  chkconfig --add cloud
  chkconfig cloud off
}

function do_signature() {
  mkdir -p /var/cache/cloud/ /usr/share/cloud/
  (cd ./cloud_scripts/; tar -cvf - * | gzip > /usr/share/cloud/cloud-scripts.tgz)
  md5sum /usr/share/cloud/cloud-scripts.tgz | awk '{print $1}' > /var/cache/cloud/cloud-scripts-signature
  echo "Cloudstack Release $CLOUDSTACK_RELEASE $(date)" > /etc/cloudstack-release
}

function configure_services() {
  mkdir -p /var/www/html
  mkdir -p /opt/cloud/bin
  mkdir -p /var/cache/cloud
  mkdir -p /usr/share/cloud
  mkdir -p /usr/local/cloud

  # Fix haproxy directory issue
  mkdir -p /var/lib/haproxy

  install_cloud_scripts
  do_signature

  chkconfig xl2tpd off

  # Disable services that slow down boot and are not used anyway
  chkconfig x11-common off
  chkconfig console-setup off

  # Hyperv kvp daemon - 64bit only
  local arch=`dpkg --print-architecture`
  if [ "${arch}" == "amd64" ]; then
    chkconfig hv_kvp_daemon off
  fi
  chkconfig radvd off

  configure_apache2
}

return 2>/dev/null || configure_services
