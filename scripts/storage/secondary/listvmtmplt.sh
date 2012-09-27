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


# $Id: listvmtmplt.sh 9132 2010-06-04 20:17:43Z manuel $ $HeadURL: svn://svn.lab.vmops.com/repos/vmdev/java/scripts/storage/secondary/listvmtmplt.sh $
# listtmplt.sh -- list templates under a directory

usage() {
  printf "Usage: %s: -r <root dir>  \n" $(basename $0) >&2
}


#set -x

rflag=
rootdir=

while getopts 'r:' OPTION
do
  case $OPTION in
  r)	rflag=1
		rootdir="$OPTARG"
		;;
  ?)	usage
		exit 2
		;;
  esac
done

if [ "$rflag" != "1" ]
then
 usage
 exit 2
fi


for i in $(find /$rootdir -name template.properties );
do  
  d=$(dirname $i)
  filename=$(grep "^filename" $i | awk -F"=" '{print $NF}')
#  size=$(grep "virtualsize" $i | awk -F"=" '{print $NF}')
#  if [ -n "$filename" ] && [ -n "$size" ]
#  then
#    d=$d/$filename/$size
#  fi
  echo ${d#/}/$filename #remove leading slash 
done

exit 0
