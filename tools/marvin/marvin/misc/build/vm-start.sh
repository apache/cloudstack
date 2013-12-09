#!/bin/bash
# Starts a vm on the xenserver with a predefined MAC and name-label

usage() {
  printf "Usage: %s: -m <mac> -n <vm name>\n" $(basename $0) >&2
  exit 2
}

mac=
vmname=
while getopts 'm:n:' OPTION
do
    case $OPTION in
        m)    mac="$OPTARG"
            ;;
        n)    vmname="$OPTARG"
            ;;
        ?)    usage
            exit 1
            ;;
    esac
done

vmuuid=$(xe vm-install template=Other\ install\ media new-name-label=$vmname)

sruuid=$(xe sr-list type=lvm | grep uuid | awk '{print $5}')
vdiuuid=$(xe vdi-create name-label=$vmname sharable=0 sr-uuid=$sruuid type=user virtual-size=21474836480)
vbduuid=$(xe vbd-create bootable=true mode=RW type=DISK device=0 unpluggable=true vdi-uuid=$vdiuuid vm-uuid=$vmuuid)

nwuuid=$(xe network-list bridge=xenbr0  | grep uuid | awk '{print $5}')
xe vif-create mac=$mac network-uuid=$nwuuid device=0 vm-uuid=$vmuuid

#Boot network followed by root disk
$(xe vm-param-set HVM-boot-params:order=nc uuid=$vmuuid)
#Minimum mem requirements for RHEL/Ubuntu
$(xe vm-memory-limits-set  static-min=1GiB static-max=1GiB dynamic-min=1GiB dynamic-max=1GiB uuid=$vmuuid)
$(xe vm-start uuid=$vmuuid)

