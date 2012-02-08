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



 

# $Id: createtmplt.sh 11601 2010-08-11 17:26:15Z kris $ $HeadURL: svn://svn.lab.vmops.com/repos/branches/2.1.refactor/java/scripts/storage/qcow2/createtmplt.sh $
# createtmplt.sh -- install a template

usage() {
  printf "Usage: %s: -t <template-fs> -n <templatename> -f <root disk file> -s <size in Gigabytes> -c <md5 cksum> -d <descr> -h  [-u]\n" $(basename $0) >&2
}


#set -x
qemu_img="cloud-qemu-img"
which $qemu_img
if [ $? -gt 0 ]
then
   which qemu-img
   if [ $? -eq 0 ]
   then
       qemu_img="qemu-img"
   fi
fi


verify_cksum() {
  echo  "$1  $2" | md5sum  -c --status
  #printf "$1\t$2" | md5sum  -c --status
  if [ $? -gt 0 ] 
  then
    printf "Checksum failed, not proceeding with install\n"
    exit 3
  fi
}

untar() {
  local ft=$(file $1| awk -F" " '{print $2}')
  local basedir=$(dirname $1)
  case $ft in
  USTAR)  local rootimg=$(tar tf $1 | grep $3)
          (cd $2; tar xf $1)
          rm -f $1
          printf "$2/$rootimg"
          ;;
      *)  printf "$1"
          return 0
	  ;;
  esac

}

uncompress() {
  local ft=$(file $1| awk -F" " '{print $2}')
  local imgfile=${1%.*} #strip out trailing file suffix
  local tmpfile=${imgfile}.tmp

  case $ft in
  gzip)  gunzip -c $1 > $tmpfile
         ;;
  bzip2)  bunzip2 -c $1 > $tmpfile
         ;;
  ZIP)  unzip -p $1 | cat > $tmpfile
        ;;
  *)	printf "$1"
        return 0
	;;
  esac

  if [ $? -gt 0 ] 
  then
    printf "Failed to uncompress file, exiting "
    exit 1 
  fi
 
  mv $tmpfile $imgfile
  printf "$imgfile"

  return 0
}

create_from_file() {
  local tmpltfs=$1
  local tmpltimg="$2"
  local tmpltname=$3
  if [ -b $tmpltimg ]; then
      $qemu-img convert -f raw -O qcow2 "$tmpltimg" /$tmpltfs/$tmpltname
  else
      $qemu_img convert -f qcow2 -O qcow2 "$tmpltimg" /$tmpltfs/$tmpltname >& /dev/null
  fi
  
  if [ "$cleanup" == "true" ]
  then
    rm -f "$tmpltimg"
  fi
  chmod a+r /$tmpltfs/$tmpltname
}

create_from_snapshot() {
  local tmpltImg="$1"
  local snapshotName="$2"
  local tmpltfs=$3
  local tmpltname=$4

  $qemu_img convert -f qcow2 -O qcow2 -s "$snapshotName" "$tmpltImg" /$tmpltfs/$tmpltname >& /dev/null
  if [ $? -gt 0 ]
  then
     printf "Failed to create template /$tmplfs/$tmpltname from snapshot $snapshotName on disk $tmpltImg "
     exit 2
  fi

  chmod a+r /$tmpltfs/$tmpltname
}

tflag=
nflag=
fflag=
sflag=
hflag=
hvm=false
cleanup=false
dflag=
cflag=
snapshotName=

while getopts 'uht:n:f:sc:d:' OPTION
do
  case $OPTION in
  t)	tflag=1
		tmpltfs="$OPTARG"
		;;
  n)	nflag=1
		tmpltname="$OPTARG"
		;;
  f)	fflag=1
		tmpltimg="$OPTARG"
		;;
  s)	sflag=1
		sflag=1
		;;
  c)	cflag=1
		snapshotName="$OPTARG"
		;;
  d)	dflag=1
		descr="$OPTARG"
		;;
  u)	cleanup="true"
		;;
  ?)	usage
		exit 2
		;;
  esac
done


if [ ! -d /$tmpltfs ] 
then
  mkdir -p /$tmpltfs
  if [ $? -gt 0 ] 
  then
    printf "Failed to create user fs $tmpltfs\n" >&2
    exit 1
  fi
fi

if [ ! -f $tmpltimg -a ! -b $tmpltimg ] 
then
  printf "root disk file $tmpltimg doesn't exist\n"
  exit 3
fi

tmpltimg=$(uncompress "$tmpltimg")
if [ $? -ne 0 ]
then
  printf "failed to uncompress $tmpltimg\n"
fi

if [ "$sflag" == "1" ]
then
   create_from_snapshot  "$tmpltimg" "$snapshotName" $tmpltfs $tmpltname
else
   create_from_file $tmpltfs "$tmpltimg" $tmpltname
fi

touch /$tmpltfs/template.properties
chmod a+r /$tmpltfs/template.properties
echo -n "" > /$tmpltfs/template.properties

today=$(date '+%m_%d_%Y')
echo "filename=$tmpltname" > /$tmpltfs/template.properties
echo "snapshot.name=$today" >> /$tmpltfs/template.properties
echo "description=$descr" >> /$tmpltfs/template.properties

if [ "$cleanup" == "true" ]
then
  rm -f "$tmpltimg"
fi

exit 0
