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
  printf "Usage: %s [uuid of this host] [uuid of the sr to place the heartbeat] [ add , true/false]\n" $(basename $0)

}


if [ -z $1 ]; then
  usage
  echo "#1# no uuid of host"
  exit 0
fi

if [ -z $2 ]; then
  usage
  echo "#2# no uuid of sr"
  exit 0
fi

if [ -z $3 ]; then
  usage
  echo "#21# no add parameter"
  exit 0
fi


if [ `xe host-list | grep $1 | wc -l` -ne 1 ]; then
  echo  "#3# Unable to find the host uuid: $1"
  exit 0
fi

if [ `xe sr-list uuid=$2 | wc -l`  -eq 0 ]; then 
  echo "#4# Unable to find SR with uuid: $2"
  exit 0
fi

if [ `xe pbd-list sr-uuid=$2 | grep -B 1 $1 | wc -l` -eq 0 ]; then
  echo "#5# Unable to find a pbd for the SR: $2"
  exit 0
fi

hbfile=/opt/xensource/bin/heartbeat

if [ "$3" = "true" ]; then

  srtype=`xe sr-param-get param-name=type uuid=$2`

 
  if [ "$srtype" = "nfs" ];then
    dir=/var/run/sr-mount/$2
    filename=$dir/hb-$1
    if [ ! -f "$filename" ]; then
      echo "#6# heartbeat file $filename doesn't exist"
      exit 0
    fi
  else 
    dir=/dev/VG_XenStorage-$2
    link=$dir/hb-$1
    lvchange -ay $link
    if [ $? -ne 0 ]; then
       echo "#7# Unable to make the heartbeat $link active"
       exit 0
    fi
  fi


  if [ -f $hbfile ]; then
    grep $dir $hbfile >/dev/null
    if [ $? -gt 0 ]
    then
      echo $dir >> $hbfile
    fi
  else
    echo $dir >> $hbfile
  fi

else 
  if [ -f $hbfile ]; then
    sed -i /$2/d $hbfile 
  fi
fi

echo "#0#DONE"

exit 0
