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

 

# $Id: createtmplt.sh 9132 2010-06-04 20:17:43Z manuel $ $HeadURL: svn://svn.lab.vmops.com/repos/vmdev/java/scripts/storage/secondary/createtmplt.sh $
# createtmplt.sh -- install a template

usage() {
  printf "Usage: %s: -t <template-fs> -n <templatename> -f <root disk file> -d <descr> -h  [-u] [-v]\n" $(basename $0) >&2
}


#set -x
ulimit -c 0

rollback_if_needed() {
  if [ $2 -gt 0 ]
  then
    printf "$3\n"
    #back out all changes
    rm -rf $1
    exit 2
fi
}

untar() {
  local ft=$(file $1| awk -F" " '{print $2}')
  case $ft in
  USTAR) 
     printf "tar archives not supported\n"  >&2
     return 1
          ;;
  *) printf "$1"
     return 0
	  ;;
  esac

}

is_compressed() {
  local ft=$(file $1| awk -F" " '{print $2}')
  local tmpfile=${1}.tmp

  case $ft in
  gzip)  ctype="gzip"
         ;;
  bzip2)  ctype="bz2"
         ;;
  [zZ][iI][pP])  ctype="zip"
        ;;
    *) echo "File $1 does not appear to be compressed" >&2
        return 1
	;;
  esac
  echo "Uncompressing to $tmpfile (type $ctype)...could take a long time" >&2
  return 0
}

uncompress() {
  local ft=$(file $1| awk -F" " '{print $2}')
  local tmpfile=${1}.tmp

  case $ft in
  gzip)  gunzip -q -c $1 > $tmpfile
         ;;
  bzip2)  bunzip2 -q -c $1 > $tmpfile
         ;;
  [zZ][iI][pP])  unzip -q -p $1 | cat > $tmpfile
        ;;
    *) printf "$1"
       return 0
	;;
  esac

  if [ $? -gt 0 ] 
  then
    printf "Failed to uncompress file (filetype=$ft), exiting "
    return 1 
  fi
 
  rm -f $1
  printf $tmpfile

  return 0
}

create_from_file() {
  local tmpltfs=$1
  local tmpltimg=$2
  local tmpltname=$3

  [ -n "$verbose" ] && echo "Moving to $tmpltfs/$tmpltname...could take a while" >&2
  mv $tmpltimg /$tmpltfs/$tmpltname

}

tflag=
nflag=
fflag=
sflag=
hflag=
hvm=false
cleanup=false
dflag=

while getopts 'vuht:n:f:s:d:S:' OPTION
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
		;;
  d)	dflag=1
		descr="$OPTARG"
		;;
  S)	Sflag=1
		size=$OPTARG
                let "size>>=10"
		ulimit -f $size
		;;
  h)	hflag=1
		hvm="true"
		;;
  u)	cleanup="true"
		;;
  v)	verbose="true"
		;;
  ?)	usage
		exit 2
		;;
  esac
done

isCifs() {
   #TO:DO incase of multiple zone where cifs and nfs exists, 
   #then check if the template file is from cifs using df -P filename
   #Currently only cifs is supported in hyperv zone.
   mount | grep "type cifs" > /dev/null
   echo $?
}

if [ "$tflag$nflag$fflag$sflag" != "1111" ]
then
 usage
 exit 2
fi

mkdir -p $tmpltfs

if [ ! -f $tmpltimg ] 
then
  printf "root disk file $tmpltimg doesn't exist\n"
  exit 3
fi

[ -n "$verbose" ] && is_compressed $tmpltimg
tmpltimg2=$(uncompress $tmpltimg)
rollback_if_needed $tmpltfs $? "failed to uncompress $tmpltimg\n"

tmpltimg2=$(untar $tmpltimg2)
rollback_if_needed $tmpltfs $? "tar archives not supported\n"

if [ ${tmpltname%.vhd} != ${tmpltname} ]
then
  if [ $(isCifs) -ne 0 ] ;
  then
      if  which  vhd-util &>/dev/null
      then
        vhd-util read -p -n ${tmpltimg2} > /dev/null
        rollback_if_needed $tmpltfs $? "vhd check of $tmpltimg2 failed\n"
        vhd-util set -n ${tmpltimg2} -f "hidden" -v "0" > /dev/null
        rollback_if_needed $tmpltfs $? "vhd remove $tmpltimg2 hidden failed\n"
     fi
  fi
fi

imgsize=$(ls -l $tmpltimg2| awk -F" " '{print $5}')

create_from_file $tmpltfs $tmpltimg2 $tmpltname

touch /$tmpltfs/template.properties
rollback_if_needed $tmpltfs $? "Failed to create template.properties file"
echo -n "" > /$tmpltfs/template.properties

today=$(date '+%m_%d_%Y')
echo "filename=$tmpltname" > /$tmpltfs/template.properties
echo "description=$descr" >> /$tmpltfs/template.properties
# we need to rethink this property as it might get changed after download due to decompression
# option is to recalcutate it here
echo "hvm=$hvm" >> /$tmpltfs/template.properties
echo "size=$imgsize" >> /$tmpltfs/template.properties

if [ "$cleanup" == "true" ]
then
  rm -f $tmpltimg
fi

exit 0
