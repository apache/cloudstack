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



 

# $Id: listvmdisksize.sh 9132 2010-06-04 20:17:43Z manuel $ $HeadURL: svn://svn.lab.vmops.com/repos/vmdev/java/scripts/storage/qcow2/listvmdisksize.sh $
# listvmdisksize.sh -- list disk sizes of a VM

usage() {
  printf "Usage: %s: -d <disk-fs> [-t | -a ] \n" $(basename $0) >&2
}


# Evaluate a floating point number expression.
function float_eval()
{
    local stat=0
    local result=0.0
    if [[ $# -gt 0 ]]; then
        result=$(echo "scale=0; $*" | bc  2>/dev/null)
        stat=$?
        if [[ $stat -eq 0  &&  -z "$result" ]]; then stat=1; fi
    fi
    echo $result
    return $stat
}


#set -x

aflag=
tflag=
aflag=
diskfs=

while getopts 'd:ta' OPTION
do
  case $OPTION in
  d)	dflag=1
		diskfs="$OPTARG"
		;;
  t)	tflag=1
		;;
  a)	aflag=1		
		;;
  ?)	usage
		exit 2
		;;
  esac
done

if [ "$dflag" != "1"  -a "$tflag$aflag" != "1" ]
then
 usage
 exit 2
fi


if [ "$tflag" == 1 ] 
then
  # Find the virtual size of the disk image
  size_in_bytes=$(qemu-img info /$diskfs | grep "virtual size" | awk '{print $4}')
  
  # Strip off the leading '('
  size_in_bytes=${size_in_bytes:1}
else
  # Find the actual size of the disk image
  size_in_bytes=$(ls -l /$diskfs  | awk '{print $5}')
fi

printf "$size_in_bytes\n"
exit 0
