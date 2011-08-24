
  #
  # Copyright (C) 2011 Cloud.com, Inc.  All rights reserved.
  #
 

zoneid=$1
templateId=$2
serviceOfferingId=$3

query="GET	http://10.91.30.226:8096/client/?command=deployVirtualMachine&zoneId=$1&templateId=$2&serviceOfferingId=$3&account=admin&domainid=1	HTTP/1.0\n\n"
 
echo -e $query | nc -v -q 20 10.91.30.226 8096
