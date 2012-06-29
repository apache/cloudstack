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
