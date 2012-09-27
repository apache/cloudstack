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
