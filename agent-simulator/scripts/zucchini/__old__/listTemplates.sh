
  #
  # Copyright (C) 2011 Cloud.com, Inc.  All rights reserved.
  #
 

query="GET	http://10.91.30.226:8096/client/?command=listTemplates&templatefilter=featured&zoneid=$1	HTTP/1.0\n\n"

echo -e $query | nc -v -q 120 10.91.30.226 8096
