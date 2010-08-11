#!/bin/sh
# $Id: make_migratable.sh 9879 2010-06-24 02:41:46Z anthony $ $HeadURL: svn://svn.lab.vmops.com/repos/vmdev/java/scripts/vm/hypervisor/xenserver/make_migratable.sh $

set -e

usage() {
  echo "$0 <VM uuid>"
  echo " -- fakes the presence of PV tools in <VM uuid>"
  echo " -- NB the VM must be either paused or running on localhost"
}
 
fake(){
  local uuid=$1
  domid=$(xe vm-list uuid=${uuid} params=dom-id --minimal)
  xenstore-write /local/domain/${domid}/attr/PVAddons/MajorVersion ${major} \
                 /local/domain/${domid}/attr/PVAddons/MinorVersion ${minor} \
                 /local/domain/${domid}/attr/PVAddons/MicroVersion ${micro} \
                 /local/domain/${domid}/data/updated 1
}

uuid=$(xe vm-list uuid=$1 params=uuid --minimal)
if [ $? -ne 0 ]; then
  echo "Argument should be a VM uuid"
  usage
  exit 1
fi

# use the PRODUCT_VERSION from here:
. /etc/xensource-inventory

major=$(echo ${PRODUCT_VERSION} | cut -f 1 -d .)
minor=$(echo ${PRODUCT_VERSION} | cut -f 2 -d .)
micro=$(echo ${PRODUCT_VERSION} | cut -f 3 -d .)

# Check the VM is running on this host
resident_on=$(xe vm-list uuid=${uuid} params=resident-on --minimal)
if [ "${resident_on}" != "${INSTALLATION_UUID}" ]; then
  echo "VM must be resident on this host"
  exit 2
fi

power_state=$(xe vm-list uuid=${uuid} params=power-state --minimal)
case "${power_state}" in
  running)
    fake $uuid
    ;;
  paused)
    fake $uuid
    ;;
  *)
    echo "VM must be either running or paused"
    exit 3
    ;;
esac


