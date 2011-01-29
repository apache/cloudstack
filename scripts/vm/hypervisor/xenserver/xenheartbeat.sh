#!/bin/bash
# Version @VERSION@

#set -x
 
usage() {
  printf "Usage: %s [uuid of this host] [interval in seconds]\n" $(basename $0) >&2

}

if [ -z $1 ]; then
  usage
  exit 2
fi

if [ -z $2 ]; then
  usage
  exit 3
fi

file=/opt/xensource/bin/heartbeat
while true 
do 
  sleep $2

  if [ ! -f $file  ]
  then
    continue
  fi

  # for iscsi
  dirs=$(cat $file | grep VG_XenStorage)
  for dir in $dirs
  do
    if [ -d $dir ]; then
      hb=$dir/hb-$1
      date +%s | dd of=$hb count=100 bs=1 2>/dev/null
      if [ $? -ne 0 ]; then
          /usr/bin/logger -t heartbeat "Problem with $hb"
          reboot -f
      fi
    else
      sed -i /${dir##/*/}/d $file
    fi
  done
  # for nfs
  dirs=$(cat $file | grep sr-mount)
  for dir in $dirs
  do
    mp=`mount | grep $dir`
    if [ -n "$mp" ]; then
      hb=$dir/hb-$1
      date +%s | dd of=$hb count=100 bs=1 2>/dev/null
      if [ $? -ne 0 ]; then
          /usr/bin/logger -t heartbeat "Problem with $hb"
          reboot -f
      fi
    else
      sed -i /${dir##/*/}/d $file
    fi
  done

done

