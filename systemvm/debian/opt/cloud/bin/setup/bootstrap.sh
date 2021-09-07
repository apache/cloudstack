#!/bin/bash
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

PATH="/usr/local/sbin:/usr/local/bin:/sbin:/bin:/usr/sbin:/usr/bin"
CMDLINE=/var/cache/cloud/cmdline

rm -f /var/cache/cloud/enabled_svcs
rm -f /var/cache/cloud/disabled_svcs

. /lib/lsb/init-functions

log_it() {
  echo "$(date) $@" >> /var/log/cloud.log
  log_action_msg "$@"
}

hypervisor() {
  if [ -d /proc/xen ]; then
    mount -t xenfs none /proc/xen
    $(dmesg | grep -q "Xen HVM")
    if [ $? -eq 0 ]; then  # 1=PV,0=HVM
      echo "xen-hvm" && return 0
    else
      echo "xen-pv" && return 0
    fi
  fi

  [ -x /usr/sbin/virt-what ] && local facts=( $(virt-what) )
  if [ "$facts" != "" ]; then
    # Xen HVM is recognized as Hyperv when Viridian extensions are enabled
    if [ "${facts[-1]}" == "xen-domU" ] && [ "${facts[0]}" == "hyperv" ]; then
      echo "xen-hvm" && return 0
    else
      echo ${facts[-1]} && return 0
    fi
  fi

  grep -q QEMU /proc/cpuinfo  && echo "kvm" && return 0
  grep -q QEMU /var/log/messages && echo "kvm" && return 0

  vmware-checkvm &> /dev/null && echo "vmware" && return 0

  echo "unknown" && return 1
}

config_guest() {
  [ ! -d /proc/xen ] && sed -i 's/^vc/#vc/' /etc/inittab && telinit q
  [ -d /proc/xen ] && sed -i 's/^#vc/vc/' /etc/inittab && telinit q

  systemctl daemon-reload

  case $HYPERVISOR in
     xen-pv|xen-domU)
          systemctl stop ntpd
          systemctl disable ntpd
          systemctl enable xe-daemon
          systemctl start xe-daemon

          cat /proc/cmdline > $CMDLINE
          sed -i "s/%/ /g" $CMDLINE
          ;;
     xen-hvm)
          systemctl stop ntpd
          systemctl disable ntpd
          systemctl enable xe-daemon
          systemctl start xe-daemon

          if [ ! -f /usr/bin/xenstore-read ]; then
            log_it "ERROR: xentools not installed, cannot found xenstore-read" && exit 5
          fi
          /usr/bin/xenstore-read vm-data/cloudstack/init > $CMDLINE
          sed -i "s/%/ /g" $CMDLINE
          ;;
     kvm)
          # Configure kvm hotplug support
          if grep -E 'CONFIG_HOTPLUG_PCI=y|CONFIG_HOTPLUG_PCI_ACPI=y' /boot/config-`uname -r`; then
            log_it "acpiphp and pci_hotplug module already compiled in"
          else
            modprobe acpiphp 2> /dev/null && log_it "acpiphp module loaded" || true
            modprobe pci_hotplug 2> /dev/null && log_it "pci_hotplug module loaded" || true
          fi

          sed -i -e "/^s0:2345:respawn.*/d" /etc/inittab
          sed -i -e "/6:23:respawn/a\s0:2345:respawn:/sbin/getty -L 115200 ttyS0 vt102" /etc/inittab
          systemctl enable qemu-guest-agent
          systemctl start qemu-guest-agent

          # Wait for $CMDLINE file to be written by the qemu-guest-agent
          for i in {1..60}; do
            if [ -s $CMDLINE ]; then
              log_it "Received a new non-empty cmdline file from qemu-guest-agent"
              # Remove old configuration files in /etc/cloudstack if VR is booted from cloudstack
              rm -rf /etc/cloudstack/*.json
              log_it "Booting from cloudstack, remove old configuration files in /etc/cloudstack/"
              break
            fi
            sleep 1
          done
          if [ ! -s $CMDLINE  ]; then
            log_it "Failed to receive the cmdline file via the qemu-guest-agent"
          fi
          ;;
     vmware)
          # system time sync'd with host via vmware tools
          systemctl stop ntpd
          systemctl disable ntpd
          systemctl enable open-vm-tools
          systemctl start open-vm-tools

          vmtoolsd --cmd 'machine.id.get' > $CMDLINE
          ;;
     virtualpc|hyperv)
          # Hyper-V is recognized as virtualpc hypervisor type. Boot args are passed using KVP Daemon
          systemctl enable hyperv-daemons.hv-fcopy-daemon.service hyperv-daemons.hv-kvp-daemon.service hyperv-daemons.hv-vss-daemon.service
          systemctl start hyperv-daemons.hv-fcopy-daemon.service hyperv-daemons.hv-kvp-daemon.service hyperv-daemons.hv-vss-daemon.service
          sleep 5
          cp -f /var/opt/hyperv/.kvp_pool_0 $CMDLINE
          cat /dev/null > /var/opt/hyperv/.kvp_pool_0
          ;;
     virtualbox)
          # Virtualbox is used to test the virtual router
          # get the commandline from a dmistring  (yes, hacky!)
          dmidecode | grep cmdline | sed 's/^.*cmdline://' > $CMDLINE
          RV=$?
          if [ $RV -ne 0 ] ; then
            log_it "Failed to get cmdline from a virtualbox dmi property"
          fi
          ;;
  esac

  # Find and export guest type
  export TYPE=$(grep -Po 'type=\K[a-zA-Z]*' $CMDLINE)
}

patch_systemvm() {
  local patchfile=$1
  local backupfolder="/tmp/.conf.backup"
  local logfile="/var/log/patchsystemvm.log"
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
  # Import global cacerts into 'cloud' service's keystore
  keytool -importkeystore -srckeystore /etc/ssl/certs/java/cacerts -destkeystore /usr/local/cloud/systemvm/certs/realhostip.keystore -srcstorepass changeit -deststorepass vmops.com -noprompt || true
  return 0
}

patch() {
  local PATCH_MOUNT=/media/cdrom
  local logfile="/var/log/patchsystemvm.log"
  if [ "$TYPE" == "consoleproxy" ] || [ "$TYPE" == "secstorage" ]  && [ -f ${PATCH_MOUNT}/agent.zip ] && [ -f /var/cache/cloud/patch.required ]
  then
    echo "Patching systemvm for cloud service with mount=$PATCH_MOUNT for type=$TYPE" >> $logfile
    patch_systemvm ${PATCH_MOUNT}/agent.zip
    if [ $? -gt 0 ]
    then
      echo "Failed to apply patch systemvm\n" >> $logfile
      exit 1
    fi
  fi

  rm -f /var/cache/cloud/patch.required
  chmod -x /etc/systemd/system/cloud*.service
  systemctl daemon-reload
  umount $PATCH_MOUNT || true

  if [ -f /mnt/cmdline ]; then
    cat /mnt/cmdline > $CMDLINE
  fi
  return 0
}

config_sysctl() {
  # When there is more memory reset the cache back pressure to default 100
  physmem=$(free|awk '/^Mem:/{print $2}')
  if [ $((physmem)) -lt 409600 ]; then
      sed  -i "/^vm.vfs_cache_pressure/ c\vm.vfs_cache_pressure = 200" /etc/sysctl.conf
  else
      sed  -i "/^vm.vfs_cache_pressure/ c\vm.vfs_cache_pressure = 100" /etc/sysctl.conf
  fi

  sync
  sysctl -p
}

bootstrap() {
  log_it "Bootstrapping systemvm appliance"

  export HYPERVISOR=$(hypervisor)
  [ $? -ne 0 ] && log_it "Failed to detect hypervisor type, bailing out" && exit 10
  log_it "Starting guest services for $HYPERVISOR"

  config_guest
  patch
  config_sysctl

  log_it "Configuring systemvm type=$TYPE"
  if [ -f "/opt/cloud/bin/setup/$TYPE.sh" ]; then
      /opt/cloud/bin/setup/$TYPE.sh
  else
      /opt/cloud/bin/setup/default.sh
  fi

  log_it "Finished setting up systemvm"
  exit 0
}

bootstrap
