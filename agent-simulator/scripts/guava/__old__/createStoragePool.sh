#
# Copyright (C) 2011 Cloud.com, Inc.  All rights reserved.
#


spool_query="GET        http://127.0.0.1/client/?command=createStoragePool&zoneId=1&podId=1&clusterid=1&name=SPOOL&url=nfs://10.91.25.6/export/share/1   HTTP/1.0\n\n"
echo -e $spool_query | nc -v -q 60 127.0.0.1 8096
