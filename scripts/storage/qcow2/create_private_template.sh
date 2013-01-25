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

 

# $Id: create_private_template.sh 9804 2010-06-22 18:36:49Z alex $ $HeadURL: svn://svn.lab.vmops.com/repos/vmdev/java/scripts/storage/qcow2/create_private_template.sh $
# create_private_template.sh -- create a private template from a snapshot
# @VERSION@

usage() {
  printf "Usage: %s: -p <snapshot path> -n <template name> -s <snapshot name> -d <template path>\n" $(basename $0) >&2
  exit 2
}

create_template() {
  local fspath=$1
  local destpath=$2

  # if backing image exists, we need to combine them, otherwise 
  # copy the image to preserve snapshots/compression
  if $qemu_img info "$tmpltimg" | grep -q backing; then
    qemu-img convert -O qcow2 /$fspath  $destpath
  else
    cp -f /$fspath $destpath
  fi

  if [ $? -gt 0  ]; then
    printf " Failed to export template  $destpath\n" >&2
    rm -rf $destpath
    return 3
  fi

  return 0
}

#set -x

pflag=
nflag=
dflag=
uflag=
iflag=
sflag=
pathval=
templatename=
snapshot=
install_dir=
user_folder=
instance_folder=

while getopts 'p:n:s:d:' OPTION
do
  case $OPTION in
  p)    pflag=1
        pathval="$OPTARG"
        ;;
  n)	nflag=1
        templatename="$OPTARG"
        ;;
  s)    sflag=1
        snapshot="$OPTARG"
        ;;
  d)    dflag=1
        templatePath="$OPTARG"
        ;;
  ?)	usage
        ;;
  esac
done

if [ "$pflag$nflag$sflag$dflag" != "1111" ]
then
  usage
fi

if [ ! -d ${templatePath} ]
then
    mkdir -p ${templatePath}
    if [ $? -gt 0 ]
    then
        printf "Failed to create template path: $templatePath \n"
        exit 5
    fi
fi

create_template $pathval $templatePath/$templatename

if [ $? -gt 0 ]
then
    printf "create priate template failed\n"
    exit 4
fi

checksum=`md5sum $templatePath/$templatename |awk '{print $1}'`

touch $templatePath/template.properties
echo -n "" > $templatePath/template.properties
today=$(date '+%m_%d_%Y')
echo "filename=$templatename" > /$templatePath/template.properties
echo "snapshot.name=$today" >> /$templatePath/template.properties
echo "description=$templatename" >> /$templatePath/template.properties
echo "name=$templatename" >> /$templatePath/template.properties
echo "hvm=1" >> /$templatePath/template.properties
echo "checksum=$checksum" >> /$templatePath/template.properties
echo "virtualsize=1000000" >> /$templatePath/template.properties

exit $?
