#!/bin/bash

#set -x
 
usage() {
  printf "Usage: %s [mountpoint in secondary storage] [uuid of the source sr]\n" $(basename $0) 
}

cleanup()
{
  if [ ! -z $localmp ]; then 
    umount $localmp
    if [ $? -eq 0 ];  then
      rm $localmp -rf
    fi
  fi
}

if [ -z $1 ]; then
  usage
  echo "2#no mountpoint"
  exit 0
else
  mountpoint=$1
fi

if [ -z $2 ]; then
  usage
  echo "3#no uuid of the source sr"
  exit 0
else
  sruuid=$2
fi

type=$(xe sr-param-get uuid=$sruuid param-name=type)
if [ $? -ne 0 ]; then
  echo "4#sr $sruuid doesn't exist"
  exit 0
fi

localmp=/var/run/cloud_mount/$(uuidgen -r)

mkdir -p $localmp
if [ $? -ne 0 ]; then
  echo "5#cann't make dir $localmp"
  exit 0
fi

mount $mountpoint $localmp
if [ $? -ne 0 ]; then
  echo "6#cann't mounbt $mountpoint to $localmp"
  exit 0
fi

vhdfile=$(ls $localmp/*.vhd)
if [ $? -ne 0 ]; then
  echo "7#There is no vhd file under $mountpoint"
  cleanup
  exit 0
fi

if [ $type == "nfs" ]; then
  uuid=$(uuidgen -r)
  dd if=$vhdfile of=/var/run/sr-mount/$sruuid/$uuid bs=2M
  if [ $? -ne 0 ]; then
    echo "8#failed ot copy vhdfile to /var/run/sr-mount/sruuid/$uuid"
    cleanup
    exit 0
  fi
  mv /var/run/sr-mount/$sruuid/$uuid /var/run/sr-mount/$sruuid/${uuid}.vhd
  xe sr-scan uuid=$sruuid
elif [ $type == "lvmoiscsi" -o $type == "lvm" ]; then
  size=$(vhd-util query -v -n $vhdfile)
  uuid=$(xe vdi-create sr-uuid=$sruuid virtual-size=${size}MiB type=user name-label="cloud")
  if [ $? -ne 0 ]; then
    echo "9#can not create vdi in sr $sruuid"
    cleanup
    exit 0
  fi
  lvsize=$(xe vdi-param-get uuid=$uuid param-name=physical-utilisation)
  if [ $? -ne 0 ]; then
    echo "12#failed to get physical size of vdi $uuid"
    cleanup
    exit 0
  fi
  lvchange -ay /dev/VG_XenStorage-$sruuid/VHD-$uuid
  if [ $? -ne 0 ]; then
    echo "10#lvm can not make VDI $uuid  visiable"
    cleanup
    exit 0
  fi
  dd if=$vhdfile of=/dev/VG_XenStorage-$sruuid/VHD-$uuid bs=2M
  if [ $? -ne 0 ]; then
    echo "11#failed to dd to sr $sruuid"
    cleanup
    exit 0
  fi
  vhd-util modify -s $lvsize -n /dev/VG_XenStorage-$sruuid/VHD-$uuid
  if [ $? -ne 0 ]; then
    echo "13#failed to set new vhd physical size for vdi vdi $uuid"
    cleanup
    exit 0
  fi
  xe sr-scan uuid=$sruuid
  if [ $? -ne 0 ]; then
    echo "14#failed to scan sr $sruuid"
    cleanup
    exit 0
  fi
else 
  echo "15#doesn't support sr type $type"
  cleanup
  exit 0
fi

echo "0#$uuid"
cleanup
exit 0
