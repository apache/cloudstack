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



# $Id: createvol.sh 9132 2010-06-04 20:17:43Z manuel $ $HeadURL: svn://svn.lab.vmops.com/repos/vmdev/java/scripts/installer/createvolume.sh $
# createvolume.sh -- install a volume

usage() {
  printf "Usage: %s: -t <volume-fs> -n <volumename> -f <root disk file> -s <size in Gigabytes> -c <md5 cksum> -d <descr> -h  [-u]\n" $(basename $0) >&2
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
  local volimg=$2
  local tgtfile=$3
  local volsize=$4
  local cleanup=$5

  #copy 64k of zeros for LUN metatdata
  dd if=/dev/zero of=/$tgtfile bs=64k count=1

  #copy the file to the disk
  dd if=$volimg of=/$tgtfile bs=64k seek=1

  rollback_if_needed $volfs $? "Failed to copy root disk"

  if [ "$cleanup" == "true" ]
  then
    rm -f $volimg
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
		volfs="$OPTARG"
		;;
  n)	nflag=1
		volname="$OPTARG"
		;;
  f)	fflag=1
		volimg="$OPTARG"
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
  verify_cksum $cksum $volimg
fi

if [ ${volfs:0:1} == / ]
then
  volfs=${volfs:1}
fi

if [ ! -d /$volfs ] 
then
  zfs create -p $volfs
  if [ $? -gt 0 ] 
  then
    printf "Failed to create user fs $volfs\n" >&2
    exit 1
  fi
fi

if [[ $(zfs get -H -o value -p type $volfs) != filesystem  ]]
then
  printf "volume fs doesn't exist\n" >&2
  exit 2
fi

volimg2=$(uncompress $volimg)
volimg2=$(untar $volimg2 /$volfs vmi-root)

if [ ! -f $volimg2 ] 
then
  rollback_if_needed $volfs 2 "root disk file $volimg doesn't exist\n"
  exit 3
fi

# need the 'G' suffix on volume size
if [ ${volsize:(-1)} != G ]
then
  volsize=${volsize}G
fi

#determine source file size -- it needs to be less than or equal to volsize
imgsize=$(ls -lh $volimg2| awk -F" " '{print $5}')
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

tgtfile=${volfs}/vmi-root-${volname}

create_from_file $volfs $volimg2 $tgtfile  $volsize $cleanup

volswap=$(ls -lh /$volfs | grep swap)
if [ $? -eq 0 ] 
then
  swapsize=$(echo $volswap | awk '{print $5}')
  volswap=$(echo $volswap | awk '{print $NF}')
  volswap=/${volfs}/${volswap} 
  tgtfile=${volfs}/vmi-swap-${volname}
  create_from_file $volfs $volswap $tgtfile $swapsize $cleanup
fi


if [ "$hvm" != "true" ]
then
  vmlinuz=$(ls /$volfs/vmlinuz*)
  if [ "$vmlinuz" == "" ]
  then
    touch /$volfs/pygrub
  fi
fi

rollback_if_needed $volfs $? "Failed to create pygrub file"

touch /$volfs/volume.properties
rollback_if_needed $volfs $? "Failed to create volume.properties file"
echo -n "" > /$volfs/volume.properties

today=$(date '+%m_%d_%Y')
echo "snapshot.name=$today" > /$volfs/volume.properties
echo "description=$descr" >> /$volfs/volume.properties
echo "name=$volname" >> /$volfs/volume.properties
echo "checksum=$cksum" >> /$volfs/volume.properties
echo "hvm=$hvm" >> /$volfs/volume.properties
echo "volume.size=$volsize" >> /$volfs/volume.properties

zfs snapshot -r $volfs@vmops_ss
rollback_if_needed $volfs $? "Failed to snapshot filesystem"

#if [ "$cleanup" == "true" ]
#then
  #rm -f $volimg
#fi

exit 0
