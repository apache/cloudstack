
  #
  # Copyright (C) 2011 Cloud.com, Inc.  All rights reserved.
  #
 

x=$1

destroy="GET  http://10.91.30.226:8096/client/?command=destroyVirtualMachine&id=$x	HTTP/1.0\n\n"

echo -e $destroy | nc -v -q 60 10.91.30.226 8096

