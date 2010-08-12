#!/usr/bin/env bash
# $Id: listvmtmplt.sh 9132 2010-06-04 20:17:43Z manuel $ $HeadURL: svn://svn.lab.vmops.com/repos/branches/2.1.x.beta/java/scripts/storage/zfs/iscsi/listvmtmplt.sh $
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

if [ ${rootdir:0:1} == / ]
then
  rootdir=${rootdir:1}
fi


if [[ $(zfs get -H -o value -p type $rootdir) != filesystem  ]]
then
  printf "Supplied root directory fs doesn't exist\n" >&2
  exit 1
fi

for i in $(find /$rootdir -name template.properties -size +0 );
do  
  d=$(dirname $i)
  echo ${d#/} #remove leading slash 
done

exit 0
