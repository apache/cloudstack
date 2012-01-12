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
 

help() {
  printf "Usage: $0 
                    -i nfs server ip 
                    -p nfs server path
                    -m mount point 
                    -h host  
                    -r write/read hb log 
                    -c cleanup
                    -t interval between read hb log\n"
  exit 1
}
#set -x
NfsSvrIP=
NfsSvrPath=
MountPoint=
HostIP=
interval=
rflag=0
cflag=0

while getopts 'i:p:m:h:t:rc' OPTION
do
  case $OPTION in
  i)
     NfsSvrIP="$OPTARG"
     ;;
  p)
     NfsSvrPath="$OPTARG"
     ;;
  m)
     MountPoint="$OPTARG"
     ;;
  h)
     HostIP="$OPTARG"
     ;;
  r)
     rflag=1 
     ;;
  t)
     interval="$OPTARG"
     ;;
  c)
    cflag=1
     ;;
  *)
     help
     ;;
  esac
done

if [ -z "$NfsSvrIP" ]
then
   exit 1
fi


#delete VMs on this mountpoint
deleteVMs() {
  local mountPoint=$1
  vmPids=$(ps aux| grep qemu | grep "$mountPoint" | awk '{print $2}' 2> /dev/null) 
  if [ $? -gt 0 ]
  then
     return
  fi

  if [ -z "$vmPids" ]
  then
     return
  fi

  for pid in $vmPids
  do
     kill -9 $pid &> /dev/null
  done
}

#checking is there the same nfs server mounted under $MountPoint?
mounts=$(cat /proc/mounts |grep nfs|grep $MountPoint)
if [ $? -gt 0 ]
then
   # remount it
   mount $NfsSvrIP:$NfsSvrPath $MountPoint -o sync,soft,proto=tcp,acregmin=0,acregmax=0,acdirmin=0,acdirmax=0,noac,timeo=133,retrans=10 &> /dev/null
   if [ $? -gt 0 ]
   then
      printf "Failed to remount $NfsSvrIP:$NfsSvrPath under $MountPoint" 
      exit 1
   fi
   if [ "$rflag" == "0" ]
   then
     deleteVMs $MountPoint
   fi
fi

hbFolder=$MountPoint/KVMHA/
hbFile=$hbFolder/hb-$HostIP

write_hbLog() {
#write the heart beat log
  stat $hbFile &> /dev/null
  if [ $? -gt 0 ]
  then
     # create a new one
     mkdir -p $hbFolder &> /dev/null
     touch $hbFile &> /dev/null
     if [ $? -gt 0 ]
     then
 	printf "Failed to create $hbFile"
        return 2
     fi
  fi

  timestamp=$(date +%s)
  echo $timestamp > $hbFile
  return $?
}

check_hbLog() {
  oldTimeStamp=$(cat $hbFile) 
  sleep $interval &> /dev/null
  newTimeStamp=$(cat $hbFile) 
  if [ $newTimeStamp -gt $oldTimeStamp ]
  then
    return 0
  fi
  return 1
}

if [ "$rflag" == "1" ]
then
  check_hbLog
  if [ $? == 0 ]
  then
    echo "=====> ALIVE <====="
  else
    echo "=====> DEAD <======"
  fi
  exit 0
elif [ "$cflag" == "1" ]
then
  reboot
  exit $?
else
  write_hbLog 
  exit $?
fi
