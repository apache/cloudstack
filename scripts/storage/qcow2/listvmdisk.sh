#!/usr/bin/env bash
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
# 
#   http://www.apache.org/licenses/LICENSE-2.0
# 
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.


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
