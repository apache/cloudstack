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



 

#setup for zucchini hosts in the simulator

#1. Approximately 10 hosts per pod. 
#2. Only 3 host tags. 
#3. With in each pod, the host tags are the same.
#4. The ratio of hosts for the three tags should be 5/2/6

a=$1 #CIDR - 16bytes
b=$2 #CIDR - 8 bytes
host=$3 #MSHOST
zone=$4

zone_query="GET  http://$host/client/?command=createZone&name=Zucchini&dns1=4.2.2.2&internaldns1=4.2.2.2&vlan=10-4000&guestcidraddress=10.1.1.0%2F24  HTTP/1.0\n\n"
echo -e $zone_query | nc -v -w 120 $host 8096

# Pod Ratio: 38:15:47
let x=a
let y=b
for name in `seq 1 152`
do
	pod_query="GET  http://$host/client/?command=createPod&zoneId=$zone&name=POD$name&cidr=172.$x.$y.0%2F24&startIp=172.$x.$y.2&endIp=172.$x.$y.252&gateway=172.$x.$y.1	HTTP/1.0\n\n"
	vlan_query="GET http://$host/client/?command=createVlanIpRange&vlan=untagged&zoneid=$zone&podId=$name&forVirtualNetwork=false&gateway=172.$y.$x.1&netmask=255.255.255.0&startip=172.$y.$x.2&endip=172.$y.$x.252        HTTP/1.0\n\n"
	echo -e $pod_query | nc -v -w 20 $host 8096
	echo -e $vlan_query | nc -v -w 20 $host 8096

	let x+=1
	let y+=1
done


let x=a
let y=b
for name in `seq 153 212`
do
	pod_query="GET  http://$host/client/?command=createPod&zoneId=$zone&name=POD$name&cidr=182.$x.$y.0%2F24&startIp=182.$x.$y.2&endIp=182.$x.$y.252&gateway=182.$x.$y.1	HTTP/1.0\n\n"
	vlan_query="GET http://$host/client/?command=createVlanIpRange&vlan=untagged&zoneid=$zone&podId=$name&forVirtualNetwork=false&gateway=182.$y.$x.1&netmask=255.255.255.0&startip=182.$y.$x.2&endip=182.$y.$x.252        HTTP/1.0\n\n"
	echo -e $pod_query | nc -v -w 20 $host 8096
	echo -e $vlan_query | nc -v -w 20 $host 8096

	let x+=1
	let y+=1
done

let x=a
let y=b
for name in `seq 213 400`
do

	pod_query="GET  http://$host/client/?command=createPod&zoneId=$zone&name=POD$name&cidr=192.$x.$y.0%2F24&startIp=192.$x.$y.2&endIp=192.$x.$y.252&gateway=192.$x.$y.1	HTTP/1.0\n\n"
	vlan_query="GET http://$host/client/?command=createVlanIpRange&vlan=untagged&zoneid=$zone&podId=$name&forVirtualNetwork=false&gateway=192.$y.$x.1&netmask=255.255.255.0&startip=192.$y.$x.2&endip=192.$y.$x.252        HTTP/1.0\n\n"
	echo -e $pod_query | nc -v -w 20 $host 8096
	echo -e $vlan_query | nc -v -w 20 $host 8096

	let x+=1
	let y+=1
done

#add clusters
#for name in `seq 1 400`
#do
#	for cluster in `seq 1 10`
#	do
#		cluster_query="GET  http://$host/client/?command=addCluster&hypervisor=Simulator&clustertype=CloudManaged&zoneId=$zone&podId=$name&clustername=POD$name-CLUSTER$cluster HTTP/1.0\n\n"
#	        echo -e $cluster_query | nc -v -w 120 $host 8096
#	done
#done

# @ 10 hosts per pod @ 10 clusters per pod
clusterid=1
#TAG1
for podid in `seq 58 152`
do
	for i in `seq 1 10`
	do
		host_query="GET	http://$host/client/?command=addHost&zoneId=$zone&podId=$podid&username=sim&password=sim&clustername=CLUSTER$clusterid&hosttags=TAG1&url=http%3A%2F%2Fsim	HTTP/1.0\n\n"
		echo -e $host_query | nc -v -w 6000 $host 8096
	done
	let clusterid+=1
done

#TAG2
for podid in `seq 153 212`
do
	for i in `seq 1 10`
	do
		host_query="GET	http://$host/client/?command=addHost&zoneId=$zone&podId=$podid&username=sim&password=sim&clustername=CLUSTER$clusterid&hosttags=TAG2&url=http%3A%2F%2Fsim	HTTP/1.0\n\n"
		echo -e $host_query | nc -v -w 6000 $host 8096
	done
	let clusterid+=1
done

#TAG3
for podid in `seq 213 400`
do
	for i in `seq 1 10`
	do
		host_query="GET	http://$host/client/?command=addHost&zoneId=$zone&podId=$podid&username=sim&password=sim&cluster=CLUSTER$clusterid&hosttags=TAG3&url=http%3A%2F%2Fsim	HTTP/1.0\n\n"
		echo -e $host_query | nc -v -w 6000 $host 8096
	done
	let clusterid+=1
done
