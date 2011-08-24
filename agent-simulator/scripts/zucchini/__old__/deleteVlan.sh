
  #
  # Copyright (C) 2011 Cloud.com, Inc.  All rights reserved.
  #
 

x=$1

vlan_query="GET  http://10.91.30.226:8096/client/?command=deleteVlanIpRange&id=$x	HTTP/1.0\n\n"


echo -e $vlan_query | nc -v -q 20 10.91.30.226 8096

