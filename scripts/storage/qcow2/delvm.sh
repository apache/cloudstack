#!/usr/bin/env bash
# $Id: delvm.sh 9132 2010-06-04 20:17:43Z manuel $ $HeadURL: svn://svn.lab.vmops.com/repos/branches/2.1.x.beta/java/scripts/storage/qcow2/delvm.sh $
# delvm.sh -- delete a cloned image used for a vm
#

usage() {
  printf "Usage: %s: -i <path-to-instance> -u <path-to-user>\n" $(basename $0) >&2
}


#set -x

iflag=
uflag=
userfs=
instancefs=

while getopts 'i:u:' OPTION
do
  case $OPTION in
  i)	iflag=1
		instancefs="$OPTARG"
		;;
  u)	uflag=1
		userfs="$OPTARG"
		;;
  ?)	usage
		exit 2
		;;
  esac
done

if [ "$iflag$uflag" != "1" -a "$iflag$uflag" != "11" ]
then
 usage
 exit 2
fi


if [ "$iflag" == 1 ] 
then
  rm -rf $instancefs
  if [ $? -gt 0 ] 
  then
    printf "Failed to destroy instance fs\n" >&2
    exit 5
  fi
fi

if [ "$uflag" == 1 ] 
then
  rm -rf $userfs  
  if [ $? -gt 0 ] 
  then
    printf "Failed to destroy user fs\n" >&2
    exit 5
  fi
fi

exit 0
