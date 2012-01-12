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



 

# clearUsageRules.sh - remove iptable rules for removed public interfaces
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
            iptables -D NETWORK_STATS -o $i ! -i eth0 -p tcp > /dev/null;
            iptables -D NETWORK_STATS -i $i ! -o eth0 -p tcp > /dev/null;
        fi
    done
rm /root/removedVifs
fi
