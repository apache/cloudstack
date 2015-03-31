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
  digestalgo=""
  case ${#1} in
        32) digestalgo="md5sum" ;;
        40) digestalgo="sha1sum" ;;
        56) digestalgo="sha224sum" ;;
        64) digestalgo="sha256sum" ;;
        96) digestalgo="sha384sum" ;;
        128) digestalgo="sha512sum" ;;
        *) echo "Please provide valid cheksum" ; exit 3 ;;
  esac
  echo  "$1  $2" | $digestalgo  -c --status
  #printf "$1\t$2" | $digestalgo  -c --status
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
      $qemu_img convert -f raw -O qcow2 "$volimg" /$volfs/$volname
  else
    # if backing image exists, we need to combine them, otherwise
    # copy the image to preserve snapshots/compression
    if $qemu_img info "$volimg" | grep -q backing; then
      $qemu_img convert -f qcow2 -O qcow2 "$volimg" /$volfs/$volname >& /dev/null
    else
      cp -f $volimg /$volfs/$volname
    fi
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
