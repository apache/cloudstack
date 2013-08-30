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
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.

usage() {
  printf "Usage: %s:  <routerAliasIp:gateway:netmask:start_ip_of_subnet:-routerAlisIp:gateway:....>\n" $(basename $0) >&2
}

source /root/func.sh

lock="biglock"
locked=$(getLockFile $lock)
if [ "$locked" != "1" ]
then
    exit 1
fi

#set -x
#backup the old config file
DHCP_CONFIG=/etc/dnsmasq.d/multiple_ranges.conf
DHCP_CONFIG_BAK=/etc/dnsmasq.d/multiple_ranges.conf.bak
DHCP_CONFIG_MAIN=/etc/dnsmasq.conf
DHCP_CONFIG_MAIN_BAK=/etc/dnsmasq.conf.bak
DHCP_FAILURE_CONFIG=/etc/multiple_ranges.conf.failure
DHCP_FAILURE_CONFIG_MAIN=/etc/dnsmasq.conf.failure
CMDLINE=$(cat /var/cache/cloud/cmdline | tr '\n' ' ')

#take a backup copy of the dnsmasq file.
cp "$DHCP_CONFIG_MAIN"  "$DHCP_CONFIG_MAIN_BAK"
cp "$DHCP_CONFIG" "$DHCP_CONFIG_BAK"

#empty the config file
echo > $DHCP_CONFIG

var="$1"
dhcp_range=""
dhcp_gateway=""
dhcp_netmask=""
dns_option=""
dns_servers=""
count=0


# fetching the dns Ips from the command line.
dns1=$(echo "$CMDLINE" | grep -o " dns1=[[:digit:]].* " | sed -e 's/dns1=//' | awk '{print $1}')
dns2=$(echo "$CMDLINE" | grep -o " dns2=[[:digit:]].* "  | sed -e 's/dns2=//' | awk '{print $1}')

dns_servers="${dns1}"
if [ -n "$dns2" ]
then
dns_servers="${dns1},${dns2}"
fi


# check if useextdns is true
use_ext_dns=$(echo "$CMDLINE" | grep -o "useextdns=true")
while [ -n "$var" ]
do
 var1=$(echo $var | cut -f1 -d "-")
 routerip=$( echo $var1 | cut -f1 -d ":" )
 gateway=$(echo $var1 | cut -f2 -d ":")
 netmask=$(echo $var1 | cut -f3 -d ":")
 start_ip_of_subnet=$(echo $var1 | cut -f4 -d ":")
 dhcp_range="${dhcp_range}"'dhcp-range=set:range'$count","$start_ip_of_subnet",static \n"
 dhcp_gateway="${dhcp_gateway}"'dhcp-option=tag:range'$count",3,"$gateway" \n"
 dhcp_netmask="${dhcp_netmask}"'dhcp-option=tag:range'$count",1,"$netmask" \n"
 if [ -n "$use_ext_dns" ]
 then
 dns_option="${dns_option}"'dhcp-option=tag:range'$count",6,"$dns_servers" \n"
 else
 dns_option="${dns_option}"'dhcp-option=tag:range'$count",6,$routerip"","$dns_servers" \n"
 fi
 var=$( echo $var | sed "s/${var1}-//" )
 count=$[$count+1]
done

#logging the configuration being removed.
log=""
log="${log}"`grep "^dhcp-option=6" "$DHCP_CONFIG_MAIN"`"\n"
log="${log}"`grep "^dhcp-option=option:router" "$DHCP_CONFIG_MAIN"`"\n"
log="${log}"`grep "^dhcp-range=" "$DHCP_CONFIG_MAIN"`"\n"

if [ "$log" != '\n\n\n' ]
then
 #Cleaning the existing dhcp confgiuration
 logger -t cloud "dnsmasq.sh: remvoing the primaryip confg from dnsmasq.conf and adding it to /etc/dnsmaq.d/multiple_ranges.conf"
 logger -t cloud "dnsmasq.sh: config removed from dnsmasq.conf is $log"
 sed -i -e '/dhcp-option=6/d'  "$DHCP_CONFIG_MAIN"
 sed -i -e '/dhcp-option=option:router/d' "$DHCP_CONFIG_MAIN"
 sed -i -e '/^dhcp-range=/d' "$DHCP_CONFIG_MAIN"
fi

#wrting the new config into the config file.
echo -e "$dhcp_range" >> "$DHCP_CONFIG"
echo -e "$dhcp_gateway" >> "$DHCP_CONFIG"
echo -e "$dhcp_netmask" >> "$DHCP_CONFIG"
echo -e "$dns_option" >> "$DHCP_CONFIG"


#restart the dnsmasq
service dnsmasq restart
result=$?
if [ "$result" -ne "0" ]
then
   logger -t cloud "dnsmasq.sh: could not configure dnsmasq"
   logger -t cloud "dnsmasq.sh: reverting to the old config"
   logger -t cloud "dnsmasq.sh: copying the failure config to `$DHCP_FAILURE_CONFIG` and `$DHCP_FAILURE_CONFIG_MAIN`"
   cp "$DHCP_CONFIG" "$DHCP_FAILURE_CONFIG"
   cp "$DHCP_CONFIG_MAIN" "$DHCP_FAILURE_CONFIG_MAIN"
   cp "$DHCP_CONFIG_BAK" "$DHCP_CONFIG"
   cp "$DHCP_CONFIG_MAIN_BAK" "$DHCP_CONFIG_MAIN"
   service dnsmasq restart
   unlock_exit $result $lock $locked
fi
rm "$DHCP_CONFIG_BAK"
rm "$DHCP_CONFIG_MAIN_BAK"
unlock_exit $result $lock $locked
