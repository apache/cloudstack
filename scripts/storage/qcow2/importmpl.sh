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


# $Id: importmpl.sh 9132 2010-06-04 20:17:43Z manuel $ $HeadURL: svn://svn.lab.vmops.com/repos/vmdev/java/scripts/storage/qcow2/importmtpl.sh $
#set -x
usage() {
  printf "Usage: %s: -m <secondary storage mount point> -r <routing template file> -v <centos template file> [-F]\n" $(basename $0) >&2
  printf "or\n" >&2
  printf "%s: -m <secondary storage mount point> -u -r <http url for routing template> -v <http url for centos template file> [-F]\n" $(basename $0) >&2
}

downloadImage() {
  local tmptfile=$1
  local url=$2
  touch $tmptfile
  if [ $? -ne 0 ]
  then
  	printf "Failed to create temporary file in directory $(dirname $0) -- is it read-only or full?\n"
  fi
  wget -O $tmptfile $url
  if [ $? -ne 0 ]
  then
    	echo "Failed to fetch routing template from $url"
    	exit 5
  fi
}

setTmplMetaFile() {
  local metaFile=$1
  local uniqName=$2
  local id=$3
  local tmplfile=$4
  local fileName=$5
  virtualsize=$(qemu-img info $tmplfile|grep virtual |awk '{print $4}'|cut -d \( -f 2)
  disksize=$(ls -l $tmplfile|awk '{print $5}')
  
  echo "qcow2=true" >> $metaFile
  echo "id=$id" >> $metaFile
  echo "public=true" >> $metaFile
  echo "qcow2.filename=$fileName" >> $metaFile
  echo "uniquename=$uniqName" >> $metaFile
  echo "qcow2.virtualsize=$virtualsize" >> $metaFile
  echo "virtualsize=$virtualsize" >> $metaFile
  echo "qcow2.size=$disksize" >> $metaFile
}

mflag=
rflag=
vflag=
uflag=
Fflag=
while getopts 'm:v:r:Fu' OPTION
do
  case $OPTION in
  m)	mflag=1
		mntpoint="$OPTARG"
		;;
  r)	rflag=1
		rttmplt="$OPTARG"
		;;
  v)	vflag=1
		vmtmplt="$OPTARG"
		;;
  u)	uflag=1 ;;
  F)	Fflag=1 ;;
  ?)	usage
		exit 2
		;;
  esac
done

if [[ "$uflag" != "1" && "$mflag$rflag$vflag" != "111" ]]
then
  usage
  exit 2
fi

if [[ "$uflag" != "1" && "$rflag" == "1" && ! -f $rttmplt ]] 
then
  echo "template image file $rttmplt doesn't exist"
  exit 3
fi

if [[ "$uflag" != "1" && "$vflag" == "1" && ! -f $vmtmplt ]] 
then
  echo "template image file $vmtmplt doesn't exist"
  exit 3
fi

if [[ "$uflag" == "1" && "$rflag" != "1" ]] 
then
  rttmplt=http://download.cloud.com/templates/builtin/a88232bf-6a18-38e7-aeee-c1702725079f.qcow2.bz2
  echo "download routing template from $rttmplt"
fi

if [[ "$uflag" == "1" && "$vflag" != "1" ]] 
then
  vmtmplt=http://download.cloud.com/templates/builtin/eec2209b-9875-3c8d-92be-c001bd8a0faf.qcow2.bz2
  echo "download cnetos template from $vmtmplt"
fi

if [ ! -d $mntpoint ] 
then
  echo "mount point $mntpoint doesn't exist\n"
  exit 4
fi

localfilert=$(uuidgen).qcow2
localfilevm=$(uuidgen).qcow2
destdirrt=$mntpoint/template/tmpl/1/1/
destdirvm=$mntpoint/template/tmpl/1/2/

mkdir -p $destdirrt
if [ $? -ne 0 ]
then
  printf "Failed to write to mount point $mntpoint -- is it mounted?\n"
fi
mkdir -p $destdirvm
if [ $? -ne 0 ]
then
  printf "Failed to write to mount point $mntpoint -- is it mounted?\n"
fi

if [ "$Fflag" == "1" ]
then
  rm -rf $destdirrt/*
  if [ $? -ne 0 ]
  then
    echo "Failed to clean up template directory $destdir -- check permissions?"
    exit 2
  fi
  rm -rf $destdirvm/*
  if [ $? -ne 0 ]
  then
    echo "Failed to clean up template directory $destdir -- check permissions?"
    exit 2
  fi
fi

if [ -f $destdirrt/template.properties ]
then
  echo "Data already exists at destination $destdir -- use -f to force cleanup of old template"
  exit 4
fi

if [ -f $destdirvm/template.properties ]
then
  echo "Data already exists at destination $destdir -- use -f to force cleanup of old template"
  exit 4
fi

destimgfiles=$(find $destdirrt -name \*.qcow2)
if [ "$destimgfiles" != "" ]
then
  echo "Data already exists at destination $destdirrt -- use -F to force cleanup of old template"
  exit 5
fi

destimgfiles=$(find $destdirvm -name \*.qcow2)
if [ "$destimgfiles" != "" ]
then
  echo "Data already exists at destination $destdirvm -- use -F to force cleanup of old template"
  exit 5
fi

tmpfilert=$(dirname $0)/$localfilert
tmpfilevm=$(dirname $0)/$localfilevm
if [ "$uflag" == "1" ]
then 
downloadImage $tmpfilert $rttmplt
downloadImage $tmpfilevm $vmtmplt
fi

if [[ "$uflag" != "1" && "$rflag$vflag" == "11" ]]
then
  cp $rttmplt $tmpfilert
  if [ $? -ne 0 ]
  then
    printf "Failed to create temporary file in directory $(dirname $0) -- is it read-only or full?\n"
    exit 6
  fi
  cp $vmtmplt $tmpfilevm 
  if [ $? -ne 0 ]
  then
    printf "Failed to create temporary file in directory $(dirname $0) -- is it read-only or full?\n"
    exit 6
  fi
fi


$(dirname $0)/createtmplt.sh -s 2 -d 'DomR Template' -n $localfilert -t $destdirrt/ -f $tmpfilert -u  &> /dev/null

if [ $? -ne 0 ]
then
  echo "Failed to install routing template $rttmplt to $destdirrt"
fi

$(dirname $0)/createtmplt.sh -s 2 -d 'CentOS 5.5(x86_64) no GUI' -n $localfilevm -t $destdirvm/ -f $tmpfilevm -u  &> /dev/null

if [ $? -ne 0 ]
then
  echo "Failed to install vm template $vmtmplt to $destdirvm"
fi

setTmplMetaFile $destdirrt/template.properties "routing" "1" $destdirrt/$localfilert $localfilert
setTmplMetaFile $destdirvm/template.properties "centos55-x86_64" "2" $destdirvm/$localfilevm $localfilevm
echo "Successfully installed routing template $rttmplt to $destdirrt and $vmtmplt to $destdirvm"
