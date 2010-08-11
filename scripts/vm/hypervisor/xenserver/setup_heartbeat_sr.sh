#!/bin/bash

#set -x
 
usage() {
  printf "Usage: %s [uuid of this host] [uuid of the sr to place the heartbeat]\n" $(basename $0) >&2

}


if [ -z $1 ]; then
  usage
  exit 2
fi

if [ -z $2 ]; then
  usage
  exit 3
fi

if [ `xe host-list | grep $1 | wc -l` -ne 1 ]; then
  printf "Error: Unable to find the host uuid: $1\n" >&2
  usage
  exit 4
fi

if [ `xe sr-list uuid=$2 | wc -l`  -eq 0 ]; then 
  printf "Error: Unable to find SR with uuid: $2\n" >&2
  usage
  exit 5
fi

if [ `xe pbd-list sr-uuid=$2 | grep -B 1 $1 | wc -l` -eq 0 ]; then
  printf "Error: Unable to find a pbd for the SR: $2\n" >&2
  usage
  exit 6
fi

srtype=`xe sr-param-get param-name=type uuid=$2`

if [ "$srtype" == "nfs" ];then
  filename=/var/run/sr-mount/$2/hb-$1
  files=`ls /var/run/sr-mount/$2 | grep "hb-$1"`
  if [ -z "$files" ]; then
    date=`date +%s`
    echo "$date" > $filename
  fi

else 

  link=/dev/VG_XenStorage-$2/hb-$1
  lv=`lvscan | grep $link`
  if [ -z "$lv" ]; then
    lvcreate VG_XenStorage-$2 -n hb-$1 --size 1M
    if [ $? -ne 0 ]; then
      printf "Error: Unable to create heartbeat SR\n" >&2
      exit 7
    fi
    lv=`lvscan | grep $link`
    if [ -z "$lv" ]; then
      printf "Error: Unable to create heartbeat SR\n" >&2
      exit 8
    fi
  fi

  if [ `echo $lv | awk '{print $1}'` == "inactive" ]; then
    lvchange -ay $link
    if [ $? -ne 0 ]; then
      printf "Error: Unable to make the heartbeat SR active\n" >&2
      exit 8
    fi
  fi

  if [ ! -L $link ]; then
    printf "Error: Unable to find the soft link $link\n" >&2
    exit 9
  fi

  dd if=/dev/zero of=$link bs=1 count=100
fi

echo "======> DONE <======"
exit 0
