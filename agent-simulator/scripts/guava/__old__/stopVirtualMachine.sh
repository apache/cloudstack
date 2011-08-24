
  #
  # Copyright (C) 2011 Cloud.com, Inc.  All rights reserved.
  #
 

x=$1

stop_vm="GET  http://127.0.0.1/client/?command=stopVirtualMachine&id=$x	HTTP/1.0\n\n"


echo -e $stop_vm | nc -v -q 60 127.0.0.1 8096

