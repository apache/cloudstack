#!/usr/bin/env bash
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
# 
#   http://www.apache.org/licenses/LICENSE-2.0
# 
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
 

# Health check script for the Secondary Storage VM

# DNS server is specified.


CMDLINE=/var/cache/cloud/cmdline
for i in `cat $CMDLINE`
do
   key=`echo $i | cut -d= -f1`
   value=`echo $i | cut -d= -f2`
   case $key in
      host)
         MGMTSERVER=$value       
         ;;
   esac
done

isCifs() {
 mount | grep "type cifs" > /dev/null
 echo $?
}

# ping dns server
echo ================================================
DNSSERVER=`egrep '^nameserver' /etc/resolv.conf  | awk '{print $2}'| head -1`
echo "First DNS server is " $DNSSERVER
ping -c 2  $DNSSERVER
if [ $? -eq 0 ]
then
    echo "Good: Can ping DNS server"
else
    echo "WARNING: cannot ping DNS server"
    echo "route follows"
    route -n
fi


# check dns resolve
echo ================================================
nslookup download.cloud.com 1> /tmp/dns 2>&1
grep 'no servers could' /tmp/dns 1> /dev/null 2>&1
if [ $? -eq 0 ]
then
    echo "ERROR: DNS not resolving download.cloud.com"
    echo resolv.conf follows
    cat /etc/resolv.conf
    exit 2
else
    echo "Good: DNS resolves download.cloud.com"
fi


# check to see if we have the NFS volume mounted
echo ================================================
storage="cifs"
if [ $(isCifs) -ne 0 ] ;
 then
   storage="nfs"
fi

mount|grep -v sunrpc|grep -v rpc_pipefs|grep $storage 1> /dev/null 2>&1
if [ $? -eq 0 ]
then
    echo "$storage is currently mounted"
    # check for write access
    for MOUNTPT in `mount|grep -v sunrpc|grep -v rpc_pipefs|grep $storage| awk '{print $3}'`
    do
        if [ $MOUNTPT != "/proc/xen" ] # mounted by xen
        then
            echo Mount point is $MOUNTPT
            touch $MOUNTPT/foo
            if [ $? -eq 0 ]
            then
                echo "Good: Can write to mount point"
                rm $MOUNTPT/foo
            else
                echo "ERROR: Cannot write to mount point"
                echo "You need to export with norootsquash"
            fi
        fi
     done
else
    echo "ERROR: NFS is not currently mounted"
    echo "Try manually mounting from inside the VM"
    NFSSERVER=`awk '{print $17}' $CMDLINE|awk -F= '{print $2}'|awk -F: '{print $1}'`
    echo "NFS server is " $NFSSERVER
    ping -c 2  $NFSSERVER
    if [ $? -eq 0 ]
    then
	echo "Good: Can ping $storage server"
    else
	echo "WARNING: cannot ping $storage server"
	echo routing table follows
	route -n
    fi
fi


# check for connectivity to the management server
echo ================================================
echo Management server is $MGMTSERVER.  Checking connectivity.
socatout=$(echo | socat - TCP:$MGMTSERVER:8250,connect-timeout=3 2>&1)
if [ $? -eq 0 ]
then
    echo "Good: Can connect to management server port 8250"
else
    echo "ERROR: Cannot connect to $MGMTSERVER port 8250"
    echo $socatout
    exit 4
fi


# check for the java process running
echo ================================================
ps -eaf|grep -v grep|grep java 1> /dev/null 2>&1
if [ $? -eq 0 ]
then
    echo "Good: Java process is running"
else
    echo "ERROR: Java process not running.  Try restarting the SSVM."
    exit 3
fi

echo ================================================
echo Tests Complete.  Look for ERROR or WARNING above.  

exit 0
