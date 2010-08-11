#!/bin/bash
#
# Health check script for the Secondary Storage VM
#

# /proc/cmdline can have a different number of arguments depending on whether or not a second
# DNS server is specified.

grep dns2 /proc/cmdline 1> /dev/null 2>&1
HAVEDNS2=$?

# ping dns server
echo ================================================
DNSSERVER=`egrep '^nameserver' /etc/resolv.conf  | awk '{print $2}'| head -1`
echo "First DNS server is " $DNSSERVER
ping -c 2 -w 2 $DNSSERVER
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
    MOUNTPT=`mount|grep -v sunrpc|grep nfs| awk '{print $3}'`
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
else
    echo "ERROR: NFS is not currently mounted"
    echo "Try manually mounting from inside the VM"
    if [ $HAVEDNS2 -eq 0 ]
    then
	NFSSERVER=`awk '{print $24}' /proc/cmdline|awk -F= '{print $2}'|awk -F: '{print $1}'`
    else
	NFSSERVER=`awk '{print $23}' /proc/cmdline|awk -F= '{print $2}'|awk -F: '{print $1}'`
    fi
    echo "NFS server is " $NFSSERVER
    ping -c 2 -w 2 $NFSSERVER
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
if [ $HAVEDNS2 -eq 0 ]
then
    MGMTSERVER=`awk '{print $21}' /proc/cmdline | awk -F= '{print $2}'`
else
    MGMTSERVER=`awk '{print $20}' /proc/cmdline | awk -F= '{print $2}'`
fi
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
