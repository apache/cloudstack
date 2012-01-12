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



 

# $Id: delvm.sh 9132 2010-06-04 20:17:43Z manuel $ $HeadURL: svn://svn.lab.vmops.com/repos/vmdev/java/scripts/storage/qcow2/delvm.sh $
# delvm.sh -- delete a cloned image used for a vm

usage() {
  printf "Usage: %s: -i <path-to-instance> -u <path-to-user>\n" $(basename $0) >&2
}


#set -x

iflag=
uflag=
userfs=
instancefs=

while getopts 'i:u:' OPTION
do
  case $OPTION in
  i)	iflag=1
		instancefs="$OPTARG"
		;;
  u)	uflag=1
		userfs="$OPTARG"
		;;
  ?)	usage
		exit 2
		;;
  esac
done

if [ "$iflag$uflag" != "1" -a "$iflag$uflag" != "11" ]
then
 usage
 exit 2
fi


if [ "$iflag" == 1 ] 
then
  rm -rf $instancefs
  if [ $? -gt 0 ] 
  then
    printf "Failed to destroy instance fs\n" >&2
    exit 5
  fi
fi

if [ "$uflag" == 1 ] 
then
  rm -rf $userfs  
  if [ $? -gt 0 ] 
  then
    printf "Failed to destroy user fs\n" >&2
    exit 5
  fi
fi

exit 0
