
  #
  # Copyright (C) 2011 Cloud.com, Inc.  All rights reserved.
  #
 

query="GET	http://10.91.30.219:8096/client/?command=listCapacity&zoneId=$1&podid=$2&response=json	HTTP/1.0\n\n"
 
echo -e $query | nc -v -q 120 10.91.30.219 8096
