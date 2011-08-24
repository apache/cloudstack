#!/bin/bash

a=$1 #CIDR - 16bytes
b=$2 #CIDR - 8 bytes
host=$3 #MSHOST
zoneid=$4

zone_query="GET  http://$host/client/?command=createZone&name=SimulatorBasic&dns1=4.2.2.2&internaldns1=4.2.2.2&vlan=10-4000&guestcidraddress=10.1.1.0%2F24  HTTP/1.0\n\n"
echo -e $zone_query | nc -v -w 120 $host 8096

let x=a
let y=b
for name in `seq $zone 100`
do
	pod_query="GET  http://$host/client/?command=createPod&zoneId=$zoneid&name=POD$name&cidr=172.$x.$y.0%2F24&startIp=172.$x.$y.2&endIp=172.$x.$y.252&gateway=172.$x.$y.1	HTTP/1.0\n\n"
	vlan_query="GET http://$host/client/?command=createVlanIpRange&vlan=untagged&zoneid=$zoneid&podId=$name&forVirtualNetwork=false&gateway=172.$y.$x.1&netmask=255.255.255.0&startip=172.$y.$x.2&endip=172.$y.$x.252        HTTP/1.0\n\n"
	echo -e $pod_query | nc -v -w 20 $host 8096
	echo -e $vlan_query | nc -v -w 20 $host 8096

	let x+=1
	let y+=1
done

clusterid=1
for podid in `seq $zone 100`
do
	for i in `seq 1 3`
	do
		host_query="GET	http://$host/client/?command=addHost&zoneId=$zoneid&podId=$podid&username=sim&password=sim&clustername=CLUSTER$clusterid&url=http%3A%2F%2Fsim	HTTP/1.0\n\n"
		echo -e $host_query | nc -v -w 6000 $host 8096
	done
	let clusterid+=1
done

sstorquery="GET	http://$host/client/?command=addSecondaryStorage&zoneid=$zoneid&url=nfs://172.1.25.32/export/share/   HTTP/1.0\n\n"
echo -e $sstorquery | nc -v -w 6000 $host 8096
