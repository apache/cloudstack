#!/bin/bash
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
# Version @VERSION@

#set -x


for vlan in $(xe vlan-list  | grep ^uuid | awk '{print $NF}'); do xe vlan-destroy uuid=$vlan 2&>/dev/null; done
for networkname in $(xe network-list | grep "name-label ( RW): VLAN" | awk '{print $NF}'); do network=$(xe network-list name-label=$networkname --minimal);  xe network-destroy uuid=$network 2&>/dev/null; done
