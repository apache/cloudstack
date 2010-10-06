#!/bin/bash
MGMT_SERVER=localhost:8080

set -x
sessionkey=$1
#create zone
# curl -b admincookie.txt -sS "http://$MGMT_SERVER/client/api?command=createZone&name=$ZONENAME&dns1=$EXTDNS1&internaldns1=8.8.8.8&guestcidraddress=10.1.1.1/24&vlan=1000-1100&response=json"
echo
#update zone
#curl -b admincookie.txt -sS "http://$MGMT_SERVER/client/api?command=updateZone&id=1&name=$ZONENAME&DNS1=$EXTDNS1&internaldns1=$INTDNS1&guestcidraddress=10.1.1.1/24&vlan=1000-1100&response=json"
echo

#add pods
curl -b admincookie.txt -sS "http://$MGMT_SERVER/client/api?command=updatePod&id=5&name=CV&gateway=192.168.140.1&cidr=192.168.140.0%2F24&startip=192.168.140.150&endip=192.168.140.155&sessionkey=$sessionkey&response=json"
echo
#curl -b admincookie.txt -sS "http://localhost:8080/client/api?command=createPod&name=POD2&zoneid=1&gateway=192.168.203.1&cidr=192.168.203.0%2F24&startip=192.168.203.65&endip=192.168.203.72&response=json"
echo

#add secondary storage
curl -b admincookie.txt -sS "http://$MGMT_SERVER/client/api?command=addSecondaryStorage&zoneId=5&url=nfs%3A//nfs1.lab.vmops.com/export/home/chiradeep/secondary/&sessionkey=$sessionkey&response=json"
echo

#register template

#curl -b admincookie.txt -sS "http://localhost:8080/client/api?command=registerTemplate&displayText=Rightscale%20%ami-ccb35ea5&format=VHD&isPublic=true&name=Rightscale%20ami-ccb35ea5&osTypeId=11&passwordEnabled=false&url=http%3A//192.168.90.243/templates/zynga/ami-ccb35ea5.vhd.bz2&displayText=ZyngaRSTemplate&zoneId=1&ostypeid=12&sessionkey=$sessionkey&response=json"
