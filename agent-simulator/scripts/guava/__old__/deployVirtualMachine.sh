
  #
  # Copyright (C) 2011 Cloud.com, Inc.  All rights reserved.
  #
 

zoneid=$1
templateId=$2
serviceOfferingId=$3
networkIds=$4

query="GET	http://10.91.30.219/client/?command=deployVirtualMachine&zoneId=$1&hypervisor=Simulator&templateId=$2&serviceOfferingId=$3&networkIds=$4&account=admin&domainid=1	HTTP/1.0\n\n"
 
echo -e $query | nc -v -q 20 10.91.30.219 8096
