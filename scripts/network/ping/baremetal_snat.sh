#!/bin/bash

set +u

mgmt_nic_ip=$1
internal_server_ip=$2

ip route | grep "$internal_server_ip" > /dev/null

if [ $? -ne 0 ]; then
    ip route add $internal_server_ip via $mgmt_nic_ip
fi

iptables-save | grep -- "-A POSTROUTING -d $internal_server_ip" > /dev/null

if [ $? -ne 0 ]; then
    iptables -t nat -A POSTROUTING -d $internal_server_ip -j SNAT --to-source $mgmt_nic_ip
fi