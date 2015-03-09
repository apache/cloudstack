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


 
# edithosts.sh -- edit the dhcphosts file on the routing domain

usage() {
  printf "Usage: %s: -m <MAC address> -4 <IPv4 address> -6 <IPv6 address> -h <hostname> -d <default router> -n <name server address> -s <Routes> -u <DUID> [-N]\n" $(basename $0) >&2
}

mac=
ipv4=
ipv6=
host=
dflt=
dns=
routes=
duid=
nondefault=

while getopts 'm:4:h:d:n:s:6:u:N' OPTION
do
  case $OPTION in
  m)    mac="$OPTARG"
        ;;
  4)    ipv4="$OPTARG"
        ;;
  6)    ipv6="$OPTARG"
        ;;
  u)    duid="$OPTARG"
        ;;
  h)    host="$OPTARG"
        ;;
  d)    dflt="$OPTARG"
        ;;
  n)    dns="$OPTARG"
        ;;
  s)    routes="$OPTARG"
        ;;
  N)    nondefault=1
        ;;
  ?)    usage
        exit 2
        ;;
  esac
done

DHCP_HOSTS=/etc/dhcphosts.txt
DHCP_OPTS=/etc/dhcpopts.txt
DHCP_LEASES=/var/lib/misc/dnsmasq.leases
HOSTS=/etc/hosts

source /root/func.sh

lock="biglock"
#default timeout value is 30 mins as DhcpEntryCommand is not synchronized on agent side any more,
#and multiple commands can be sent to the same VR at a time
locked=$(getLockFile $lock 1800)
if [ "$locked" != "1" ]
then
    exit 1
fi

grep "redundant_router=1" /var/cache/cloud/cmdline > /dev/null
no_redundant=$?

dnsmasq_managed_lease=`cat /var/cache/cloud/dnsmasq_managed_lease`

wait_for_dnsmasq () {
  local _pid=$(pidof dnsmasq)
  for i in 0 1 2 3 4 5 6 7 8 9 10
  do
    sleep 1
    _pid=$(pidof dnsmasq)
    [ "$_pid" != "" ] && break;
  done
  [ "$_pid" != "" ] && return 0;
  logger -t cloud "edithosts: timed out waiting for dnsmasq to start"
  return 1
}

if [ $dnsmasq_managed_lease ]
then
  #release previous dhcp lease if present
  logger -t cloud "edithosts: releasing $ipv4"
  dhcp_release $(ip route get "$ipv4/32" | grep " dev " | sed -e "s/^.* dev \([^ ]*\) .*$/\1/g") $ipv4 $(grep "$ipv4 " $DHCP_LEASES | awk '{print $2}') > /dev/null 2>&1
  logger -t cloud "edithosts: released $ipv4"
fi

logger -t cloud "edithosts: update $mac $ipv4 $ipv6 $host to hosts"

[ ! -f $DHCP_HOSTS ] && touch $DHCP_HOSTS
[ ! -f $DHCP_OPTS ] && touch $DHCP_OPTS
[ ! -f $DHCP_LEASES ] && touch $DHCP_LEASES

#delete any previous entries from the dhcp hosts file
sed -i  /$mac/d $DHCP_HOSTS
if [ $ipv4 ]
then
  sed -i  /$ipv4,/d $DHCP_HOSTS
fi
if [ $ipv6 ]
then
  #searching with [$ipv6], matching other ip so using $ipv6],
  sed -i  /$ipv6],/d $DHCP_HOSTS
fi
# don't want to do this in the future, we can have same VM with multiple nics/entries
sed -i  /$host,/d $DHCP_HOSTS

#put in the new entry
if [ $ipv4 ]
then
  echo "$mac,$ipv4,$host,infinite" >>$DHCP_HOSTS
fi
if [ $ipv6 ]
then
  if [ $nondefault ]
  then
    echo "id:$duid,set:nondefault6,[$ipv6],$host,infinite" >>$DHCP_HOSTS
  else
    echo "id:$duid,[$ipv6],$host,infinite" >>$DHCP_HOSTS
  fi
fi

if [ $dnsmasq_managed_lease -eq 0 ]
then
  #delete leases to supplied mac and ip addresses
  if [ $ipv4 ]
  then
    sed -i  /$mac/d $DHCP_LEASES
    sed -i  /"$ipv4 "/d $DHCP_LEASES
  fi
  if [ $ipv6 ]
  then
    sed -i  /$duid/d $DHCP_LEASES
    sed -i  /"$ipv6 "/d $DHCP_LEASES
  fi
  sed -i  /"$host "/d $DHCP_LEASES

  #put in the new entry
  if [ $ipv4 ]
  then
    echo "0 $mac $ipv4 $host *" >> $DHCP_LEASES
  fi
  if [ $ipv6 ]
  then
    echo "0 $duid $ipv6 $host *" >> $DHCP_LEASES
  fi
fi

#edit hosts file as well
if [ $ipv4 ]
then
  sed -i  /"$ipv4 "/d $HOSTS
fi
if [ $ipv6 ]
then
  sed -i  /"$ipv6 "/d $HOSTS
fi
sed -i  /" $host$"/d $HOSTS
if [ $ipv4 ]
then
  echo "$ipv4 $host" >> $HOSTS
fi
if [ $ipv6 ]
then
  echo "$ipv6 $host" >> $HOSTS
fi

if [ "$dflt" != "" -a "$ipv4" != "" ]
then
  #make sure dnsmasq looks into options file
  sed -i /dhcp-optsfile/d /etc/dnsmasq.conf
  echo "dhcp-optsfile=$DHCP_OPTS" >> /etc/dnsmasq.conf

  tag=$(echo $ipv4 | tr '.' '_')
  sed -i /$tag,/d $DHCP_OPTS
  if [ "$dflt" == "0.0.0.0" ]
  then
    logger -t cloud "$0: unset default router for $ipv4"
    logger -t cloud "$0: unset dns server for $ipv4"
    echo "$tag,3" >> $DHCP_OPTS
    echo "$tag,6" >> $DHCP_OPTS
    echo "$tag,15" >> $DHCP_OPTS
  fi
  [ "$routes" != "" ] && echo "$tag,121,$routes" >> $DHCP_OPTS
  #delete entry we just put in because we need a tag
  sed -i  /$ipv4,/d $DHCP_HOSTS
  #put it back with a tag
  echo "$mac,set:$tag,$ipv4,$host,infinite" >>$DHCP_HOSTS
fi

# make dnsmasq re-read files
pid=$(pidof dnsmasq)
if [ "$pid" != "" ]
then
  # use SIGHUP to avoid service outage if dhcp_release is available.
  if [ $dnsmasq_managed_lease ]
  then
    kill -HUP $pid
  else
    service dnsmasq restart
  fi
else
  if [ $no_redundant -eq 1 ]
  then
      wait_for_dnsmasq
  else
      logger -t cloud "edithosts: skip wait dnsmasq due to redundant virtual router"
  fi
fi

ret=$?
unlock_exit $ret $lock $locked
