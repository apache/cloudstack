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



 

#setup for Guava hosts in the simulator

#1. 1 host per cluster
#2. 100 clusters in a pod

zone_query="GET  http://10.91.30.219/client/?command=createZone&networktype=Advanced&securitygroupenabled=false&name=Go&dns1=4.2.2.2&internaldns1=4.2.2.2&vlan=500-1000&guestcidraddress=10.1.1.0%2F24	HTTP/1.0\n\n"
echo -e $zone_query | nc -v -q 120 10.91.30.219 8096

pod_query="GET  http://10.91.30.219/client/?command=createPod&zoneId=1&name=Guava&netmask=255.255.0.0&startIp=172.1.2.2&endIp=172.1.255.252&gateway=172.1.2.1	HTTP/1.0\n\n"
echo -e $pod_query | nc -v -q 120 10.91.30.219 8096

vlan_query="GET http://10.91.30.219/client/?command=createVlanIpRange&forVirtualNetwork=true&zoneId=1&vlan=untagged&gateway=172.2.1.1&netmask=255.255.0.0&startip=172.2.1.2&endip=172.2.255.254	HTTP/1.0\n\n"
echo -e $vlan_query | nc -v -q 120 10.91.30.219 8096

for name in `seq 1 100`
do
	cluster_query="GET	http://10.91.30.219/client/?command=addCluster&hypervisor=Simulator&clustertype=CloudManaged&zoneId=1&podId=1&clustername=CS$name	HTTP/1.0\n\n"
	echo -e $cluster_query | nc -v -q 120 10.91.30.219 8096

	host_query="GET	http://10.91.30.219/client/api?_=1302625706202&command=addHost&zoneId=1&podId=1&clusterid=$name&hypervisor=Simulator&clustertype=CloudManaged&hosttags=&username=sim&password=sim&url=http%3A%2F%2Fsim	HTTP/1.0\n\n"
	echo -e $host_query | nc -v -q 60 10.91.30.219 8096

	spool_query="GET	http://10.91.30.219/client/?command=createStoragePool&zoneId=1&podId=1&clusterid=$name&name=SPOOL$name&url=nfs://172.1.25.$name/export/share/$name   HTTP/1.0\n\n"
	echo -e $spool_query | nc -v -q 60 10.91.30.219 8096
done
