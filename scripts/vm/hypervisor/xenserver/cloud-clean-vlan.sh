#!/bin/bash
# Version @VERSION@

#set -x


for vlan in $(xe vlan-list  | grep ^uuid | awk '{print $NF}'); do xe vlan-destroy uuid=$vlan 2&>/dev/null; done
for networkname in $(xe network-list | grep "name-label ( RW): VLAN" | awk '{print $NF}'); do network=$(xe network-list name-label=$networkname --minimal);  xe network-destroy uuid=$network 2&>/dev/null; done
