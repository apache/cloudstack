
  #
  # Copyright (C) 2011 Cloud.com, Inc.  All rights reserved.
  #
 

x=$1
y=$2
name=$3

vlan_query="GET http://10.91.30.212/client/?command=createVlanIpRange&vlan=untagged&zoneid=1&podId=$name&forVirtualNetwork=false&gateway=172.$x.$y.1&netmask=255.255.255.0&startip=172.$x.$y.2&endip=172.$x.$y.252        HTTP/1.0\n\n"

echo -e $vlan_query | nc -v -w 20 10.91.30.212 8096
