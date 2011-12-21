#!/bin/bash

ip link|grep BROADCAST|grep -v eth0|grep -v eth1|cut -d ":" -f 2 > /tmp/iflist
while read i
do
    ifconfig $i down
done < /tmp/iflist
service dnsmasq stop
