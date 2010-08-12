#!/bin/bash
# $Id: cleanupmyvms.sh 9804 2010-06-22 18:36:49Z alex $ $HeadURL: svn://svn.lab.vmops.com/repos/branches/2.1.x.beta/java/scripts/storage/qcow2/cleanupmyvms.sh $
# @VERSION@

if [ "$1" == "" ]
then
  echo "Usage: $(basename $0) <instance>"
  exit 3
fi

instance=$1

for i in $(virsh list | grep $instance | awk '{print $2}' );  
do   
  files=$(virsh dumpxml $i | grep file | grep source | awk '{print $2}' | awk -F"=" '{print $2}'  );   
  echo "Destroying VM: $i"
  virsh destroy $i
  virsh undefine $i
  sleep 2
  for f in $files; 
  do  
     f=${f%%/>}
     echo "Destroying disk: $f"
     rm -f $f
  done; 
done

#double check, if there are vms in shut-off state
for i in $(virsh list --all |grep $instance| awk '{print $2}')
do
  files=$(virsh dumpxml $i | grep file | grep source | awk '{print $2}' | awk -F"=" '{print $2}'  );   
  echo "Undefine VM: $i"
  virsh undefine $i
  for f in $files;
  do
     f=${f%%/>}
     if [ -f $f ]
     then
     	f=${f%%/>}
     	echo "Destroying disk: $f"
     	rm -f $f
     fi
  done
done

#cleanup the bridge
br=`virsh net-list|grep $instance |awk '{print $1}'`
if [ -n "$br" ]
then
virsh net-destroy $br
fi
#double check
br=`brctl show |grep $instance|awk '{print $1}'`
if [ -n "$br" ]
then
   ifconfig $br down
   sleep 1
   brctl delbr $br
   sleep 1
fi

dnsmasq=`cat /var/run/libvirt/network/vmops-$instance-private.pid`
kill -9 $dnsmasq 2>/dev/null

