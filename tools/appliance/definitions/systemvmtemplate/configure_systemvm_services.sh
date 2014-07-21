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

set -x

ROOTPW=password
HOSTNAME=systemvm
CLOUDSTACK_RELEASE=4.5.0

configure_apache2() {
   # Enable ssl, rewrite and auth
   a2enmod ssl rewrite auth_basic auth_digest
   a2ensite default-ssl
   # Backup stock apache configuration since we may modify it in Secondary Storage VM
   cp /etc/apache2/sites-available/default /etc/apache2/sites-available/default.orig
   cp /etc/apache2/sites-available/default-ssl /etc/apache2/sites-available/default-ssl.orig
}

configure_services() {
  local arch=`dpkg --print-architecture`
  mkdir -p /var/www/html
  mkdir -p /opt/cloud/bin
  mkdir -p /var/cache/cloud
  mkdir -p /usr/share/cloud
  mkdir -p /usr/local/cloud
  mkdir -p /root/.ssh
  # Fix haproxy directory issue
  mkdir -p /var/lib/haproxy

  # Get config files from master
  snapshot_url="https://git-wip-us.apache.org/repos/asf?p=cloudstack.git;a=snapshot;h=HEAD;sf=tgz"
  snapshot_dir="/opt/cloudstack*"
  cd /opt
  wget --no-check-certificate $snapshot_url -O cloudstack.tar.gz
  tar -zxvf cloudstack.tar.gz --wildcards 'cloudstack-HEAD-???????/systemvm'
  cp -rv $snapshot_dir/systemvm/patches/debian/config/* /
  cp -rv $snapshot_dir/systemvm/patches/debian/vpn/* /
  mkdir -p /usr/share/cloud/
  cd $snapshot_dir/systemvm/patches/debian/config
  tar -cvf /usr/share/cloud/cloud-scripts.tar *
  cd $snapshot_dir/systemvm/patches/debian/vpn
  tar -rvf /usr/share/cloud/cloud-scripts.tar *
  cd /opt
  rm -fr $snapshot_dir cloudstack.tar.gz

  chkconfig --add cloud-early-config
  chkconfig cloud-early-config on
  chkconfig --add cloud-passwd-srvr
  chkconfig cloud-passwd-srvr off
  chkconfig --add cloud
  chkconfig cloud off
  chkconfig xl2tpd off
  # Hyperv  kvp daemon - 64bit only
  if [ "${arch}" == "amd64" ]; then
    chkconfig hv_kvp_daemon off
  fi
  chkconfig radvd off
}

do_signature() {
  mkdir -p /var/cache/cloud/
  gzip -c /usr/share/cloud/cloud-scripts.tar > /usr/share/cloud/cloud-scripts.tgz
  md5sum /usr/share/cloud/cloud-scripts.tgz | awk '{print $1}' > /var/cache/cloud/cloud-scripts-signature
  echo "Cloudstack Release $CLOUDSTACK_RELEASE $(date)" > /etc/cloudstack-release
}

begin=$(date +%s)

configure_services
configure_apache2
echo "*************DONE SETTING UP SERVICES********************"
do_signature

fin=$(date +%s)
t=$((fin-begin))

echo "Signed systemvm build, finished building systemvm appliance in $t seconds"
