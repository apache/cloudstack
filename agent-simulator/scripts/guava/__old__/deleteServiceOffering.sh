
  #
  # Copyright (C) 2011 Cloud.com, Inc.  All rights reserved.
  #
 

x=$1

delete_so="GET  http://10.91.30.219:8096/client/?command=deleteServiceOffering&id=$x	HTTP/1.0\n\n"

echo -e $delete_so | nc -v -q 20 10.91.30.219 8096

