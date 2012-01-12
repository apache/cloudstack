#!/usr/bin/env bash
# Copyright (C) 2011 Citrix Systems, Inc.  All rights reserved
#     
# This software is licensed under the GNU General Public License v3 or later.
# 
# It is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation, either version 3 of the License, or any later version.
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
# 
# You should have received a copy of the GNU General Public License
# along with this program.  If not, see <http://www.gnu.org/licenses/>.
# 



 

# edithosts.sh -- edit the dhcphosts file on the routing domain
# $1 : the mac address
# $2 : the associated ip address
# $3 : the hostname

wait_for_dnsmasq () {
  local _pid=$(pidof dnsmasq)
  for i in 0 1 2 3 4 5 6 7 8 9 10
  do
    sleep 1
    _pid=$(pidof dnsmasq)
    [ "$_pid" != "" ] && break;
  done
  [ "$_pid" != "" ] && return 0;
  echo "edithosts: timed out waiting for dnsmasq to start"
  return 1
}

[ ! -f /etc/dhcphosts.txt ] && touch /etc/dhcphosts.txt
[ ! -f /var/lib/misc/dnsmasq.leases ] && touch /var/lib/misc/dnsmasq.leases

#delete any previous entries from the dhcp hosts file
sed -i  /$1/d /etc/dhcphosts.txt 
sed -i  /$2,/d /etc/dhcphosts.txt 
sed -i  /$3,/d /etc/dhcphosts.txt 

#put in the new entry
echo "$1,$2,$3,infinite" >>/etc/dhcphosts.txt

#delete leases to supplied mac and ip addresses
sed -i  /$1/d /var/lib/misc/dnsmasq.leases 
sed -i  /"$2 "/d /var/lib/misc/dnsmasq.leases 
sed -i  /"$3 "/d /var/lib/misc/dnsmasq.leases 

#put in the new entry
echo "0 $1 $2 $3 *" >> /var/lib/misc/dnsmasq.leases

#edit hosts file as well
sed -i  /"$2 "/d /etc/hosts
sed -i  /"$3"/d /etc/hosts
echo "$2 $3" >> /etc/hosts

# make dnsmasq re-read files
pid=$(pidof dnsmasq)
if [ "$pid" != "" ]
then
  # send SIGHUP to dnsmasq to reload /etc/hosts /etc/dhcphosts.txt
  # this will not reload /etc/dnsmasq.conf
  kill -s 1 $pid
else
  service dnsmasq start
  wait_for_dnsmasq
fi

exit $?
