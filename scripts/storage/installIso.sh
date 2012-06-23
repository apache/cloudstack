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


# $Id: installIso.sh 9132 2010-06-04 20:17:43Z manuel $ $HeadURL: svn://svn.lab.vmops.com/repos/vmdev/java/scripts/storage/installIso.sh $
# installIso.sh -- install an iso

usage() {
  printf "Usage: %s: -t <iso-fs> -f <iso file> -c <md5 cksum> [-u]\n" $(basename $0) >&2
}


#set -x

verify_cksum() {
  echo  "$1  $2" | md5sum  -c --status
  if [ $? -gt 0 ] 
  then
    printf "Checksum failed, not proceeding with install\n"
    exit 3
  fi
}

install_file() {
  local isofs=$1
  local isofile=$2
  local cleanup=$3
  local tmpltname=$4

  mv $isofile /$isofs/$tmpltname

  if [ $? -gt 0 ] 
  then
    printf "Move operation failed, iso $isofile not installed\n"
    exit 4
  fi

  #create symbolic link for iso file
  file=$tmpltname
  isofs=$isofs/$file
  mp=${isofs%/iso/*}
  mp=/$mp/iso
  path=${isofs:${#mp}}  
  pushd $mp
  ln -s $path $file
  popd


}


tflag=
fflag=
cleanup=false
cflag=
tmpltname=

while getopts 'ut:f:n:c:' OPTION
do
  case $OPTION in
  t)	tflag=1
		isofs="$OPTARG"
		;;
  f)	fflag=1
		isofile="$OPTARG"
		;;
  c)	cflag=1
		cksum="$OPTARG"
		;;
  u)	cleanup="true"
		;;
  n)    tmpltname="$OPTARG"
        ;;
  ?)	usage
		exit 2
		;;
  esac
done

if [ "$tflag$fflag" != "11" ]
then
 usage
 exit 2
fi

if [ -n "$cksum" ]
then
  verify_cksum $cksum $isofile
fi

if [ ${isofs:0:1} == / ]
then
  isofs=${isofs:1}
fi

if [ ! -d /$isofs ] 
then
  mkdir -p /$isofs
  if [ $? -gt 0 ] 
  then
    printf "Failed to create iso fs $isofs\n" >&2
    exit 1
  fi
fi

install_file $isofs $isofile $cleanup $tmpltname

isofilename=${isofile##*/}
today=$(date '+%m_%d_%Y')

echo "filename=$tmpltname" > /$isofs/template.properties
echo "snapshot.name=$today" >> /$isofs/template.properties


exit 0
