
  #
  # Copyright (C) 2011 Cloud.com, Inc.  All rights reserved.
  #
 

name=$1

host_query="GET	http://10.91.30.226:8096/client/?command=addHost&zoneId=1&podId=$((name+250))&username=sim&password=sim&clustername=simulator-$name&hosttags=RP$name&url=http%3A%2F%2Fsim	HTTP/1.0\n\n"
echo -e $host_query | nc -v -q 60 10.91.30.226 8096
