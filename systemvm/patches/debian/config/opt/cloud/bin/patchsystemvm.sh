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
logfile="/var/log/cloud.log"

log_it() {
  echo "$(date) patchsystemvm.sh $@" >> $logfile
}

# To use existing console proxy .zip-based package file
patch_console_proxy() {
   local patchfile=$1
   log_it "Patching CPVM/SSVM with cloud agent jars from $patchfile"
   rm /usr/local/cloud/systemvm -rf
   mkdir -p /usr/local/cloud/systemvm
   log_it "Patching /usr/local/cloud/systemvm:"
   echo "All" | unzip $patchfile -d /usr/local/cloud/systemvm >>$logfile 2>&1
   find /usr/local/cloud/systemvm/ -name \*.sh | xargs chmod 555
   return 0
}

consoleproxy_svcs() {
   log_it "Configuring console proxy services"
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
   log_it "Configuring SSVM services"
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
   log_it "Configuring VR services"
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
       log_it "Configuring redundant VR services"
       chkconfig dnsmasq off
       chkconfig cloud-passwd-srvr off
       chkconfig keepalived on
       chkconfig conntrackd on
       chkconfig postinit on
       echo "keepalived conntrackd postinit" >> /var/cache/cloud/enabled_svcs
       echo "dnsmasq cloud-passwd-srvr" >> /var/cache/cloud/disabled_svcs
   else
       log_it "Configuring non-redundant VR services"
       chkconfig dnsmasq on
       chkconfig cloud-passwd-srvr on
       chkconfig keepalived off
       chkconfig conntrackd off
       echo "dnsmasq cloud-passwd-srvr " >> /var/cache/cloud/enabled_svcs
       echo "keepalived conntrackd " >> /var/cache/cloud/disabled_svcs
   fi
}

dhcpsrvr_svcs() {
   log_it "Configuring DHCP services"
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
   log_it "Configuring external load balancing VM services"
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
   log_it "Configuring internal load balancing VM services"
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
   log_it "Configuring PCI hot plug"
   sed -i -e "/acpiphp/d" /etc/modules
   sed -i -e "/pci_hotplug/d" /etc/modules
   echo acpiphp >> /etc/modules
   echo pci_hotplug >> /etc/modules
}

enable_serial_console() {
   log_it "Enabling serial console"
   sed -i -e "/^serial.*/d" /boot/grub/grub.conf
   sed -i -e "/^terminal.*/d" /boot/grub/grub.conf
   sed -i -e "/^default.*/a\serial --unit=0 --speed=115200 --parity=no --stop=1" /boot/grub/grub.conf
   sed -i -e "/^serial.*/a\terminal --timeout=0 serial console" /boot/grub/grub.conf
   sed -i -e "s/\(^kernel.* ro\) \(console.*\)/\1 console=tty0 console=ttyS0,115200n8/" /boot/grub/grub.conf
   sed -i -e "/^s0:2345:respawn.*/d" /etc/inittab
   sed -i -e "/6:23:respawn/a\s0:2345:respawn:/sbin/getty -L 115200 ttyS0 vt102" /etc/inittab
}

log_it "Starting $0 $*"

CMDLINE=$(cat /var/cache/cloud/cmdline)
log_it "CMDLINE passed to system VM patch process: $CMDLINE"

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
  log_it "Patching ${TYPE}."
  patch_console_proxy ${PATCH_MOUNT}/systemvm.zip
  if [ $? -gt 0 ]
  then
    log_it "Failed to apply patch systemvm"
    exit 5
  fi
fi


#empty known hosts
echo "" > /root/.ssh/known_hosts

if [ "$Hypervisor" == "kvm" ]
then
   log_it "Enabling PCI hotplug and serial console for KVM"
   enable_pcihotplug
   enable_serial_console
fi

if [ "$TYPE" == "router" ] || [ "$TYPE" == "vpcrouter" ]
then
  log_it "Updating ${TYPE} services"
  routing_svcs
  if [ $? -gt 0 ]
  then
    log_it "Failed to execute routing_svcs"
    exit 6
  fi
fi

if [ "$TYPE" == "dhcpsrvr" ]
then
  log_it "Updating ${TYPE} services"
  dhcpsrvr_svcs
  if [ $? -gt 0 ]
  then
    log_it "Failed to execute dhcpsrvr_svcs"
    exit 6
  fi
fi


if [ "$TYPE" == "consoleproxy" ]
then
  log_it "Updating ${TYPE} services"
  consoleproxy_svcs
  if [ $? -gt 0 ]
  then
    log_it "Failed to execute consoleproxy_svcs"
    exit 7
  fi
fi

if [ "$TYPE" == "secstorage" ]
then
  log_it "Updating ${TYPE} services"
  secstorage_svcs
  if [ $? -gt 0 ]
  then
    log_it "Failed to execute secstorage_svcs"
    exit 8
  fi
fi

if [ "$TYPE" == "elbvm" ]
then
  log_it "Updating ${TYPE} services"
  elbvm_svcs
  if [ $? -gt 0 ]
  then
    log_it "Failed to execute elbvm svcs"
    exit 9
  fi
fi

if [ "$TYPE" == "ilbvm" ]
then
  log_it "Updating ${TYPE} services"
  ilbvm_svcs
  if [ $? -gt 0 ]
  then
    log_it "Failed to execute ilbvm svcs"
    exit 9
  fi
fi

exit $?
