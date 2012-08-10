#!/bin/bash

if [ -z $1 ]
then
    echo "Fail to find VPN peer address!"
    exit 1
fi

ipsec auto --status | grep vpn-$1 > /tmp/vpn-$1.status

cat /tmp/vpn-$1.status | grep "ISAKMP SA established" > /dev/null
isakmpok=$?
if [ $isakmpok -ne 0 ]
then
    echo -n "ISAKMP SA NOT found but checking IPsec;"
else
    echo -n "ISAKMP SA found;"
fi

cat /tmp/vpn-$1.status | grep "IPsec SA established" > /dev/null
ipsecok=$?
if [ $ipsecok -ne 0 ]
then
    echo -n "IPsec SA not found;"
    echo "Site-to-site VPN have not connected"
    exit 11
fi
echo -n "IPsec SA found;"
echo "Site-to-site VPN have connected"
exit 0
