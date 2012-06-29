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


# $Id: installcentos.sh 9132 2010-06-04 20:17:43Z manuel $ $HeadURL: svn://svn.lab.vmops.com/repos/vmdev/java/scripts/installer/installcentos.sh $

# set -x
usage() {
 echo "Usage: $(basename $0) -t <templates base location> -c <createtmplt.sh path> "
 echo "eg: $(basename $0) -t tank/volumes/demo/template -c /root/createmplt.sh "
}

fflag=
tflag=
cflag=
while getopts 'f:t:c:' OPTION
do
  case $OPTION in
  f)	fflag=1
        ;;
  t)	tflag=1
  		template_location="$OPTARG"
  		;;
  c)	cflag=1
  		create_tmplt_path="$OPTARG"
  		;;    
  ?)	usage
		exit 2
		;;
  esac
done

shift $(($OPTIND - 1))

if [ "$tflag" != "1" ] || [ "$cflag" != "1" ] 
then
 usage
 exit 2
fi

tmpltfs=$template_location/public/os/centos53-x86_64
if [ "$fflag" == "1" ] 
then
  zfs destroy -Rr $tmpltfs 2> /dev/null
fi

snaps=$(zfs list -t snapshot -r $tmpltfs 2> /dev/null)
if [ $? -eq 0 -a "$snaps" != "" ]
then
  echo "Warning: some snapshots already exist at target location $tmpltfs"
  echo "Use -f to delete these first"
  exit 2
fi

$create_tmplt_path -t $tmpltfs -n centos53-x86_64 -f /root/template/vmi-root-centos.5-3.x86-64.img.bz2 -s 2 -d "centos5.3-x86_64" -u

rm -f /$tmpltfs/*.tar

exit 0
