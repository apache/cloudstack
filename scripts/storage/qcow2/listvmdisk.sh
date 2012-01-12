#!/usr/bin/env bash
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



 

# $Id: listvmdisk.sh 9132 2010-06-04 20:17:43Z manuel $ $HeadURL: svn://svn.lab.vmops.com/repos/vmdev/java/scripts/storage/qcow2/listvmdisk.sh $
# listvmdisk.sh -- list disks of a VM

usage() {
  printf "Usage: %s: -i <instance-fs> [-r | -d <num> ] \n" $(basename $0) >&2
}


#set -x

iflag=
rflag=
dflag=
disknum=
instancefs=

while getopts 'i:d:r' OPTION
do
  case $OPTION in
  i)	iflag=1
		instancefs="$OPTARG"
		;;
  d)	dflag=1
		disknum="$OPTARG"
		;;
  r)	rflag=1
		;;
  ?)	usage
		exit 2
		;;
  esac
done

if [ "$iflag" != "1"  -a "$rflag$dflag" != "1" ]
then
 usage
 exit 2
fi



if [ "$rflag" == 1 ] 
then
  find $instancefs -name rootdisk
  if [ $? -gt 0 ] 
  then
    exit 5
  fi
  exit 0
fi

if [ "$dflag" == 1 ] 
then
  if [[ $disknum -eq 0 ]]
  then 
    find $instancefs -name datadisk
  else 
    find $instancefs -name datadisk${disknum}
  fi
  if [ $? -gt 0 ] 
  then
    exit 6
  fi
  exit 0
fi

exit 0
