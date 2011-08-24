#
# Copyright (C) 2011 Cloud.com, Inc.  All rights reserved.
#



name=$1

account_query="GET	http://127.0.0.1/client/?command=createAccount&accounttype=1&email=simulator%40simulator.com&username=$name&firstname=first$name&lastname=last$name&password=5f4dcc3b5aa765d61d8327deb882cf99&account=$name&domainid=1	HTTP/1.1\n\n"

echo -e $account_query | nc -v -q 120 127.0.0.1 8096
