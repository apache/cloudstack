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



 

# $Id: installIso.sh 9132 2010-06-04 20:17:43Z manuel $ $HeadURL: svn://svn.lab.vmops.com/repos/vmdev/java/scripts/storage/secondary/installIso.sh $
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
