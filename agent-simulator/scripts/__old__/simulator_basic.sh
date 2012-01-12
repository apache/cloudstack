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

a=$1 #CIDR - 16bytes
b=$2 #CIDR - 8 bytes
host=$3 #MSHOST
zoneid=$4

zone_query="GET  http://$host/client/?command=createZone&networktype=Basic&securitygroupenabled=false&name=SimulatorBasic&dns1=4.2.2.2&internaldns1=4.2.2.2&vlan=10-4000  HTTP/1.0\n\n"
echo -e $zone_query | nc -v -w 120 $host 8096

let x=a
let y=b

for name in `seq $zoneid 1`
do
	pod_query="GET  http://$host/client/?command=createPod&zoneId=$zoneid&name=POD$name&netmask=255.255.255.0&startIp=172.$x.$y.2&endIp=172.$x.$y.252&gateway=172.$x.$y.1	HTTP/1.0\n\n"
	vlan_query="GET http://$host/client/?command=createVlanIpRange&vlan=untagged&zoneid=$zoneid&podId=$name&forVirtualNetwork=false&gateway=172.$y.$x.1&netmask=255.255.255.0&startip=172.$y.$x.2&endip=172.$y.$x.252        HTTP/1.0\n\n"
	echo -e $pod_query | nc -v -w 20 $host 8096
	echo -e $vlan_query | nc -v -w 20 $host 8096

	let x+=1
	let y+=1
done

clusterid=1

for podid in `seq $zoneid 1`
do
		cluster_query="GET  http://$host/client/?command=addCluster&hypervisor=Simulator&clustertype=CloudManaged&zoneid=$zoneid&podId=$podid&clustername=POD$name-CLUSTER$cluster HTTP/1.0\n\n"
		echo -e $cluster_query | nc -v -w 120 $host 8096

		host_query="GET	http://$host/client/?command=addHost&zoneId=$zoneid&podId=$podid&username=sim&password=sim&clusterid=$clusterid&url=http%3A%2F%2Fsim&hypervisor=Simulator&clustertype=CloudManaged	HTTP/1.0\n\n"
		echo -e $host_query | nc -v -w 6000 $host 8096
	let clusterid+=1
done

sstorquery="GET	http://$host/client/?command=addSecondaryStorage&zoneid=$zoneid&url=nfs://172.1.25.32/export/share/   HTTP/1.0\n\n"
echo -e $sstorquery | nc -v -w 6000 $host 8096
