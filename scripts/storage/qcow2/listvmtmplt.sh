#!/usr/bin/env bash
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
