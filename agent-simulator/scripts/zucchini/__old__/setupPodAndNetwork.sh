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

 

workers=$1
x=$2 #CIDR - 16bytes
y=$3 #CIDR - 8 bytes

for name in `seq 1 $workers`
do
	pod_query="GET  http://10.91.30.226:8096/client/?command=createPod&zoneId=1&name=RP$name&cidr=182.$x.$y.0%2F24&startIp=182.$x.$y.2&endIp=182.$x.$y.252&gateway=182.$x.$y.1	HTTP/1.0\n\n"
	vlan_query="GET http://10.91.30.226:8096/client/?command=createVlanIpRange&vlan=untagged&zoneid=1&podId=$((name+250))&forVirtualNetwork=false&gateway=182.$y.$x.1&netmask=255.255.255.0&startip=182.$y.$x.2&endip=182.$y.$x.252        HTTP/1.0\n\n"
	so_query="GET	http://10.91.30.226:8096/client/?command=createServiceOffering&name=RP$name&displayText=RP$name&storageType=local&cpuNumber=1&cpuSpeed=1000&memory=512&offerha=false&usevirtualnetwork=false&hosttags=RP$name	HTTP/1.0\n\n"

	echo -e $pod_query | nc -v -q 20 10.91.30.226 8096
	echo -e $vlan_query | nc -v -q 20 10.91.30.226 8096
	echo -e $so_query | nc -v -q 20 10.91.30.226 8096

	let x+=1
	let y+=1
done



