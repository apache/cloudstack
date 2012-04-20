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



 

# $Id: createvol.sh 11601 2010-08-11 17:26:15Z kris $ $HeadURL: svn://svn.lab.vmops.com/repos/branches/2.1.refactor/java/scripts/storage/qcow2/createvol.sh $
# createvol.sh -- install a volume

usage() {
  printf "Usage: %s: -t <volume-fs> -n <volumename> -f <root disk file> -s <size in Gigabytes> -c <md5 cksum> -d <descr> -h  [-u]\n" $(basename $0) >&2
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
  local volfs=$1
  local volimg="$2"
  local volname=$3
  if [ -b $volimg ]; then
      $qemu-img convert -f raw -O qcow2 "$volimg" /$volfs/$volname
  else
      $qemu_img convert -f qcow2 -O qcow2 "$volimg" /$volfs/$volname >& /dev/null
  fi
  
  if [ "$cleanup" == "true" ]
  then
    rm -f "$volimg"
  fi
  chmod a+r /$volfs/$volname
}

create_from_snapshot() {
  local volImg="$1"
  local snapshotName="$2"
  local volfs=$3
  local volname=$4

  $qemu_img convert -f qcow2 -O qcow2 -s "$snapshotName" "$volImg" /$volfs/$volname >& /dev/null
  if [ $? -gt 0 ]
  then
     printf "Failed to create volume /$tmplfs/$volname from snapshot $snapshotName on disk $volImg "
     exit 2
  fi

  chmod a+r /$volfs/$volname
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
		volfs="$OPTARG"
		;;
  n)	nflag=1
		volname="$OPTARG"
		;;
  f)	fflag=1
		volimg="$OPTARG"
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


if [ ! -d /$volfs ] 
then
  mkdir -p /$volfs
  if [ $? -gt 0 ] 
  then
    printf "Failed to create user fs $volfs\n" >&2
    exit 1
  fi
fi

if [ ! -f $volimg -a ! -b $volimg ] 
then
  printf "root disk file $volimg doesn't exist\n"
  exit 3
fi

volimg=$(uncompress "$volimg")
if [ $? -ne 0 ]
then
  printf "failed to uncompress $volimg\n"
fi

if [ "$sflag" == "1" ]
then
   create_from_snapshot  "$volimg" "$snapshotName" $volfs $volname
else
   create_from_file $volfs "$volimg" $volname
fi

touch /$volfs/volume.properties
chmod a+r /$volfs/volume.properties
echo -n "" > /$volfs/volume.properties

today=$(date '+%m_%d_%Y')
echo "filename=$volname" > /$volfs/volume.properties
echo "snapshot.name=$today" >> /$volfs/volume.properties
echo "description=$descr" >> /$volfs/volume.properties

if [ "$cleanup" == "true" ]
then
  rm -f "$volimg"
fi

exit 0
