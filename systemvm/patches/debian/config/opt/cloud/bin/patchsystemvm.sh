#/bin/bash
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

#set -x
logfile="/var/log/patchsystemvm.log"
# To use existing console proxy .zip-based package file
patch_console_proxy() {
   local patchfile=$1
   local backupfolder="/tmp/.conf.backup"
   if [ -f /usr/local/cloud/systemvm/conf/cloud.jks ]; then
      rm -fr $backupfolder
      mkdir -p $backupfolder
      cp -r /usr/local/cloud/systemvm/conf/* $backupfolder/
   fi
   rm /usr/local/cloud/systemvm -rf
   mkdir -p /usr/local/cloud/systemvm
   echo "All" | unzip $patchfile -d /usr/local/cloud/systemvm >$logfile 2>&1
   find /usr/local/cloud/systemvm/ -name \*.sh | xargs chmod 555
   if [ -f $backupfolder/cloud.jks ]; then
      cp -r $backupfolder/* /usr/local/cloud/systemvm/conf/
      echo "Restored keystore file and certs using backup" >> $logfile
   fi
   rm -fr $backupfolder
   return 0
}

consoleproxy_svcs() {
   chkconfig cloud on
   chkconfig postinit on
   chkconfig cloud-passwd-srvr off
   chkconfig haproxy off ;
   chkconfig dnsmasq off
   chkconfig ssh on
   chkconfig apache2 off
   chkconfig nfs-common off
   chkconfig portmap off
   chkconfig keepalived off
   chkconfig conntrackd off
   echo "cloud postinit ssh" > /var/cache/cloud/enabled_svcs
   echo "cloud-passwd-srvr haproxy dnsmasq apache2 nfs-common portmap" > /var/cache/cloud/disabled_svcs
   mkdir -p /var/log/cloud
}

secstorage_svcs() {
   chkconfig cloud on
   chkconfig postinit on
   chkconfig cloud-passwd-srvr off
   chkconfig haproxy off ;
   chkconfig dnsmasq off
   chkconfig portmap on
   chkconfig nfs-common on
   chkconfig ssh on
   chkconfig apache2 off
   chkconfig keepalived off
   chkconfig conntrackd off
   echo "cloud postinit ssh nfs-common portmap" > /var/cache/cloud/enabled_svcs
   echo "cloud-passwd-srvr haproxy dnsmasq" > /var/cache/cloud/disabled_svcs
   mkdir -p /var/log/cloud
}

routing_svcs() {
   grep "redundant_router=1" /var/cache/cloud/cmdline > /dev/null
   RROUTER=$?
   chkconfig cloud off
   chkconfig haproxy on ; 
   chkconfig ssh on
   chkconfig nfs-common off
   chkconfig portmap off
   echo "ssh haproxy apache2" > /var/cache/cloud/enabled_svcs
   echo "cloud nfs-common portmap" > /var/cache/cloud/disabled_svcs
   if [ $RROUTER -eq 0 ]
   then
       chkconfig dnsmasq off
       chkconfig cloud-passwd-srvr off
       chkconfig keepalived on
       chkconfig conntrackd on
       chkconfig postinit on
       echo "keepalived conntrackd postinit" >> /var/cache/cloud/enabled_svcs
       echo "dnsmasq cloud-passwd-srvr" >> /var/cache/cloud/disabled_svcs
   else
       chkconfig dnsmasq on
       chkconfig cloud-passwd-srvr on
       chkconfig keepalived off
       chkconfig conntrackd off
       echo "dnsmasq cloud-passwd-srvr " >> /var/cache/cloud/enabled_svcs
       echo "keepalived conntrackd " >> /var/cache/cloud/disabled_svcs
   fi
}

dhcpsrvr_svcs() {
   chkconfig cloud off
   chkconfig cloud-passwd-srvr on ; 
   chkconfig haproxy off ; 
   chkconfig dnsmasq on
   chkconfig ssh on
   chkconfig nfs-common off
   chkconfig portmap off
   chkconfig keepalived off
   chkconfig conntrackd off
   echo "ssh dnsmasq cloud-passwd-srvr apache2" > /var/cache/cloud/enabled_svcs
   echo "cloud nfs-common haproxy portmap" > /var/cache/cloud/disabled_svcs
}

elbvm_svcs() {
   chkconfig cloud off
   chkconfig haproxy on ; 
   chkconfig ssh on
   chkconfig nfs-common off
   chkconfig portmap off
   chkconfig keepalived off
   chkconfig conntrackd off
   echo "ssh haproxy" > /var/cache/cloud/enabled_svcs
   echo "cloud dnsmasq cloud-passwd-srvr apache2 nfs-common portmap" > /var/cache/cloud/disabled_svcs
}


ilbvm_svcs() {
   chkconfig cloud off
   chkconfig haproxy on ; 
   chkconfig ssh on
   chkconfig nfs-common off
   chkconfig portmap off
   chkconfig keepalived off
   chkconfig conntrackd off
   echo "ssh haproxy" > /var/cache/cloud/enabled_svcs
   echo "cloud dnsmasq cloud-passwd-srvr apache2 nfs-common portmap" > /var/cache/cloud/disabled_svcs
}

enable_pcihotplug() {
   sed -i -e "/acpiphp/d" /etc/modules
   sed -i -e "/pci_hotplug/d" /etc/modules
   echo acpiphp >> /etc/modules
   echo pci_hotplug >> /etc/modules
}

enable_serial_console() {
   sed -i -e "/^serial.*/d" /boot/grub/grub.conf
   sed -i -e "/^terminal.*/d" /boot/grub/grub.conf
   sed -i -e "/^default.*/a\serial --unit=0 --speed=115200 --parity=no --stop=1" /boot/grub/grub.conf
   sed -i -e "/^serial.*/a\terminal --timeout=0 serial console" /boot/grub/grub.conf
   sed -i -e "s/\(^kernel.* ro\) \(console.*\)/\1 console=tty0 console=ttyS0,115200n8/" /boot/grub/grub.conf
   sed -i -e "/^s0:2345:respawn.*/d" /etc/inittab
   sed -i -e "/6:23:respawn/a\s0:2345:respawn:/sbin/getty -L 115200 ttyS0 vt102" /etc/inittab
}


CMDLINE=$(cat /var/cache/cloud/cmdline)
TYPE="router"
PATCH_MOUNT=$1
Hypervisor=$2

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

if [ "$TYPE" == "consoleproxy" ] || [ "$TYPE" == "secstorage" ]  && [ -f ${PATCH_MOUNT}/systemvm.zip ]
then
  patch_console_proxy ${PATCH_MOUNT}/systemvm.zip
  if [ $? -gt 0 ]
  then
    printf "Failed to apply patch systemvm\n" >$logfile
    exit 5
  fi
fi


#empty known hosts
echo "" > /root/.ssh/known_hosts

if [ "$Hypervisor" == "kvm" ]
then
   enable_pcihotplug
   enable_serial_console
fi

if [ "$TYPE" == "router" ] || [ "$TYPE" == "vpcrouter" ]
then
  routing_svcs
  if [ $? -gt 0 ]
  then
    printf "Failed to execute routing_svcs\n" >$logfile
    exit 6
  fi
fi

if [ "$TYPE" == "dhcpsrvr" ]
then
  dhcpsrvr_svcs
  if [ $? -gt 0 ]
  then
    printf "Failed to execute dhcpsrvr_svcs\n" >$logfile
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

if [ "$TYPE" == "elbvm" ]
then
  elbvm_svcs
  if [ $? -gt 0 ]
  then
    printf "Failed to execute elbvm svcs\n" >$logfile
    exit 9
  fi
fi

if [ "$TYPE" == "ilbvm" ]
then
  ilbvm_svcs
  if [ $? -gt 0 ]
  then
    printf "Failed to execute ilbvm svcs\n" >$logfile
    exit 9
  fi
fi

exit $?
