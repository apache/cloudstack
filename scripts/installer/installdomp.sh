#!/bin/bash
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


# $Id: installdomp.sh 9132 2010-06-04 20:17:43Z manuel $ $HeadURL: svn://svn.lab.vmops.com/repos/vmdev/java/scripts/installer/installdomp.sh $

#set -x
usage() {
 echo "Usage: $(basename $0) <templates base location>"
 echo "eg: $(basename $0) tank/volumes/demo/template"
}

fflag=

while getopts 'f' OPTION
do
  case $OPTION in
  f)	fflag=1
        ;;
  ?)	usage
		exit 2
		;;
  esac
done

shift $(($OPTIND - 1))

if [ $# -lt 1 ]
then
 usage
 exit 2
fi

tmpltfs=$1/private/u000000/os/consoleproxy
if [ "$fflag" == "1" ] 
then
  zfs destroy -r $tmpltfs 2> /dev/null
fi

snaps=$(zfs list -t snapshot -r $tmpltfs)
if [ $? -eq 0 -a "$snaps" != "no datasets available" ]
then
  echo "Warning: some snapshots already exist at target location $tmpltfs"
  echo "Use -f to delete these first"
  exit 2
fi

$(dirname $0)/createtmplt.sh  -t $tmpltfs -n consoleproxy -f /root/template/vmi-root-fc8-x86_64-domP.img.bz2 -s 3 -d consoleproxy -u

rm -f /$tmpltfs/consoleproxy.tar

exit 0
