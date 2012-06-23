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

 

# $Id: managevolume.sh 9132 2010-06-04 20:17:43Z manuel $ $HeadURL: svn://svn.lab.vmops.com/repos/vmdev/java/scripts/storage/qcow2/managevolume.sh $
# modifyvolume.sh -- add or delete a disk volume 

usage() {
  echo "Usage: modifydisk.sh -f <folder of volume> -p <path of volume> -s <size of volume> [-d <delete>] \n"
}

check_params() {
  if [ "$fflag$pflag$sflag" != "111" ]
  then
    return 1
  else
    return 0
  fi
}

cleanup_and_exit_if_error() {
  local return_code=$1
  local msg=$2
  local path=$3
  
  if [ $return_code -gt 0 ]
  then
    delete_disk $path
    exit_if_error $return_code "$msg"
  fi
}

exit_if_error() {
  local return_code=$1
  local msg=$2
  
  if [ $return_code -gt 0 ]
  then
    printf "${msg}\n"
    exit 1
  fi
}

make_folder() {
  local folder=$1
  
  if [ ! -d ${folder} ]
  then
    mkdir -p ${folder}
  fi
}

check_disk() {
  local folder=$1
  local path=$2
  
  make_folder $folder
  
  if [ -f $path ]
  then
    return 1
  else
    return 0
  fi
}

delete_disk() {
  local path=$1
  
  if [ -f $path ]
  then
    rm $path
  fi
  
  return 0
}

create_disk() {
  local path=$1
  local size=$2
  
  size=$(convert_size_to_gb $size)
  
  if [ $? -gt 0 ]
  then
    return 1
  fi

  qemu-img create -f qcow2 $path $size
  
  return $?
}

convert_size_to_gb() {
  local size=$1
  
  suffix=${size:(-1)}
  case $suffix in
    G)   
         ;;
    [0-9])   size=${size}G
         ;;
    *)   printf "Error in disk size: expect G as a suffix or no suffix\n"
         return 1
         ;;
  esac
  
  echo $size
  return 0
}

# set -x

fflag=0
pflag=0
sflag=0
dflag=0

while getopts 'f:p:s:d:' OPTION
do
  case $OPTION in
  f)	fflag=1
  		folder="$OPTARG"
  		;;
  p)	pflag=1
		path="$OPTARG"
		;;
  s)	sflag=1
  		size="$OPTARG"
  		;;
  d)	dflag=1
  		;;
  ?)	usage
		exit 2
		;;
  esac
done

# Check all parameters
check_params
exit_if_error $? "$(usage)"

if [ "$dflag" == "0" ]
then
  # Add the volume
  
  # Create the folder if necessary, and make sure there is no existing disk there
  check_disk $folder $path
  cleanup_and_exit_if_error $? "Failed to create disk at $path; path already exists." $path
  
  # Create the disk
  create_disk $path $size
  cleanup_and_exit_if_error $? "Failed to create disk at $path of size $datadisksize." $path
  
else
  # Delete the volume
  
  delete_disk $path
  
fi

exit 0


