#!/usr/bin/env bash
# clearUsageRules.sh - remove iptable rules for removed public interfaces
#
#
# @VERSION@

# if removedVifs file doesn't exist, no rules to be cleared
if [ -f /root/removedVifs ]
then
    var=`cat /root/removedVifs`
    # loop through even vif to be cleared
    for i in $var; do
        # Make sure vif doesn't exist
        if [ ! -f /sys/class/net/$i ]
        then
            # remove rules
            iptables -D NETWORK_STATS -i eth0 -o $i > /dev/null;
            iptables -D NETWORK_STATS -i $i -o eth0 > /dev/null;
            iptables -D NETWORK_STATS -o $pubIf ! -i eth0 -p tcp > /dev/null;
            iptables -D NETWORK_STATS -i $pubIf ! -o eth0 -p tcp > /dev/null;
        fi
    done
rm /root/removedVifs
fi
