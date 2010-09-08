#!/usr/bin/env bash
# edithosts.sh -- edit the dhcphosts file on the routing domain
# $1 : the mac address
# $2 : the associated ip address
# $3 : the hostname

wait_for_dnsmasq () {
  local _pid=$(/sbin/pidof dnsmasq)
  for i in 0 1 2 3 4 5 6 7 8 9 10
  do
    sleep 1
    _pid=$(/sbin/pidof dnsmasq)
    [ "$_pid" != "" ] && break;
  done
  [ "$_pid" != "" ] && return 0;
  echo "edithosts: timed out waiting for dnsmasq to start"
  return 1
}

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
pid=$(/sbin/pidof dnsmasq)
if [ "$pid" != "" ]
then
  service dnsmasq restart
else
  wait_for_dnsmasq
fi

exit $?
