#!/bin/bash
# Copyright (C) 2011 Citrix Systems, Inc.  All rights reserved
#     
# This software is licensed under the GNU General Public License v3 or later.
# 
# It is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation, either version 3 of the License, or any later version.
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
# 
# You should have received a copy of the GNU General Public License
# along with this program.  If not, see <http://www.gnu.org/licenses/>.
# 
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

