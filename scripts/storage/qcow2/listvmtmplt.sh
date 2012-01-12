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



 

# $Id: listvmtmplt.sh 9132 2010-06-04 20:17:43Z manuel $ $HeadURL: svn://svn.lab.vmops.com/repos/vmdev/java/scripts/storage/qcow2/listvmtmplt.sh $
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
  filename=$(grep "filename" $i | awk -F"=" '{print $NF}')
  size=$(grep "virtualsize" $i | awk -F"=" '{print $NF}')
  if [ -n "$filename" ] && [ -n "$size" ]
  then
    d=$d/$filename/$size
  fi
  echo ${d#/} #remove leading slash 
done

exit 0
