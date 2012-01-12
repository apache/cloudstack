#!/usr/bin/env bash
# Copyright (C) 2011 Citrix Systems, Inc.  All rights reserved
#     
# This software is licensed under the GNU General Public License v3 or later.
# 
# It is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation, either version 3 of the License, or any later version.
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
# 
# You should have received a copy of the GNU General Public License
# along with this program.  If not, see <http://www.gnu.org/licenses/>.
# 



 

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

  qemu-img convert -O qcow2 /$fspath  $destpath

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
