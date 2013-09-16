#!/usr/bin/env bash
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

usage() {
  printf " %s   <alias_count:ip:netmask;alias_count2:ip2:netmask2;....> \n" $(basename $0) >&2
}
source /root/func.sh

lock="biglock"
locked=$(getLockFile $lock)
if [ "$locked" != "1" ]
then
    exit 1
fi

PORTS_CONF=/etc/apache2/ports.conf
PORTS_CONF_BAK=/etc/ports.conf.bak
FAIL_DIR=/etc/failure_config
CMDLINE=$(cat /var/cache/cloud/cmdline | tr '\n' ' ')

if [ ! -d "$FAIL_DIR" ]
  then
      mkdir "$FAIL_DIR"
fi
#bakup ports.conf
cp "$PORTS_CONF" "$PORTS_CONF_BAK"

domain=$(echo "$CMDLINE" | grep -o " domain=.* " | sed -e 's/domain=//' | awk '{print $1}')

setup_apache2() {
  local ip=$1
  logger -t cloud "Setting up apache web server for $ip"
  cp /etc/apache2/sites-available/default  /etc/apache2/sites-available/ipAlias.${ip}.meta-data
  cp /etc/apache2/sites-available/default-ssl  /etc/apache2/sites-available/ipAlias.${ip}-ssl.meta-data
  cp /etc/apache2/ports.conf /etc/apache2/conf.d/ports.${ip}.meta-data.conf
  sed -i -e "s/<VirtualHost.*>/<VirtualHost $ip:80>\nServerName $domain/" /etc/apache2/sites-available/ipAlias.${ip}.meta-data
  sed -i -e "s/<VirtualHost.*>/<VirtualHost $ip:443>\nServerName $domain/" /etc/apache2/sites-available/ipAlias.${ip}-ssl.meta-data
  sed -i -e "/NameVirtualHost .*:80/d" /etc/apache2/conf.d/ports.${ip}.meta-data.conf
  sed -i -e "s/Listen .*:80/Listen $ip:80/g" /etc/apache2/conf.d/ports.${ip}.meta-data.conf
  sed -i -e "s/Listen .*:443/Listen $ip:443/g" /etc/apache2/conf.d/ports.${ip}.meta-data.conf
  ln -s /etc/apache2/sites-available/ipAlias.${ip}.meta-data /etc/apache2/sites-enabled/ipAlias.${ip}.meta-data
  ln -s /etc/apache2/sites-available/ipAlias.${ip}-ssl.meta-data /etc/apache2/sites-enabled/ipAlias.${ip}-ssl.meta-data
}

var="$1"
cert="/root/.ssh/id_rsa.cloud"
config_ips=""

while [ -n "$var" ]
do
 var1=$(echo $var | cut -f1 -d "-")
 alias_count=$( echo $var1 | cut -f1 -d ":" )
 routerip=$(echo $var1 | cut -f2 -d ":")
 netmask=$(echo $var1 | cut -f3 -d ":")
 ifconfig eth0:$alias_count $routerip netmask $netmask up
 setup_apache2 "$routerip"
 config_ips="${config_ips}"$routerip":"
 var=$( echo $var | sed "s/${var1}-//" )
done

#restarting the apache server for the config to take effect.
service apache2 restart
result=$?
if [ "$result" -ne "0" ]
then
   logger -t cloud "createIpAlias.sh: could not configure apache2 server"
   logger -t cloud "createIpAlias.sh: reverting to the old config"
   logger -t cloud "createIpAlias.sh: moving out the failure config to $FAIL_DIR"
   while [ -n "$config_ips" ]
   do
      ip=$( echo $config_ips | cut -f1 -d ":" )
      mv  "/etc/apache2/sites-available/ipAlias.${ip}.meta-data" "$FAIL_DIR/ipAlias.${ip}.meta-data"
      mv  "/etc/apache2/sites-available/ipAlias.${ip}-ssl.meta-data" "$FAIL_DIR/ipAlias.${ip}-ssl.meta-data"
      mv  "/etc/apache2/conf.d/ports.${ip}.meta-data.conf"       "$FAIL_DIR/ports.${ip}.meta-data.conf"
      rm -f "/etc/apache2/sites-enabled/ipAlias.${ip}.meta-data"
      rm -f "/etc/apache2/sites-enabled/ipAlias.${ip}-ssl.meta-data"
      config_ips=$( echo $config_ips | sed "s/${ip}://" )
   done
   service apache2 restart
   unlock_exit $result $lock $locked
fi

#restaring the password service to enable it on the ip aliases
/etc/init.d/cloud-passwd-srvr restart
unlock_exit $? $lock $locked