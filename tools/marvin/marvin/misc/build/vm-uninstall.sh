#!/bin/bash

# Uninstalls a given VM

usage() {
  printf "Usage: %s: -n <vm name>\n" $(basename $0) >&2
  exit 2
}

vmname=
while getopts 'n:' OPTION
do
    case $OPTION in
        n)    vmname="$OPTARG"
            ;;
        ?)    usage
            exit 1
            ;;
    esac
done

for vdi_uuid in $(xe vdi-list name-label=$vmname | grep ^uuid | awk '{print $5}')
do
    xe vdi-unlock --force uuid=$vdi_uuid
    xe vdi-destroy uuid=$vdi_uuid
done
xe vm-uninstall force=true vm=$vmname
