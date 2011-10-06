#!/bin/bash
  #
  # Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
  # 
  # This software is licensed under the GNU General Public License v3 or later.
  # 
  # It is free software: you can redistribute it and/or modify
  # it under the terms of the GNU General Public License as published by
  # the Free Software Foundation, either version 3 of the License, or any later version.
  # This program is distributed in the hope that it will be useful,
  # but WITHOUT ANY WARRANTY; without even the implied warranty of
  # MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  # GNU General Public License for more details.
  # 
  # You should have received a copy of the GNU General Public License
  # along with this program.  If not, see <http://www.gnu.org/licenses/>.
  #
 

#
# Health check script for the Secondary Storage VM
#

# /proc/cmdline can have a different number of arguments depending on whether or not a second
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
mount|grep -v sunrpc|grep nfs 1> /dev/null 2>&1
if [ $? -eq 0 ]
then
    echo "NFS is currently mounted"
    # check for write access
    for MOUNTPT in `mount|grep -v sunrpc|grep nfs| awk '{print $3}'`
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
	echo "Good: Can ping NFS server"
    else
	echo "WARNING: cannot ping NFS server"
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
