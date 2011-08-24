
  #
  # Copyright (C) 2011 Cloud.com, Inc.  All rights reserved.
  #
 

test="GET	http://10.91.30.226:8096/?command=listEvents&page=1     HTTP/1.0\n\n"
echo -e $test | nc -v -q 120 10.91.30.226 8096

