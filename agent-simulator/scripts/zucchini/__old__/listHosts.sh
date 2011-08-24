
  #
  # Copyright (C) 2011 Cloud.com, Inc.  All rights reserved.
  #
 

query="GET	http://10.91.30.226:8096/client/?command=listHosts&zoneId=$1&account=admin&domainid=1&page=$2	HTTP/1.0\n\n"
 
echo -e $query | nc -v -q 120 10.91.30.226 8096
