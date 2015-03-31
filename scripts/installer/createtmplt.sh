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


# $Id: createtmplt.sh 9132 2010-06-04 20:17:43Z manuel $ $HeadURL: svn://svn.lab.vmops.com/repos/vmdev/java/scripts/installer/createtmplt.sh $
# createtmplt.sh -- install a template

usage() {
  printf "Usage: %s: -t <template-fs> -n <templatename> -f <root disk file> -s <size in Gigabytes> -c <md5 cksum> -d <descr> -h  [-u]\n" $(basename $0) >&2
}


#set -x

rollback_if_needed() {
  if [ $2 -gt 0 ]
  then
    printf "$3\n"
    #back out all changes
    zfs destroy -r $1
    exit 2
fi
}

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
  local tarfile=$(basename $1)
  case $ft in
  USTAR)  local rootimg=$(tar tf $1 | grep $3)
          (cp $1 $2;  cd $2; tar xf $tarfile)
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
  [zZ][iI][pP])  unzip -p $1 | cat > $tmpfile
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
  local tmpltimg=$2
  local tgtfile=$3
  local volsize=$4
  local cleanup=$5

  #copy 64k of zeros for LUN metatdata
  dd if=/dev/zero of=/$tgtfile bs=64k count=1

  #copy the file to the disk
  dd if=$tmpltimg of=/$tgtfile bs=64k seek=1

  rollback_if_needed $tmpltfs $? "Failed to copy root disk"

  if [ "$cleanup" == "true" ]
  then
    rm -f $tmpltimg
  fi
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

while getopts 'uht:n:f:s:c:d:' OPTION
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
		volsize="$OPTARG"
		;;
  c)	cflag=1
		cksum="$OPTARG"
		;;
  d)	dflag=1
		descr="$OPTARG"
		;;
  h)	hflag=1
		hvm="true"
		;;
  u)	cleanup="true"
		;;
  ?)	usage
		exit 2
		;;
  esac
done

if [ "$tflag$nflag$fflag$sflag" != "1111" ]
then
 usage
 exit 2
fi

if [ -n "$cksum" ]
then
  verify_cksum $cksum $tmpltimg
fi

if [ ${tmpltfs:0:1} == / ]
then
  tmpltfs=${tmpltfs:1}
fi

if [ ! -d /$tmpltfs ] 
then
  zfs create -p $tmpltfs
  if [ $? -gt 0 ] 
  then
    printf "Failed to create user fs $tmpltfs\n" >&2
    exit 1
  fi
fi

if [[ $(zfs get -H -o value -p type $tmpltfs) != filesystem  ]]
then
  printf "template fs doesn't exist\n" >&2
  exit 2
fi

tmpltimg2=$(uncompress $tmpltimg)
tmpltimg2=$(untar $tmpltimg2 /$tmpltfs vmi-root)

if [ ! -f $tmpltimg2 ] 
then
  rollback_if_needed $tmpltfs 2 "root disk file $tmpltimg doesn't exist\n"
  exit 3
fi

# need the 'G' suffix on volume size
if [ ${volsize:(-1)} != G ]
then
  volsize=${volsize}G
fi

#determine source file size -- it needs to be less than or equal to volsize
imgsize=$(ls -lh $tmpltimg2| awk -F" " '{print $5}')
if [ ${imgsize:(-1)} == G ] 
then
  imgsize=${imgsize%G} #strip out the G 
  imgsize=${imgsize%.*} #...and any decimal part
  let imgsize=imgsize+1 # add 1 to compensate for decimal part
  volsizetmp=${volsize%G}
  if [ $volsizetmp -lt $imgsize ]
  then
    volsize=${imgsize}G  
  fi
fi

tgtfile=${tmpltfs}/vmi-root-${tmpltname}

create_from_file $tmpltfs $tmpltimg2 $tgtfile  $volsize $cleanup

tmpltswap=$(ls -lh /$tmpltfs | grep swap)
if [ $? -eq 0 ] 
then
  swapsize=$(echo $tmpltswap | awk '{print $5}')
  tmpltswap=$(echo $tmpltswap | awk '{print $NF}')
  tmpltswap=/${tmpltfs}/${tmpltswap} 
  tgtfile=${tmpltfs}/vmi-swap-${tmpltname}
  create_from_file $tmpltfs $tmpltswap $tgtfile $swapsize $cleanup
fi


if [ "$hvm" != "true" ]
then
  vmlinuz=$(ls /$tmpltfs/vmlinuz*)
  if [ "$vmlinuz" == "" ]
  then
    touch /$tmpltfs/pygrub
  fi
fi

rollback_if_needed $tmpltfs $? "Failed to create pygrub file"

touch /$tmpltfs/template.properties
rollback_if_needed $tmpltfs $? "Failed to create template.properties file"
echo -n "" > /$tmpltfs/template.properties

today=$(date '+%m_%d_%Y')
echo "snapshot.name=$today" > /$tmpltfs/template.properties
echo "description=$descr" >> /$tmpltfs/template.properties
echo "name=$tmpltname" >> /$tmpltfs/template.properties
echo "checksum=$cksum" >> /$tmpltfs/template.properties
echo "hvm=$hvm" >> /$tmpltfs/template.properties
echo "volume.size=$volsize" >> /$tmpltfs/template.properties

zfs snapshot -r $tmpltfs@vmops_ss
rollback_if_needed $tmpltfs $? "Failed to snapshot filesystem"

#if [ "$cleanup" == "true" ]
#then
  #rm -f $tmpltimg
#fi

exit 0
