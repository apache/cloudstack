
  #
  # Copyright (C) 2011 Cloud.com, Inc.  All rights reserved.
  #
 

x=$1

delete="GET  http://127.0.0.1/client/?command=deleteAccount&id=$x	HTTP/1.0\n\n"

echo -e $delete | nc -v -q 60 127.0.0.1 8096

