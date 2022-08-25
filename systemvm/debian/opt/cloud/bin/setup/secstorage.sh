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

. /opt/cloud/bin/setup/common.sh

setup_secstorage() {
  log_it "Setting up secondary storage system vm"

  echo "cloud apache2 nfs-common portmap" > /var/cache/cloud/enabled_svcs
  echo "conntrackd keepalived haproxy dnsmasq" > /var/cache/cloud/disabled_svcs
  mkdir -p /var/log/cloud

  setup_storage_network
  setup_system_rfc1918_internal

  log_it "Setting up entry in hosts"
  sed -i /$NAME/d /etc/hosts
  public_ip=`getPublicIp`
  echo "$public_ip $NAME" >> /etc/hosts

  log_it "Applying iptables rules"
  cp /etc/iptables/iptables-secstorage /etc/iptables/rules.v4

  log_it "Configuring apache2"
  setup_apache2 $ETH2_IP

  # Deprecated, should move to Cs Python all of it
  sed -e "s/<VirtualHost .*:8180>/<VirtualHost $ETH2_IP:80>/" \
    -e "s/<VirtualHost .*:8443>/<VirtualHost $ETH2_IP:443>/" \
    -e "s/Listen .*:8180/Listen $ETH2_IP:80/g" \
    -e "s/Listen .*:8443/Listen $ETH2_IP:443/g" /etc/apache2/vhost.template > /etc/apache2/sites-enabled/vhost-${ETH2_IP}.conf

  log_it "Setting up apache2 for post upload of volume/template"
  a2enmod proxy
  a2enmod proxy_http
  a2enmod headers

  if [ -z $USEHTTPS ] | $USEHTTPS ; then
    if [ -f /etc/apache2/http.conf ]; then
      rm -rf /etc/apache2/http.conf
    fi
      cat >/etc/apache2/https.conf <<HTTPS
RewriteEngine On
RewriteCond %{HTTPS} =on
RewriteCond %{REQUEST_METHOD} =POST
RewriteRule ^/upload/(.*) http://127.0.0.1:8210/upload?uuid=\$1 [P,L]
Header always set Access-Control-Allow-Origin "*"
Header always set Access-Control-Allow-Methods "POST, OPTIONS"
Header always set Access-Control-Allow-Headers "x-requested-with, Content-Type, origin, authorization, accept, client-security-token, x-signature, x-metadata, x-expires"
HTTPS
  else
    if [ -f /etc/apache2/https.conf ]; then
      rm -rf /etc/apache2/https.conf
    fi
      cat >/etc/apache2/http.conf <<HTTP
RewriteEngine On
RewriteCond %{REQUEST_METHOD} =POST
RewriteRule ^/upload/(.*) http://127.0.0.1:8210/upload?uuid=\$1 [P,L]
Header always set Access-Control-Allow-Origin "*"
Header always set Access-Control-Allow-Methods "POST, OPTIONS"
Header always set Access-Control-Allow-Headers "x-requested-with, Content-Type, origin, authorization, accept, client-security-token, x-signature, x-metadata, x-expires"
HTTP
  fi


  disable_rpfilter
  enable_fwding 0
  enable_irqbalance 0
  setup_ntp

  rm -f /etc/logrotate.d/cloud
}

setup_secstorage
