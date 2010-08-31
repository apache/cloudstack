#/bin/bash
# $Id: patchsystemvm.sh 10800 2010-07-16 13:48:39Z edison $ $HeadURL: svn://svn.lab.vmops.com/repos/branches/2.1.x/java/scripts/vm/hypervisor/xenserver/prepsystemvm.sh $

#set -x
logfile="/var/log/patchsystemvm.log"
#
# To use existing console proxy .zip-based package file
#
patch_console_proxy() {
   local patchfile=$1
   rm /usr/local/cloud/systemvm -rf
   mkdir -p /usr/local/cloud/systemvm
   echo "All" | unzip $patchfile -d /usr/local/cloud/systemvm >$logfile 2>&1
   find /usr/local/cloud/systemvm/ -name \*.sh | xargs chmod 555
   return 0
}

consoleproxy_svcs() {
   chkconfig cloud on
   chkconfig postinit on
   chkconfig domr_webserver off
   chkconfig haproxy off ;
   chkconfig dnsmasq off
   chkconfig sshd on
   chkconfig httpd off
   chkconfig nfs off
   chkconfig nfslock off
   chkconfig rpcbind off
   chkconfig rpcidmap off
   mkdir -p /var/log/cloud
}

secstorage_svcs() {
   chkconfig cloud on
   chkconfig postinit on
   chkconfig domr_webserver off
   chkconfig haproxy off ;
   chkconfig dnsmasq off
   chkconfig sshd on
   chkconfig httpd off
   mkdir -p /var/log/cloud
}

routing_svcs() {
   chkconfig cloud off
   chkconfig domr_webserver on ; 
   chkconfig haproxy on ; 
   chkconfig dnsmasq on
   chkconfig sshd on
   chkconfig nfs off
   chkconfig nfslock off
   chkconfig rpcbind off
   chkconfig rpcidmap off
}

CMDLINE=$(cat /var/cache/cloud/cmdline)
TYPE="router"

for i in $CMDLINE
  do
    # search for foo=bar pattern and cut out foo
    KEY=$(echo $i | cut -d= -f1)
    VALUE=$(echo $i | cut -d= -f2)
    case $KEY in
      type)
        TYPE=$VALUE
        ;;
      *)
        ;;
    esac
done

if [ "$TYPE" == "consoleproxy" ] || [ "$TYPE" == "secstorage" ]  && [ -f /media/cdrom/systemvm.zip ]
then
  patch_console_proxy /media/cdrom/systemvm.zip
  if [ $? -gt 0 ]
  then
    printf "Failed to apply patch systemvm\n" >$logfile
    exit 5
  fi
fi


#empty known hosts
echo "" > /root/.ssh/known_hosts

if [ "$TYPE" == "router" ]
then
  routing_svcs
  if [ $? -gt 0 ]
  then
    printf "Failed to execute routing_svcs\n" >$logfile
    exit 6
  fi
fi


if [ "$TYPE" == "consoleproxy" ]
then
  consoleproxy_svcs
  if [ $? -gt 0 ]
  then
    printf "Failed to execute consoleproxy_svcs\n" >$logfile
    exit 7
  fi
fi

if [ "$TYPE" == "secstorage" ]
then
  secstorage_svcs
  if [ $? -gt 0 ]
  then
    printf "Failed to execute secstorage_svcs\n" >$logfile
    exit 8
  fi
fi

exit $?
