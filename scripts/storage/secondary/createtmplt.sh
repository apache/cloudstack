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


usage() {
  printf "\nUsage: %s:\n\t-t templatefilesystem\n\t-n templatename\n\t-f templatefile\n\n" $(basename $0) >&2
}

#set -x
ulimit -c 0

# Usage: e.g. failed $? "this is an error"
failed() {
  local returnval=$1
  local returnmsg=$2
  
  # check for an message, if there is no one dont print anything
  if [[ -z $returnmsg ]]; then
    :
  else
    echo -e $returnmsg
  fi
  if [[ $returnval -eq 0 ]]; then
    return 0
  else
    echo "Installation failed"
    exit $returnval
  fi
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

  if [[ $? -gt 0 ]]; then
    printf "Failed to uncompress file (filetype=$ft), exiting"
    rm -f $tmpfile
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

  echo "Moving to $tmpltfs/$tmpltname...could take a while" >&2
  mv $tmpltimg /$tmpltfs/$tmpltname
}

tflag=
nflag=
fflag=

# check if first parameter is not a dash (-) then print the usage block
if [[ ! $@ =~ ^\-.+ ]]; then
	usage
	exit 0
fi

OPTERR=0
while getopts 't:n:f:' OPTION
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

findtmpltype() {
  local ft=$(file $1| awk -F" " '{print $2}')

  case $ft in
  Microsoft)
    if [[ $(isCifs) -ne 0 ]]; then
      if [[ $(which vhd-util &>/dev/null) ]]; then
        vhd-util read -p -n ${tmpltimg2} > /dev/null
        failed $? "vhd check of $tmpltimg2 failed\n"
        vhd-util set -n ${tmpltimg2} -f "hidden" -v "0" > /dev/null
        failed $? "vhd remove $tmpltimg2 hidden failed\n"
      else
        failed 2 "Please install vhd-util command"
      fi
    fi
    ;;
  POSIX)
    tar xvf $tmpltimg2 -C $tmpltfs &> /dev/null
    failed $? "untar $tmpltimg2 to $tmpltfs"
    ;;
  x86)
    :
    ;;
  QEMU)
    :
    ;;
  esac
}

if [[ "$tflag$nflag$fflag" != "111" ]]; then
  usage
  exit 2
fi

mkdir -p $tmpltfs

if [[ ! -f $tmpltimg ]]; then
  printf "template file $tmpltimg doesn't exist\n"
  exit 2
fi

is_compressed $tmpltimg
tmpltimg2=$(uncompress $tmpltimg)

findtmpltype $tmpltimg2

create_from_file $tmpltfs $tmpltimg2 $tmpltname

exit 0