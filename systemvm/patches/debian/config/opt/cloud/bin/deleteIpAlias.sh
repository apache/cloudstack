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

remove_apache_config() {
local ip=$1
 logger -t cloud "removing apache web server config for $ip"
 rm -f "/etc/apache2/sites-available/ipAlias.${ip}.meta-data"
 rm -f "/etc/apache2/sites-available/ipAlias.${ip}-ssl.meta-data"
 rm -f "/etc/apache2/conf.d/ports.${ip}.meta-data.conf"
 rm -f "/etc/apache2/sites-enabled/ipAlias.${ip}-ssl.meta-data"
 rm -f "/etc/apache2/sites-enabled/ipAlias.${ip}.meta-data"
}

var="$1"
cert="/root/.ssh/id_rsa.cloud"

while [[ !( "$var" == "-" ) ]]
do
 var1=$(echo $var | cut -f1 -d "-")
 alias_count=$( echo $var1 | cut -f1 -d ":" )
 routerip=$( echo $var1 | cut -f2 -d ":" )
 ifconfig eth0:$alias_count  down
 remove_apache_config "$routerip"
 var=$( echo $var | sed "s/${var1}-//" )
done
#restarting the apache server for the config to take effect.
service apache2 restart

releaseLockFile $lock $locked

iptables -F dnsIpAlias_allow

#recreating the active ip aliases
/opt/cloud/bin/createIpAlias.sh $2
unlock_exit $? $lock $locked