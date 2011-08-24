
  #
  # Copyright (C) 2011 Cloud.com, Inc.  All rights reserved.
  #
 

x=$1

so_query="GET	http://10.91.30.226:8096/client/?command=createServiceOffering&name=SO$x&displayText=SO$x&storageType=local&cpuNumber=1&cpuSpeed=1000&memory=512&offerha=false&usevirtualnetwork=false&hosttags=SP$x	HTTP/1.0\n\n"

echo -e $so_query | nc -v -q 20 10.91.30.226 8096
